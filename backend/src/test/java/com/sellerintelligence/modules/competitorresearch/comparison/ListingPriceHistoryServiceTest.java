package com.sellerintelligence.modules.competitorresearch.comparison;

import com.sellerintelligence.modules.competitorresearch.comparison.entity.ListingPriceHistoryRecord;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ListingPriceHistoryResponse;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ListingPricePointDto;
import com.sellerintelligence.modules.competitorresearch.comparison.model.RecordPricePointRequest;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ShippingStatus;
import com.sellerintelligence.modules.competitorresearch.comparison.repository.ListingPriceHistoryRepository;
import com.sellerintelligence.modules.competitorresearch.comparison.service.ListingPriceHistoryService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingPriceHistoryServiceTest {

    @Mock
    private ListingPriceHistoryRepository historyRepository;

    private ListingPriceHistoryService service;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ListingPriceHistoryService(historyRepository);
    }

    @Test
    @DisplayName("1. Deve registrar ponto legítimo de preço com timestamps auditáveis")
    void shouldRecordLegitimatePricePoint() {
        Instant observedAt = Instant.now().minus(1, ChronoUnit.HOURS);
        RecordPricePointRequest request = new RecordPricePointRequest(
                "MERCADO_LIVRE",
                "MLB123456789",
                "Placa de Vídeo RTX 4060",
                new BigDecimal("2199.9000"),
                new BigDecimal("35.0000"),
                observedAt,
                "OFFICIAL",
                "https://produto.mercadolivre.com.br/MLB-123456789"
        );

        when(historyRepository.save(any(ListingPriceHistoryRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ListingPricePointDto result = service.recordPricePoint(tenantId, request);

        assertThat(result.price()).isEqualByComparingTo("2199.9000");
        assertThat(result.shippingCost()).isEqualByComparingTo("35.0000");
        assertThat(result.shippingStatus()).isEqualTo(ShippingStatus.KNOWN);
        assertThat(result.finalPriceWithShipping()).isEqualByComparingTo("2234.9000");
        assertThat(result.observedAt()).isEqualTo(observedAt);
        assertThat(result.sourceType()).isEqualTo("OFFICIAL");

        verify(historyRepository).save(any(ListingPriceHistoryRecord.class));
    }

    @Test
    @DisplayName("2. Deve retornar histórico com apenas 1 ponto e SEM retroatividade inventada para anúncio recém-incluído")
    void shouldReturnSinglePointWithoutFakeHistoryForNewlyAddedListing() {
        Instant now = Instant.now();
        ListingPriceHistoryRecord r1 = new ListingPriceHistoryRecord(
                UUID.randomUUID(),
                tenantId,
                "MERCADO_LIVRE",
                "MLB987654321",
                "Processador AMD Ryzen 7 5700X",
                new BigDecimal("1150.0000"),
                "BRL",
                null,
                now,
                now,
                "OFFICIAL",
                "https://produto.mercadolivre.com.br/MLB-987654321"
        );

        when(historyRepository.findHistoryInWindow(eq(tenantId), eq("MERCADO_LIVRE"), eq("MLB987654321"), any(), any()))
                .thenReturn(List.of(r1));

        ListingPriceHistoryResponse response = service.getPriceHistory(
                tenantId, "MERCADO_LIVRE", "MLB987654321", null, null
        );

        assertThat(response.dataPointsCount()).isEqualTo(1);
        assertThat(response.hasHistoricalTrend()).isFalse(); // 1 ponto não forma tendência
        assertThat(response.points()).hasSize(1);
        assertThat(response.historyNotice()).contains("A plataforma NUNCA inventa dados retroativos passados");
        assertThat(response.trendDescription()).contains("Apenas uma observação registrada");
        assertThat(response.latestPrice()).isEqualByComparingTo("1150.0000");
        assertThat(response.earliestPrice()).isEqualByComparingTo("1150.0000");
    }

    @Test
    @DisplayName("3. Deve calcular variação e tendência histórica somente a partir de pontos legítimos reais")
    void shouldCalculateTrendFromRealObservedPoints() {
        Instant t0 = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant t1 = Instant.now().minus(5, ChronoUnit.DAYS);
        Instant t2 = Instant.now();

        ListingPriceHistoryRecord r1 = new ListingPriceHistoryRecord(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "MLB111",
                "Smart TV 50 Polegadas 4K", new BigDecimal("2000.0000"), "BRL",
                new BigDecimal("50.0000"), t0, t0, "OFFICIAL", null
        );

        ListingPriceHistoryRecord r2 = new ListingPriceHistoryRecord(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "MLB111",
                "Smart TV 50 Polegadas 4K", new BigDecimal("1900.0000"), "BRL",
                new BigDecimal("50.0000"), t1, t1, "OFFICIAL", null
        );

        ListingPriceHistoryRecord r3 = new ListingPriceHistoryRecord(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "MLB111",
                "Smart TV 50 Polegadas 4K", new BigDecimal("1800.0000"), "BRL",
                new BigDecimal("50.0000"), t2, t2, "OFFICIAL", null
        );

        when(historyRepository.findHistoryInWindow(eq(tenantId), eq("MERCADO_LIVRE"), eq("MLB111"), any(), any()))
                .thenReturn(List.of(r1, r2, r3));

        ListingPriceHistoryResponse response = service.getPriceHistory(
                tenantId, "MERCADO_LIVRE", "MLB111", null, null
        );

        assertThat(response.dataPointsCount()).isEqualTo(3);
        assertThat(response.hasHistoricalTrend()).isTrue();
        assertThat(response.earliestPrice()).isEqualByComparingTo("2000.0000");
        assertThat(response.latestPrice()).isEqualByComparingTo("1800.0000");
        assertThat(response.minPrice()).isEqualByComparingTo("1800.0000");
        assertThat(response.maxPrice()).isEqualByComparingTo("2000.0000");
        assertThat(response.overallVariationPercentage()).isEqualByComparingTo("-10.0000"); // -10.0000%
        assertThat(response.trendDescription()).contains("Redução de preço de -10.00%");
        assertThat(response.historyNotice()).contains("Histórico auditado com 3 pontos reais observados");
    }

    @Test
    @DisplayName("4. Deve retornar resposta vazia com aviso quando não houver histórico")
    void shouldReturnEmptyWhenNoHistory() {
        when(historyRepository.findHistoryInWindow(eq(tenantId), eq("MERCADO_LIVRE"), eq("MLB_NONE"), any(), any()))
                .thenReturn(List.of());

        ListingPriceHistoryResponse response = service.getPriceHistory(
                tenantId, "MERCADO_LIVRE", "MLB_NONE", null, null
        );

        assertThat(response.dataPointsCount()).isEqualTo(0);
        assertThat(response.hasHistoricalTrend()).isFalse();
        assertThat(response.points()).isEmpty();
        assertThat(response.trendDescription()).contains("Nenhum histórico registrado");
    }
}
