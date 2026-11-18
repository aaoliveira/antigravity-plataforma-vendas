package com.sellerintelligence.modules.marketintelligence;

import com.sellerintelligence.modules.catalog.Product;
import com.sellerintelligence.modules.catalog.ProductRepository;
import com.sellerintelligence.modules.catalog.ProductVariant;
import com.sellerintelligence.modules.catalog.ProductVariantRepository;
import com.sellerintelligence.modules.marketintelligence.model.ItemCondition;
import com.sellerintelligence.modules.marketintelligence.model.MarketObservationSourceType;
import com.sellerintelligence.modules.marketintelligence.model.MatchScoreBreakdown;
import com.sellerintelligence.modules.marketintelligence.model.MatchStatus;
import com.sellerintelligence.modules.marketintelligence.model.ProductMatchCandidateResponse;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductMatchingServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private MarketObservationRepository observationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductMatchRepository productMatchRepository;

    private ProductMatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new ProductMatchingService(
                observationRepository,
                productRepository,
                productVariantRepository,
                productMatchRepository
        );
    }

    @Test
    @DisplayName("Matching com GTIN idêntico e termos similares supera o limiar (0.75) e resulta em MATCHED explicável")
    void shouldMatchProductWhenGtinMatchesAndTitleIsSimilar() {
        UUID obsId = UUID.randomUUID();
        MarketObservation observation = new MarketObservation(
                obsId,
                TENANT_ID,
                "MLB-GALAXY-A54",
                "MERCADO_LIVRE",
                "Smartphone Samsung Galaxy A54 128GB 5G Preto",
                new BigDecimal("1499.0000"),
                "BRL",
                ItemCondition.NEW,
                "Loja Oficial Samsung",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-GALAXY-A54",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "PUBLIC_SCRAPER",
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
                null
        );

        UUID prodId = UUID.randomUUID();
        Product product = new Product(
                prodId,
                TENANT_ID,
                "Samsung Galaxy A54 128GB 5G Octa-Core",
                "SKU-SAM-A54",
                "Samsung",
                "Smartphones",
                "ACTIVE"
        );

        ProductVariant variant = new ProductVariant(
                UUID.randomUUID(),
                TENANT_ID,
                prodId,
                "SKU-SAM-A54-BLK",
                "7891234567890", // GTIN idêntico!
                "{}",
                "ACTIVE"
        );

        when(observationRepository.findByIdAndTenantId(obsId, TENANT_ID)).thenReturn(Optional.of(observation));
        when(productRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(product));
        when(productVariantRepository.findAllByTenantIdAndProductId(TENANT_ID, prodId)).thenReturn(List.of(variant));
        when(productMatchRepository.findByTenantIdAndCanonicalProductIdAndObservationId(TENANT_ID, prodId, obsId))
                .thenReturn(Optional.empty());

        List<ProductMatchCandidateResponse> candidates = matchingService.matchObservation(
                TENANT_ID,
                obsId,
                new BigDecimal("0.7500")
        );

        assertThat(candidates).hasSize(1);
        ProductMatchCandidateResponse candidate = candidates.get(0);
        MatchScoreBreakdown breakdown = candidate.scoreBreakdown();

        assertThat(breakdown.overallScore()).isGreaterThanOrEqualTo(new BigDecimal("0.7500"));
        assertThat(breakdown.status()).isEqualTo(MatchStatus.MATCHED);
        assertThat(breakdown.gtinScore()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(breakdown.explanation()).contains("GTIN idêntico (7891234567890)");
        assertThat(breakdown.explanation()).contains("MATCHED");

        verify(productMatchRepository).save(any(ProductMatch.class));
    }

    @Test
    @DisplayName("Critério de Aceite: Matching NÃO une automaticamente itens abaixo do limiar (fica POTENTIAL_MATCH)")
    void shouldNotAutoMatchWhenScoreIsBelowThreshold() {
        UUID obsId = UUID.randomUUID();
        // Observação é um acessório (capa para galaxy A54)
        MarketObservation observation = new MarketObservation(
                obsId,
                TENANT_ID,
                "MLB-CAPA-A54",
                "MERCADO_LIVRE",
                "Capa Silicone Anti Queda para Galaxy A54",
                new BigDecimal("29.9000"),
                "BRL",
                ItemCondition.NEW,
                "Acessórios Top",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-CAPA-A54",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "PUBLIC_SCRAPER",
                BigDecimal.ONE,
                null,
                null,
                null,
                null, // sem GTIN
                null, // sem marca
                null,
                null,
                null,
                null,
                null
        );

        UUID prodId = UUID.randomUUID();
        // Produto do catálogo é o aparelho Smartphone
        Product product = new Product(
                prodId,
                TENANT_ID,
                "Smartphone Samsung Galaxy A54 128GB 5G",
                "SKU-SAM-A54",
                "Samsung",
                "Smartphones",
                "ACTIVE"
        );

        ProductVariant variant = new ProductVariant(
                UUID.randomUUID(),
                TENANT_ID,
                prodId,
                "SKU-SAM-A54-BLK",
                "7891234567890",
                "{}",
                "ACTIVE"
        );

        when(observationRepository.findByIdAndTenantId(obsId, TENANT_ID)).thenReturn(Optional.of(observation));
        when(productRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(product));
        when(productVariantRepository.findAllByTenantIdAndProductId(TENANT_ID, prodId)).thenReturn(List.of(variant));

        List<ProductMatchCandidateResponse> candidates = matchingService.matchObservation(
                TENANT_ID,
                obsId,
                new BigDecimal("0.7500")
        );

        assertThat(candidates).hasSize(1);
        ProductMatchCandidateResponse candidate = candidates.get(0);
        MatchScoreBreakdown breakdown = candidate.scoreBreakdown();

        // O score deve ser inferior a 0.7500
        assertThat(breakdown.overallScore()).isLessThan(new BigDecimal("0.7500"));
        // Status NUNCA deve ser MATCHED! Deve ser POTENTIAL_MATCH ou NO_MATCH
        assertThat(breakdown.status()).isNotEqualTo(MatchStatus.MATCHED);
        assertThat(breakdown.explanation()).contains("NÃO unido automaticamente");
    }

    @Test
    @DisplayName("Penalidade de GTIN Divergente: Produtos com GTINs diferentes sofrem penalidade drástica")
    void shouldPenalizeConflictingGtin() {
        UUID obsId = UUID.randomUUID();
        MarketObservation observation = new MarketObservation(
                obsId,
                TENANT_ID,
                "MLB-ITEM-1",
                "MERCADO_LIVRE",
                "Mouse Sem Fio Logitech M280",
                new BigDecimal("89.9000"),
                "BRL",
                ItemCondition.NEW,
                "Loja X",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-ITEM-1",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "SYNC",
                BigDecimal.ONE,
                null,
                null,
                null,
                "1111111111111", // GTIN observado
                "Logitech",
                null,
                null,
                null,
                null,
                null
        );

        UUID prodId = UUID.randomUUID();
        Product product = new Product(
                prodId,
                TENANT_ID,
                "Mouse Sem Fio Logitech M280 Preto",
                "SKU-LOGI-M280",
                "Logitech",
                "Periféricos",
                "ACTIVE"
        );

        ProductVariant variant = new ProductVariant(
                UUID.randomUUID(),
                TENANT_ID,
                prodId,
                "SKU-LOGI-M280",
                "9999999999999", // GTIN diferente no catálogo!
                "{}",
                "ACTIVE"
        );

        when(observationRepository.findByIdAndTenantId(obsId, TENANT_ID)).thenReturn(Optional.of(observation));
        when(productRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(product));
        when(productVariantRepository.findAllByTenantIdAndProductId(TENANT_ID, prodId)).thenReturn(List.of(variant));

        List<ProductMatchCandidateResponse> candidates = matchingService.matchObservation(
                TENANT_ID,
                obsId,
                new BigDecimal("0.7500")
        );

        assertThat(candidates).hasSize(1);
        MatchScoreBreakdown breakdown = candidates.get(0).scoreBreakdown();

        assertThat(breakdown.overallScore()).isLessThan(new BigDecimal("0.5000"));
        assertThat(breakdown.status()).isEqualTo(MatchStatus.NO_MATCH);
        assertThat(breakdown.explanation()).contains("Conflito de GTIN");
    }

    @Test
    @DisplayName("Normalização Textual: Desconsidera acentos, pontuação e stopwords em português")
    void shouldHandleAccentsAndStopwordsCorrectly() {
        Set<String> tokens1 = matchingService.extractTokens("Café Torrado em Grãos 1kg Especial - Com Embalagem!");
        Set<String> tokens2 = matchingService.extractTokens("cafe torrado graos 1kg especial");

        assertThat(tokens1).contains("cafe", "torrado", "graos", "1kg", "especial");
        assertThat(tokens1).doesNotContain("em", "com", "!");
        assertThat(tokens1).containsAll(tokens2);
    }

    @Test
    @DisplayName("Limiar Customizado: Limiar mais exigente (0.90) classifica match de 0.82 como POTENTIAL_MATCH")
    void shouldRespectCustomThreshold() {
        UUID obsId = UUID.randomUUID();
        MarketObservation observation = new MarketObservation(
                obsId,
                TENANT_ID,
                "MLB-TEST-THRESHOLD",
                "MERCADO_LIVRE",
                "Notebook Dell Inspiron 15 i5 8GB 256GB SSD",
                new BigDecimal("3200.0000"),
                "BRL",
                ItemCondition.NEW,
                "Dell Oficial",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-TEST-THRESHOLD",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "SCRAPER",
                BigDecimal.ONE,
                null,
                null,
                null,
                null,
                "Dell",
                null,
                null,
                null,
                null,
                null
        );

        UUID prodId = UUID.randomUUID();
        Product product = new Product(
                prodId,
                TENANT_ID,
                "Notebook Dell Inspiron 15 Polegadas Intel Core i5 8GB SSD 256GB",
                "SKU-DELL-15",
                "Dell",
                "Informática",
                "ACTIVE"
        );

        when(observationRepository.findByIdAndTenantId(obsId, TENANT_ID)).thenReturn(Optional.of(observation));
        when(productRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(product));
        when(productVariantRepository.findAllByTenantIdAndProductId(TENANT_ID, prodId)).thenReturn(List.of());

        // Limiar customizado ultra-estrito de 0.90
        List<ProductMatchCandidateResponse> candidates = matchingService.matchObservation(
                TENANT_ID,
                obsId,
                new BigDecimal("0.9000")
        );

        assertThat(candidates).hasSize(1);
        MatchScoreBreakdown breakdown = candidates.get(0).scoreBreakdown();

        // Como o score (~0.80 - 0.85) é menor que 0.9000, não pode ser MATCHED
        assertThat(breakdown.overallScore()).isLessThan(new BigDecimal("0.9000"));
        assertThat(breakdown.status()).isEqualTo(MatchStatus.POTENTIAL_MATCH);
        assertThat(breakdown.explanation()).contains("POTENTIAL_MATCH (NÃO unido automaticamente");
    }
}
