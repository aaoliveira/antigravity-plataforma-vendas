package com.sellerintelligence.modules.company.service;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.BusinessRuleException;
import com.sellerintelligence.core.exception.DuplicateResourceException;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.company.entity.CompanyAccountLinkRecord;
import com.sellerintelligence.modules.company.entity.CompanyRecord;
import com.sellerintelligence.modules.company.model.CompanyAccountLinkDto;
import com.sellerintelligence.modules.company.model.LinkAccountCommand;
import com.sellerintelligence.modules.company.repository.CompanyAccountLinkRepository;
import com.sellerintelligence.modules.company.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço responsável por gerenciar vínculos explícitos entre Empresas e Contas de Marketplaces.
 * Proíbe vínculos ambíguos (uma conta só pode pertencer a 1 empresa ativa) e mantém histórico integral.
 */
@Service
@Transactional
public class CompanyAccountLinkService {

    private static final Logger log = LoggerFactory.getLogger(CompanyAccountLinkService.class);

    private final CompanyRepository companyRepository;
    private final CompanyAccountLinkRepository linkRepository;
    private final SellerAccountRepository sellerAccountRepository;
    private final AuditPublisher auditPublisher;

    public CompanyAccountLinkService(
            CompanyRepository companyRepository,
            CompanyAccountLinkRepository linkRepository,
            SellerAccountRepository sellerAccountRepository,
            AuditPublisher auditPublisher
    ) {
        this.companyRepository = companyRepository;
        this.linkRepository = linkRepository;
        this.sellerAccountRepository = sellerAccountRepository;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Vincula uma conta de marketplace a uma empresa.
     * Proíbe vínculo duplicado e ambíguo. Se já houver vínculo ativo com outra empresa,
     * exige replaceActiveLink = true para transferir o vínculo encerrando a vigência anterior.
     */
    public CompanyAccountLinkDto linkAccount(UUID tenantId, UUID companyId, LinkAccountCommand cmd, String userEmail) {
        CompanyRecord company = companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada no tenant ativo: " + companyId));

        if (!company.isActive()) {
            throw new BusinessRuleException("Não é permitido vincular conta de marketplace a uma empresa inativa.");
        }

        SellerAccount sellerAccount = sellerAccountRepository.findByIdAndTenantId(cmd.sellerAccountId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Conta de marketplace não encontrada no tenant ativo: " + cmd.sellerAccountId()));

        Optional<CompanyAccountLinkRecord> activeLinkOpt = linkRepository.findActiveBySellerAccount(tenantId, cmd.sellerAccountId());

        int nextVersion = 1;
        Instant now = Instant.now();

        if (activeLinkOpt.isPresent()) {
            CompanyAccountLinkRecord currentActive = activeLinkOpt.get();

            if (currentActive.getCompanyId().equals(companyId)) {
                throw new DuplicateResourceException("Esta conta já está vinculada ativamente a esta mesma empresa.");
            }

            if (!cmd.replaceActiveLink()) {
                throw new DuplicateResourceException(
                        String.format("A conta '%s' já possui vínculo ativo com outra empresa. Confirme a substituição (replaceActiveLink=true) para transferir.",
                                sellerAccount.getAccountName())
                );
            }

            // Encerra vigência anterior para preservar histórico
            currentActive.terminate(now);
            linkRepository.save(currentActive);
            nextVersion = currentActive.getVersion() + 1;

            auditPublisher.publish(
                    "ACCOUNT_COMPANY_UNLINKED",
                    null,
                    userEmail,
                    tenantId,
                    "127.0.0.1",
                    "SellerIntelligence-Company",
                    UUID.randomUUID().toString(),
                    String.format("Vínculo anterior da conta %s com empresa %s encerrado por transferência",
                            sellerAccount.getId(), currentActive.getCompanyId())
            );
        }

        CompanyAccountLinkRecord newLink = new CompanyAccountLinkRecord(
                UUID.randomUUID(),
                tenantId,
                companyId,
                cmd.sellerAccountId(),
                now,
                null,
                true,
                nextVersion,
                cmd.notes(),
                userEmail,
                now,
                now
        );

        CompanyAccountLinkRecord saved = linkRepository.save(newLink);

        auditPublisher.publish(
                "ACCOUNT_COMPANY_LINKED",
                null,
                userEmail,
                tenantId,
                "127.0.0.1",
                "SellerIntelligence-Company",
                UUID.randomUUID().toString(),
                String.format("Conta de marketplace %s (%s) vinculada à empresa %s (versão %d)",
                        sellerAccount.getAccountName(), sellerAccount.getMarketplace(), company.getCorporateName(), nextVersion)
        );

        log.info("Vínculo de conta criado: id={} tenant={} company={} account={} version={}",
                saved.getId(), tenantId, companyId, cmd.sellerAccountId(), nextVersion);

        return CompanyAccountLinkDto.fromRecord(
                saved,
                company.getCorporateName(),
                sellerAccount.getMarketplace(),
                sellerAccount.getAccountName()
        );
    }

    /**
     * Encerra um vínculo ativo explicitamente.
     */
    public void unlinkAccount(UUID tenantId, UUID linkId, String userEmail) {
        CompanyAccountLinkRecord link = linkRepository.findByIdAndTenantId(linkId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Vínculo não encontrado no tenant ativo: " + linkId));

        if (!link.isActive()) {
            return; // Já encerrado
        }

        link.terminate(Instant.now());
        linkRepository.save(link);

        auditPublisher.publish(
                "ACCOUNT_COMPANY_UNLINKED",
                null,
                userEmail,
                tenantId,
                "127.0.0.1",
                "SellerIntelligence-Company",
                UUID.randomUUID().toString(),
                String.format("Vínculo %s entre empresa %s e conta %s encerrado",
                        linkId, link.getCompanyId(), link.getSellerAccountId())
        );

        log.info("Vínculo de conta encerrado: id={} tenant={}", linkId, tenantId);
    }

    /**
     * Lista contas de marketplace com vínculo ativo com uma determinada empresa.
     */
    @Transactional(readOnly = true)
    public List<CompanyAccountLinkDto> listActiveAccountsByCompany(UUID tenantId, UUID companyId) {
        CompanyRecord company = companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada no tenant ativo: " + companyId));

        List<CompanyAccountLinkRecord> links = linkRepository.findAllActiveByCompany(tenantId, companyId);
        if (links.isEmpty()) {
            return List.of();
        }

        Map<UUID, SellerAccount> accountsMap = sellerAccountRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(SellerAccount::getId, a -> a, (a1, a2) -> a1));

        return links.stream().map(l -> {
            SellerAccount acc = accountsMap.get(l.getSellerAccountId());
            String mkt = acc != null ? acc.getMarketplace() : "DESCONHECIDO";
            String name = acc != null ? acc.getAccountName() : "Conta " + l.getSellerAccountId();
            return CompanyAccountLinkDto.fromRecord(l, company.getCorporateName(), mkt, name);
        }).toList();
    }

    /**
     * Retorna o histórico cronológico de vínculos de uma conta de marketplace específica.
     */
    @Transactional(readOnly = true)
    public List<CompanyAccountLinkDto> getAccountLinkHistory(UUID tenantId, UUID sellerAccountId) {
        SellerAccount sellerAccount = sellerAccountRepository.findByIdAndTenantId(sellerAccountId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Conta de marketplace não encontrada no tenant ativo: " + sellerAccountId));

        List<CompanyAccountLinkRecord> links = linkRepository.findHistoryBySellerAccount(tenantId, sellerAccountId);
        if (links.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> companyNames = companyRepository.findAllByTenantIdOrderByCorporateNameAsc(tenantId).stream()
                .collect(Collectors.toMap(CompanyRecord::getId, CompanyRecord::getCorporateName, (c1, c2) -> c1));

        return links.stream().map(l -> {
            String compName = companyNames.getOrDefault(l.getCompanyId(), "Empresa " + l.getCompanyId());
            return CompanyAccountLinkDto.fromRecord(l, compName, sellerAccount.getMarketplace(), sellerAccount.getAccountName());
        }).toList();
    }

    /**
     * Retorna o vínculo ativo atual de uma conta de marketplace, se existir.
     */
    @Transactional(readOnly = true)
    public Optional<CompanyAccountLinkDto> getActiveLink(UUID tenantId, UUID sellerAccountId) {
        return linkRepository.findActiveBySellerAccount(tenantId, sellerAccountId).map(l -> {
            String compName = companyRepository.findByIdAndTenantId(l.getCompanyId(), tenantId)
                    .map(CompanyRecord::getCorporateName)
                    .orElse("Empresa");
            SellerAccount acc = sellerAccountRepository.findByIdAndTenantId(sellerAccountId, tenantId)
                    .orElse(null);
            String mkt = acc != null ? acc.getMarketplace() : "DESCONHECIDO";
            String name = acc != null ? acc.getAccountName() : "Conta";
            return CompanyAccountLinkDto.fromRecord(l, compName, mkt, name);
        });
    }
}
