package com.sellerintelligence.modules.company;

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
import com.sellerintelligence.modules.company.service.CompanyAccountLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyAccountLinkServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyAccountLinkRepository linkRepository;

    @Mock
    private SellerAccountRepository sellerAccountRepository;

    @Mock
    private AuditPublisher auditPublisher;

    private CompanyAccountLinkService linkService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId1 = UUID.randomUUID();
    private final UUID companyId2 = UUID.randomUUID();
    private final UUID sellerAccountId = UUID.randomUUID();
    private final String userEmail = "admin@sellerintelligence.com";

    private CompanyRecord company1;
    private CompanyRecord company2;
    private SellerAccount sellerAccount;

    @BeforeEach
    void setUp() {
        linkService = new CompanyAccountLinkService(
                companyRepository,
                linkRepository,
                sellerAccountRepository,
                auditPublisher
        );

        company1 = new CompanyRecord(
                companyId1, tenantId, "Empresa Alpha LTDA", "Alpha", "33000167000101",
                null, null, true, null, Instant.now(), Instant.now()
        );

        company2 = new CompanyRecord(
                companyId2, tenantId, "Empresa Beta S/A", "Beta", "00000000000191",
                null, null, true, null, Instant.now(), Instant.now()
        );

        sellerAccount = new SellerAccount(
                sellerAccountId, tenantId, "MERCADO_LIVRE", "MLB_12345", "Loja Oficial Meli", "ACTIVE"
        );
    }

    @Test
    @DisplayName("1. Deve vincular conta de marketplace a uma empresa com sucesso quando não há vínculo anterior")
    void shouldLinkAccountSuccessfullyWhenNoPriorLink() {
        LinkAccountCommand cmd = new LinkAccountCommand(sellerAccountId, "Vínculo inicial", false);

        when(companyRepository.findByIdAndTenantId(companyId1, tenantId)).thenReturn(Optional.of(company1));
        when(sellerAccountRepository.findByIdAndTenantId(sellerAccountId, tenantId)).thenReturn(Optional.of(sellerAccount));
        when(linkRepository.findActiveBySellerAccount(tenantId, sellerAccountId)).thenReturn(Optional.empty());
        when(linkRepository.save(any(CompanyAccountLinkRecord.class))).thenAnswer(i -> i.getArgument(0));

        CompanyAccountLinkDto result = linkService.linkAccount(tenantId, companyId1, cmd, userEmail);

        assertThat(result).isNotNull();
        assertThat(result.companyId()).isEqualTo(companyId1);
        assertThat(result.sellerAccountId()).isEqualTo(sellerAccountId);
        assertThat(result.version()).isEqualTo(1);
        assertThat(result.isActive()).isTrue();
        assertThat(result.companyName()).isEqualTo("Empresa Alpha LTDA");
        assertThat(result.marketplace()).isEqualTo("MERCADO_LIVRE");

        verify(auditPublisher).publish(
                eq("ACCOUNT_COMPANY_LINKED"),
                any(), eq(userEmail), eq(tenantId), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("2. Deve rejeitar vínculo duplicado com a mesma empresa")
    void shouldRejectDuplicateLinkWithSameCompany() {
        LinkAccountCommand cmd = new LinkAccountCommand(sellerAccountId, "Tentativa duplicada", false);

        CompanyAccountLinkRecord existingActive = new CompanyAccountLinkRecord(
                UUID.randomUUID(), tenantId, companyId1, sellerAccountId,
                Instant.now().minusSeconds(3600), null, true, 1, null, userEmail, Instant.now(), Instant.now()
        );

        when(companyRepository.findByIdAndTenantId(companyId1, tenantId)).thenReturn(Optional.of(company1));
        when(sellerAccountRepository.findByIdAndTenantId(sellerAccountId, tenantId)).thenReturn(Optional.of(sellerAccount));
        when(linkRepository.findActiveBySellerAccount(tenantId, sellerAccountId)).thenReturn(Optional.of(existingActive));

        assertThatThrownBy(() -> linkService.linkAccount(tenantId, companyId1, cmd, userEmail))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Esta conta já está vinculada ativamente a esta mesma empresa");
    }

    @Test
    @DisplayName("3. Deve bloquear vínculo ambíguo com outra empresa quando replaceActiveLink for falso")
    void shouldBlockAmbiguousLinkWhenReplaceIsFalse() {
        LinkAccountCommand cmd = new LinkAccountCommand(sellerAccountId, "Troca sem confirmação", false);

        CompanyAccountLinkRecord existingActive = new CompanyAccountLinkRecord(
                UUID.randomUUID(), tenantId, companyId1, sellerAccountId,
                Instant.now().minusSeconds(3600), null, true, 1, null, userEmail, Instant.now(), Instant.now()
        );

        when(companyRepository.findByIdAndTenantId(companyId2, tenantId)).thenReturn(Optional.of(company2));
        when(sellerAccountRepository.findByIdAndTenantId(sellerAccountId, tenantId)).thenReturn(Optional.of(sellerAccount));
        when(linkRepository.findActiveBySellerAccount(tenantId, sellerAccountId)).thenReturn(Optional.of(existingActive));

        assertThatThrownBy(() -> linkService.linkAccount(tenantId, companyId2, cmd, userEmail))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("já possui vínculo ativo com outra empresa");
    }

    @Test
    @DisplayName("4. Deve transferir vínculo para nova empresa quando replaceActiveLink=true, encerrando o anterior e incrementando versão")
    void shouldTransferLinkAndPreserveHistoryWhenReplaceIsTrue() {
        LinkAccountCommand cmd = new LinkAccountCommand(sellerAccountId, "Transferência para Beta", true);

        CompanyAccountLinkRecord previousLink = new CompanyAccountLinkRecord(
                UUID.randomUUID(), tenantId, companyId1, sellerAccountId,
                Instant.now().minusSeconds(86400), null, true, 1, "Vínculo antigo", userEmail, Instant.now(), Instant.now()
        );

        when(companyRepository.findByIdAndTenantId(companyId2, tenantId)).thenReturn(Optional.of(company2));
        when(sellerAccountRepository.findByIdAndTenantId(sellerAccountId, tenantId)).thenReturn(Optional.of(sellerAccount));
        when(linkRepository.findActiveBySellerAccount(tenantId, sellerAccountId)).thenReturn(Optional.of(previousLink));
        when(linkRepository.save(any(CompanyAccountLinkRecord.class))).thenAnswer(i -> i.getArgument(0));

        CompanyAccountLinkDto newLink = linkService.linkAccount(tenantId, companyId2, cmd, userEmail);

        // Vínculo anterior encerrado
        assertThat(previousLink.isActive()).isFalse();
        assertThat(previousLink.getEffectiveTo()).isNotNull();

        // Novo vínculo ativo na versão 2
        assertThat(newLink.companyId()).isEqualTo(companyId2);
        assertThat(newLink.version()).isEqualTo(2);
        assertThat(newLink.isActive()).isTrue();

        verify(auditPublisher).publish(
                eq("ACCOUNT_COMPANY_UNLINKED"),
                any(), eq(userEmail), eq(tenantId), any(), any(), any(), any()
        );
        verify(auditPublisher).publish(
                eq("ACCOUNT_COMPANY_LINKED"),
                any(), eq(userEmail), eq(tenantId), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("5. Deve rejeitar vínculo de conta a empresa inativa")
    void shouldRejectLinkToInactiveCompany() {
        company1.setActive(false);
        LinkAccountCommand cmd = new LinkAccountCommand(sellerAccountId, null, false);

        when(companyRepository.findByIdAndTenantId(companyId1, tenantId)).thenReturn(Optional.of(company1));

        assertThatThrownBy(() -> linkService.linkAccount(tenantId, companyId1, cmd, userEmail))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Não é permitido vincular conta de marketplace a uma empresa inativa");
    }

    @Test
    @DisplayName("6. Deve encerrar vínculo ativo via unlinkAccount")
    void shouldUnlinkAccount() {
        UUID linkId = UUID.randomUUID();
        CompanyAccountLinkRecord activeLink = new CompanyAccountLinkRecord(
                linkId, tenantId, companyId1, sellerAccountId,
                Instant.now().minusSeconds(3600), null, true, 1, null, userEmail, Instant.now(), Instant.now()
        );

        when(linkRepository.findByIdAndTenantId(linkId, tenantId)).thenReturn(Optional.of(activeLink));

        linkService.unlinkAccount(tenantId, linkId, userEmail);

        assertThat(activeLink.isActive()).isFalse();
        assertThat(activeLink.getEffectiveTo()).isNotNull();
        verify(linkRepository).save(activeLink);
        verify(auditPublisher).publish(
                eq("ACCOUNT_COMPANY_UNLINKED"),
                any(), eq(userEmail), eq(tenantId), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("7. Deve listar histórico completo de vínculos de uma conta")
    void shouldListAccountLinkHistory() {
        CompanyAccountLinkRecord link1 = new CompanyAccountLinkRecord(
                UUID.randomUUID(), tenantId, companyId1, sellerAccountId,
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600), false, 1, null, userEmail, Instant.now(), Instant.now()
        );
        CompanyAccountLinkRecord link2 = new CompanyAccountLinkRecord(
                UUID.randomUUID(), tenantId, companyId2, sellerAccountId,
                Instant.now().minusSeconds(3600), null, true, 2, null, userEmail, Instant.now(), Instant.now()
        );

        when(sellerAccountRepository.findByIdAndTenantId(sellerAccountId, tenantId)).thenReturn(Optional.of(sellerAccount));
        when(linkRepository.findHistoryBySellerAccount(tenantId, sellerAccountId)).thenReturn(List.of(link2, link1));
        when(companyRepository.findAllByTenantIdOrderByCorporateNameAsc(tenantId)).thenReturn(List.of(company1, company2));

        List<CompanyAccountLinkDto> history = linkService.getAccountLinkHistory(tenantId, sellerAccountId);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).version()).isEqualTo(2);
        assertThat(history.get(0).companyName()).isEqualTo("Empresa Beta S/A");
        assertThat(history.get(1).version()).isEqualTo(1);
        assertThat(history.get(1).companyName()).isEqualTo("Empresa Alpha LTDA");
    }
}
