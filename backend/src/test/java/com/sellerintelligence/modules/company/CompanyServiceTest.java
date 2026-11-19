package com.sellerintelligence.modules.company;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.DuplicateResourceException;
import com.sellerintelligence.modules.company.entity.CompanyRecord;
import com.sellerintelligence.modules.company.model.CompanyDto;
import com.sellerintelligence.modules.company.model.CreateCompanyCommand;
import com.sellerintelligence.modules.company.model.UpdateCompanyCommand;
import com.sellerintelligence.modules.company.repository.CompanyRepository;
import com.sellerintelligence.modules.company.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuditPublisher auditPublisher;

    private CompanyService companyService;

    private final UUID tenantId = UUID.randomUUID();
    private final String userEmail = "admin@sellerintelligence.com";

    @BeforeEach
    void setUp() {
        companyService = new CompanyService(companyRepository, auditPublisher);
    }

    @Test
    @DisplayName("1. Deve criar empresa com sucesso quando CNPJ for sintaticamente válido e não duplicado")
    void shouldCreateCompanySuccessfully() {
        CreateCompanyCommand cmd = new CreateCompanyCommand(
                "Empresa Exemplo Comércio LTDA",
                "Exemplo Shop",
                "33.000.167/0001-01",
                "123.456.789.000",
                "SIMPLES_NACIONAL",
                "Empresa matriz de testes"
        );

        when(companyRepository.existsByTenantIdAndCnpj(eq(tenantId), eq("33000167000101")))
                .thenReturn(false);

        when(companyRepository.save(any(CompanyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyDto result = companyService.createCompany(tenantId, cmd, userEmail);

        assertThat(result).isNotNull();
        assertThat(result.corporateName()).isEqualTo("Empresa Exemplo Comércio LTDA");
        assertThat(result.tradeName()).isEqualTo("Exemplo Shop");
        assertThat(result.cnpj()).isEqualTo("33000167000101");
        assertThat(result.formattedCnpj()).isEqualTo("33.000.167/0001-01");
        assertThat(result.isActive()).isTrue();

        ArgumentCaptor<CompanyRecord> captor = ArgumentCaptor.forClass(CompanyRecord.class);
        verify(companyRepository).save(captor.capture());
        CompanyRecord saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getCnpj()).isEqualTo("33000167000101");

        verify(auditPublisher).publish(
                eq("COMPANY_CREATED"),
                any(),
                eq(userEmail),
                eq(tenantId),
                any(), any(), any(),
                any()
        );
    }

    @Test
    @DisplayName("2. Deve rejeitar cadastro com CNPJ inválido sintaticamente")
    void shouldRejectInvalidCnpj() {
        CreateCompanyCommand cmd = new CreateCompanyCommand(
                "Empresa com CNPJ Inválido",
                null,
                "11.111.111/1111-11",
                null,
                null,
                null
        );

        assertThatThrownBy(() -> companyService.createCompany(tenantId, cmd, userEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CNPJ inválido sintaticamente");
    }

    @Test
    @DisplayName("3. Deve rejeitar cadastro de empresa com CNPJ duplicado no mesmo tenant")
    void shouldRejectDuplicateCnpjInSameTenant() {
        CreateCompanyCommand cmd = new CreateCompanyCommand(
                "Empresa Duplicada",
                null,
                "00.000.000/0001-91",
                null,
                null,
                null
        );

        when(companyRepository.existsByTenantIdAndCnpj(eq(tenantId), eq("00000000000191")))
                .thenReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(tenantId, cmd, userEmail))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Já existe uma empresa cadastrada com este CNPJ no tenant");
    }

    @Test
    @DisplayName("4. Deve atualizar dados cadastrais de empresa existente e emitir auditoria")
    void shouldUpdateCompanySuccessfully() {
        UUID companyId = UUID.randomUUID();
        CompanyRecord existing = new CompanyRecord(
                companyId,
                tenantId,
                "Nome Antigo LTDA",
                "Fantasia Antiga",
                "33000167000101",
                null,
                null,
                true,
                null,
                Instant.now().minusSeconds(86400),
                Instant.now().minusSeconds(86400)
        );

        when(companyRepository.findByIdAndTenantId(companyId, tenantId))
                .thenReturn(Optional.of(existing));
        when(companyRepository.save(any(CompanyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCompanyCommand updateCmd = new UpdateCompanyCommand(
                "Nome Novo Atualizado LTDA",
                "Fantasia Nova",
                "987.654.321",
                "LUCRO_PRESUMIDO",
                true,
                "Notas atualizadas"
        );

        CompanyDto updated = companyService.updateCompany(tenantId, companyId, updateCmd, userEmail);

        assertThat(updated.corporateName()).isEqualTo("Nome Novo Atualizado LTDA");
        assertThat(updated.tradeName()).isEqualTo("Fantasia Nova");
        assertThat(updated.taxRegime()).isEqualTo("LUCRO_PRESUMIDO");

        verify(auditPublisher).publish(
                eq("COMPANY_UPDATED"),
                any(),
                eq(userEmail),
                eq(tenantId),
                any(), any(), any(),
                any()
        );
    }

    @Test
    @DisplayName("5. Deve desativar empresa e registrar auditoria")
    void shouldDeactivateCompany() {
        UUID companyId = UUID.randomUUID();
        CompanyRecord existing = new CompanyRecord(
                companyId,
                tenantId,
                "Empresa Ativa LTDA",
                null,
                "33000167000101",
                null,
                null,
                true,
                null,
                Instant.now().minusSeconds(86400),
                Instant.now().minusSeconds(86400)
        );

        when(companyRepository.findByIdAndTenantId(companyId, tenantId))
                .thenReturn(Optional.of(existing));

        companyService.deactivateCompany(tenantId, companyId, userEmail);

        assertThat(existing.isActive()).isFalse();
        verify(companyRepository).save(existing);
        verify(auditPublisher).publish(
                eq("COMPANY_DEACTIVATED"),
                any(),
                eq(userEmail),
                eq(tenantId),
                any(), any(), any(),
                any()
        );
    }
}
