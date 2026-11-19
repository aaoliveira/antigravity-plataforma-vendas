package com.sellerintelligence.modules.company.service;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.DuplicateResourceException;
import com.sellerintelligence.modules.company.entity.CompanyRecord;
import com.sellerintelligence.modules.company.model.CompanyDto;
import com.sellerintelligence.modules.company.model.CreateCompanyCommand;
import com.sellerintelligence.modules.company.model.UpdateCompanyCommand;
import com.sellerintelligence.modules.company.repository.CompanyRepository;
import com.sellerintelligence.modules.company.util.CnpjValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de domínio para gestão de Empresas e Unidades de Negócio (PJ).
 * Aplica validação sintática estrita de CNPJ e isolamento multi-tenant.
 */
@Service
@Transactional
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;
    private final AuditPublisher auditPublisher;

    public CompanyService(CompanyRepository companyRepository, AuditPublisher auditPublisher) {
        this.companyRepository = companyRepository;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Cadastra nova empresa garantindo validação sintática do CNPJ e unicidade por tenant.
     */
    public CompanyDto createCompany(UUID tenantId, CreateCompanyCommand cmd, String userEmail) {
        if (!CnpjValidator.isValid(cmd.cnpj())) {
            throw new IllegalArgumentException("CNPJ inválido sintaticamente: " + cmd.cnpj());
        }

        String cleanCnpj = CnpjValidator.clean(cmd.cnpj());

        if (companyRepository.existsByTenantIdAndCnpj(tenantId, cleanCnpj)) {
            throw new DuplicateResourceException("Já existe uma empresa cadastrada com este CNPJ no tenant.");
        }

        Instant now = Instant.now();
        CompanyRecord record = new CompanyRecord(
                UUID.randomUUID(),
                tenantId,
                cmd.corporateName().trim(),
                cmd.tradeName() != null ? cmd.tradeName().trim() : null,
                cleanCnpj,
                cmd.stateRegistration() != null ? cmd.stateRegistration().trim() : null,
                cmd.taxRegime() != null ? cmd.taxRegime().trim() : null,
                true,
                cmd.notes(),
                now,
                now
        );

        CompanyRecord saved = companyRepository.save(record);

        auditPublisher.publish(
                "COMPANY_CREATED",
                null,
                userEmail,
                tenantId,
                "127.0.0.1",
                "SellerIntelligence-Company",
                UUID.randomUUID().toString(),
                String.format("Empresa criada: id=%s razaoSocial=%s cnpj=%s",
                        saved.getId(), saved.getCorporateName(), saved.getCnpj())
        );

        log.info("Empresa criada com sucesso: id={} tenant={} cnpj={}", saved.getId(), tenantId, saved.getCnpj());
        return CompanyDto.fromRecord(saved);
    }

    /**
     * Atualiza dados cadastrais de uma empresa.
     */
    public CompanyDto updateCompany(UUID tenantId, UUID companyId, UpdateCompanyCommand cmd, String userEmail) {
        CompanyRecord record = companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada no tenant ativo: " + companyId));

        record.setCorporateName(cmd.corporateName().trim());
        record.setTradeName(cmd.tradeName() != null ? cmd.tradeName().trim() : null);
        record.setStateRegistration(cmd.stateRegistration() != null ? cmd.stateRegistration().trim() : null);
        record.setTaxRegime(cmd.taxRegime() != null ? cmd.taxRegime().trim() : null);
        if (cmd.isActive() != null) {
            record.setActive(cmd.isActive());
        }
        if (cmd.notes() != null) {
            record.setNotes(cmd.notes());
        }

        CompanyRecord updated = companyRepository.save(record);

        auditPublisher.publish(
                "COMPANY_UPDATED",
                null,
                userEmail,
                tenantId,
                "127.0.0.1",
                "SellerIntelligence-Company",
                UUID.randomUUID().toString(),
                String.format("Empresa atualizada: id=%s razaoSocial=%s ativa=%s",
                        updated.getId(), updated.getCorporateName(), updated.isActive())
        );

        return CompanyDto.fromRecord(updated);
    }

    /**
     * Desativa logicamente uma empresa.
     */
    public void deactivateCompany(UUID tenantId, UUID companyId, String userEmail) {
        CompanyRecord record = companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada no tenant ativo: " + companyId));

        record.setActive(false);
        companyRepository.save(record);

        auditPublisher.publish(
                "COMPANY_DEACTIVATED",
                null,
                userEmail,
                tenantId,
                "127.0.0.1",
                "SellerIntelligence-Company",
                UUID.randomUUID().toString(),
                String.format("Empresa desativada: id=%s razaoSocial=%s", record.getId(), record.getCorporateName())
        );
    }

    @Transactional(readOnly = true)
    public Optional<CompanyDto> getCompany(UUID tenantId, UUID companyId) {
        return companyRepository.findByIdAndTenantId(companyId, tenantId)
                .map(CompanyDto::fromRecord);
    }

    @Transactional(readOnly = true)
    public List<CompanyDto> listAllCompanies(UUID tenantId) {
        return companyRepository.findAllByTenantIdOrderByCorporateNameAsc(tenantId)
                .stream()
                .map(CompanyDto::fromRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CompanyDto> listCompanies(UUID tenantId, String search, boolean activeOnly, Pageable pageable) {
        String query = (search != null && !search.isBlank()) ? search.trim() : null;
        return companyRepository.findByTenantIdWithFilter(tenantId, query, activeOnly, pageable)
                .map(CompanyDto::fromRecord);
    }
}
