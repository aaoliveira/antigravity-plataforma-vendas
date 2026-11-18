package com.sellerintelligence.modules.marketintelligence;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.BusinessRuleException;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.marketintelligence.model.CreateObservationRequest;
import com.sellerintelligence.modules.marketintelligence.model.ItemCondition;
import com.sellerintelligence.modules.marketintelligence.model.MarketObservationSourceType;
import com.sellerintelligence.modules.marketintelligence.model.ObservationResponse;
import com.sellerintelligence.modules.marketintelligence.service.MarketObservationSanitizer;
import com.sellerintelligence.modules.marketintelligence.service.MarketObservationService;
import com.sellerintelligence.modules.marketintelligence.service.MarketObservationValidator;
import com.sellerintelligence.modules.marketintelligence.service.ObservationDeduplicationService;
import com.sellerintelligence.modules.marketintelligence.service.ProductMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketObservationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private MarketObservationRepository observationRepository;

    @Mock
    private ProductMatchRepository productMatchRepository;

    @Mock
    private ProductMatchingService matchingService;

    @Mock
    private AuditPublisher auditPublisher;

    private MarketObservationValidator validator;
    private MarketObservationSanitizer sanitizer;
    private ObservationDeduplicationService deduplicationService;
    private MarketObservationService service;

    @BeforeEach
    void setUp() {
        validator = new MarketObservationValidator();
        sanitizer = new MarketObservationSanitizer();
        deduplicationService = new ObservationDeduplicationService(observationRepository, sanitizer);
        service = new MarketObservationService(
                observationRepository,
                productMatchRepository,
                validator,
                deduplicationService,
                matchingService,
                auditPublisher
        );
    }

    @Test
    @DisplayName("Deve persistir observação válida com proveniência completa e confidence 1.0000")
    void shouldPersistObservationWithValidProvenance() {
        CreateObservationRequest request = new CreateObservationRequest(
                "MLB-999888",
                "MERCADO_LIVRE",
                "Smartphone Samsung Galaxy A54 128GB 5G",
                new BigDecimal("1499.9000"),
                "BRL",
                ItemCondition.NEW,
                "Loja Oficial Samsung",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-999888-smartphone-samsung",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "PUBLIC_SCRAPER_OFFICIAL",
                BigDecimal.ONE,
                null,
                null,
                null,
                "7891234567890",
                "Samsung",
                null,
                null,
                null,
                null,
                "{\"title\":\"Smartphone Samsung\",\"attributes\":{\"color\":\"black\"}}"
        );

        when(observationRepository.findByTenantIdAndMarketplaceAndObservedListingId(TENANT_ID, "MERCADO_LIVRE", "MLB-999888"))
                .thenReturn(Optional.empty());

        when(observationRepository.save(any(MarketObservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ObservationResponse response = service.createObservation(TENANT_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.observedListingId()).isEqualTo("MLB-999888");
        assertThat(response.marketplace()).isEqualTo("MERCADO_LIVRE");
        assertThat(response.sourceType()).isEqualTo(MarketObservationSourceType.OBSERVED);
        assertThat(response.confidence()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(response.gtin()).isEqualTo("7891234567890");
        assertThat(response.minimizedPayload()).contains("Smartphone Samsung");

        verify(auditPublisher).publish(eq("MARKET_OBSERVATION_INGESTED"), any(), any(), eq(TENANT_ID), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Regra Anti-Alucinação: Deve proibir vendas estimadas sem metodologia documentada")
    void shouldRejectSalesMetricsWithoutMethodology() {
        CreateObservationRequest request = new CreateObservationRequest(
                "MLB-999888",
                "MERCADO_LIVRE",
                "Produto Fantasma",
                new BigDecimal("100.0000"),
                "BRL",
                ItemCondition.NEW,
                "Vendedor X",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-999888",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "UNVERIFIED_ESTIMATE",
                new BigDecimal("0.8000"),
                null, // methodologyDescription ausente
                null, // modelVersion ausente
                null, // timeWindow ausente
                null,
                null,
                new BigDecimal("500.0000"), // salesVolume fornecido sem metodologia!
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.createObservation(TENANT_ID, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Valores de vendas, visitas, CTR ou faturamento são proibidos sem fonte verificável e metodologia documentada.");

        verifyNoInteractions(observationRepository);
    }

    @Test
    @DisplayName("Regra Anti-Alucinação: Deve proibir faturamento sem metodologia e versão do modelo")
    void shouldRejectRevenueWithoutMethodology() {
        CreateObservationRequest request = new CreateObservationRequest(
                "MLB-777666",
                "MERCADO_LIVRE",
                "Item X",
                new BigDecimal("200.0000"),
                "BRL",
                ItemCondition.NEW,
                "Vendedor Y",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-777666",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "ALGO_V1",
                new BigDecimal("0.9000"),
                "Estimativa baseada em avaliações", // timeWindow e modelVersion ausentes
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("50000.0000"), // revenue fornecido
                null
        );

        assertThatThrownBy(() -> service.createObservation(TENANT_ID, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Valores de vendas, visitas, CTR ou faturamento são proibidos sem fonte verificável e metodologia documentada.");
    }

    @Test
    @DisplayName("Regra Anti-Alucinação: Observação ESTIMATED exige metodologia, versão do modelo, janela e confiança")
    void shouldRejectEstimatedObservationWithoutMethodologyOrConfidence() {
        CreateObservationRequest request = new CreateObservationRequest(
                "MLB-555444",
                "MERCADO_LIVRE",
                "Item Estimado",
                new BigDecimal("300.0000"),
                "BRL",
                ItemCondition.NEW,
                "Vendedor Z",
                MarketObservationSourceType.ESTIMATED,
                "https://produto.mercadolivre.com.br/MLB-555444",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "ESTIMATION_REGRESSION",
                null, // confiança ausente!
                null, // metodologia ausente!
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.createObservation(TENANT_ID, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Observações do tipo ESTIMATED exigem metodologia documentada");
    }

    @Test
    @DisplayName("Deve aceitar observação ESTIMATED quando toda a metodologia e confiança forem fornecidas")
    void shouldAcceptEstimatedObservationWithFullMethodology() {
        CreateObservationRequest request = new CreateObservationRequest(
                "MLB-555444",
                "MERCADO_LIVRE",
                "Item Estimado Válido",
                new BigDecimal("300.0000"),
                "BRL",
                ItemCondition.NEW,
                "Vendedor Z",
                MarketObservationSourceType.ESTIMATED,
                "https://produto.mercadolivre.com.br/MLB-555444",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "LINEAR_REGRESSION_REVIEWS_V2",
                new BigDecimal("0.8500"),
                "Metodologia estatística baseada em volume de reviews e velocidade de vendas dos últimos 30 dias",
                "v2.1.0",
                "LAST_30_DAYS",
                null,
                null,
                new BigDecimal("120.0000"),
                1500L,
                new BigDecimal("0.0450"),
                new BigDecimal("36000.0000"),
                null
        );

        when(observationRepository.findByTenantIdAndMarketplaceAndObservedListingId(TENANT_ID, "MERCADO_LIVRE", "MLB-555444"))
                .thenReturn(Optional.empty());
        when(observationRepository.save(any(MarketObservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ObservationResponse response = service.createObservation(TENANT_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.sourceType()).isEqualTo(MarketObservationSourceType.ESTIMATED);
        assertThat(response.confidence()).isEqualByComparingTo(new BigDecimal("0.8500"));
        assertThat(response.methodologyDescription()).contains("Metodologia estatística");
        assertThat(response.modelVersion()).isEqualTo("v2.1.0");
        assertThat(response.timeWindow()).isEqualTo("LAST_30_DAYS");
        assertThat(response.salesVolume()).isEqualByComparingTo(new BigDecimal("120.0000"));
    }

    @Test
    @DisplayName("Segurança SSRF: Deve rejeitar IP de metadados de nuvem e redes privadas como source_url")
    void shouldRejectSsrfTargetUrl() {
        CreateObservationRequest request = new CreateObservationRequest(
                "MLB-SSRF",
                "MERCADO_LIVRE",
                "Tentativa SSRF",
                new BigDecimal("10.0000"),
                "BRL",
                ItemCondition.NEW,
                null,
                MarketObservationSourceType.OBSERVED,
                "http://169.254.169.254/latest/meta-data/",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "SSRF_PROBE",
                BigDecimal.ONE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.createObservation(TENANT_ID, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Endereços de rede interna ou metadados de nuvem são proibidos como source_url.");
    }

    @Test
    @DisplayName("Deduplicação: Reprocessar mesma observação deve atualizar registro sem criar duplicatas")
    void shouldDeduplicateObservationForSameListingWithinValidity() {
        MarketObservation existing = new MarketObservation(
                UUID.randomUUID(),
                TENANT_ID,
                "MLB-DEDUP",
                "MERCADO_LIVRE",
                "Produto Original",
                new BigDecimal("100.0000"),
                "BRL",
                ItemCondition.NEW,
                "Loja ABC",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-DEDUP",
                Instant.now().minus(2, ChronoUnit.DAYS),
                Instant.now().minus(2, ChronoUnit.DAYS),
                Instant.now().plus(5, ChronoUnit.DAYS),
                "SYNC_V1",
                BigDecimal.ONE,
                null,
                null,
                null,
                "7890000000000",
                "Marca ABC",
                null,
                null,
                null,
                null,
                "{\"payload\":\"original\"}"
        );

        when(observationRepository.findByTenantIdAndMarketplaceAndObservedListingId(TENANT_ID, "MERCADO_LIVRE", "MLB-DEDUP"))
                .thenReturn(Optional.of(existing));
        when(observationRepository.save(existing)).thenReturn(existing);

        CreateObservationRequest updateRequest = new CreateObservationRequest(
                "MLB-DEDUP",
                "MERCADO_LIVRE",
                "Produto Atualizado de Preço",
                new BigDecimal("95.0000"),
                "BRL",
                ItemCondition.NEW,
                "Loja ABC",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-DEDUP",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "SYNC_V1",
                BigDecimal.ONE,
                null,
                null,
                null,
                "7890000000000",
                "Marca ABC",
                null,
                null,
                null,
                null,
                "{\"payload\":\"updated\"}"
        );

        ObservationResponse response = service.createObservation(TENANT_ID, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("95.0000"));
        assertThat(response.title()).isEqualTo("Produto Atualizado de Preço");

        verify(observationRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Minimização de Payload: Deve expurgar senhas, tokens e CPF do payload bruto")
    void shouldRedactSensitiveDataFromPayload() {
        CreateObservationRequest request = new CreateObservationRequest(
                "MLB-SANITIZE",
                "MERCADO_LIVRE",
                "Produto com Payload Sujo",
                new BigDecimal("50.0000"),
                "BRL",
                ItemCondition.NEW,
                "Vendedor",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-SANITIZE?access_token=secret123&utm_source=google",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "SYNC",
                BigDecimal.ONE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "{\"token\":\"secret_abc_123\",\"cpf\":\"123.456.789-00\",\"item\":\"cadeira gamer\"}"
        );

        when(observationRepository.findByTenantIdAndMarketplaceAndObservedListingId(TENANT_ID, "MERCADO_LIVRE", "MLB-SANITIZE"))
                .thenReturn(Optional.empty());
        when(observationRepository.save(any(MarketObservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ObservationResponse response = service.createObservation(TENANT_ID, request);

        assertThat(response.sourceUrl()).doesNotContain("access_token=secret123");
        assertThat(response.minimizedPayload()).doesNotContain("secret_abc_123");
        assertThat(response.minimizedPayload()).doesNotContain("123.456.789-00");
        assertThat(response.minimizedPayload()).contains("[REDACTED]");
        assertThat(response.minimizedPayload()).contains("cadeira gamer");
    }
}
