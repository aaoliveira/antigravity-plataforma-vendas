package com.sellerintelligence.modules.competitorresearch.comparison;

import com.sellerintelligence.modules.competitorresearch.comparison.model.ComparableListingItem;
import com.sellerintelligence.modules.competitorresearch.comparison.model.PriceComparisonResult;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ShippingStatus;
import com.sellerintelligence.modules.competitorresearch.comparison.model.TemporalComparabilityStatus;
import com.sellerintelligence.modules.competitorresearch.comparison.service.ListingPriceComparisonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ListingPriceComparisonServiceTest {

    private ListingPriceComparisonService service;

    @BeforeEach
    void setUp() {
        service = new ListingPriceComparisonService();
    }

    @Test
    @DisplayName("1. Deve calcular variação de preço em BigDecimal quando as observações estiverem dentro da janela comparável")
    void shouldCalculatePriceVariationWhenWithinComparableTemporalWindow() {
        Instant now = Instant.now();
        Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

        ComparableListingItem base = ComparableListingItem.builder()
                .title("Teclado Mecânico Gamer Redragon Kumara")
                .price(new BigDecimal("200.0000"))
                .observedAt(fiveDaysAgo)
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .title("Teclado Mecânico Gamer Redragon Kumara K552")
                .price(new BigDecimal("220.0000"))
                .observedAt(now)
                .build();

        PriceComparisonResult result = service.comparePrices(base, target, 30);

        assertThat(result.isTemporalWindowComparable()).isTrue();
        assertThat(result.temporalStatus()).isEqualTo(TemporalComparabilityStatus.COMPARABLE);
        assertThat(result.priceDifference()).isEqualByComparingTo("20.0000");
        assertThat(result.priceVariationPercentage()).isEqualByComparingTo("10.0000"); // +10.0000%
        assertThat(result.timeDeltaDays()).isEqualTo(5L);
        assertThat(result.temporalWarningMessage()).isNull();
    }

    @Test
    @DisplayName("2. NÃO deve calcular variação de preço quando a diferença de tempo exceder a janela comparável")
    void shouldRefusePriceVariationWhenOutsideComparableTemporalWindow() {
        Instant now = Instant.now();
        Instant sixtyDaysAgo = now.minus(60, ChronoUnit.DAYS);

        ComparableListingItem base = ComparableListingItem.builder()
                .title("Mouse Sem Fio Logitech MX Master 3S")
                .price(new BigDecimal("500.0000"))
                .observedAt(sixtyDaysAgo) // 60 dias atrás
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .title("Mouse Sem Fio Logitech MX Master 3S")
                .price(new BigDecimal("600.0000"))
                .observedAt(now)
                .build();

        PriceComparisonResult result = service.comparePrices(base, target, 30);

        assertThat(result.isTemporalWindowComparable()).isFalse();
        assertThat(result.temporalStatus()).isEqualTo(TemporalComparabilityStatus.NON_COMPARABLE);
        assertThat(result.priceDifference()).isNull();
        assertThat(result.priceVariationPercentage()).isNull();
        assertThat(result.timeDeltaDays()).isEqualTo(60L);
        assertThat(result.temporalWarningMessage())
                .contains("Variação de preço não calculada")
                .contains("excede a janela máxima comparável");
    }

    @Test
    @DisplayName("3. NUNCA deve inferir preço final com frete quando o custo de frete for desconhecido (null)")
    void shouldNeverInferFinalPriceWhenShippingIsUnknown() {
        ComparableListingItem base = ComparableListingItem.builder()
                .title("SSD Kingston NV2 1TB NVMe M.2")
                .price(new BigDecimal("400.0000"))
                .shippingCost(null) // Frete desconhecido na API oficial
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .title("SSD Kingston NV2 1TB NVMe M.2 2280")
                .price(new BigDecimal("390.0000"))
                .shippingCost(new BigDecimal("25.0000")) // Frete conhecido
                .build();

        PriceComparisonResult result = service.comparePrices(base, target, 30);

        assertThat(result.baseShippingStatus()).isEqualTo(ShippingStatus.UNKNOWN);
        assertThat(result.baseShippingCost()).isNull();
        assertThat(result.baseFinalPriceWithShipping()).isNull(); // NUNCA inferido!

        assertThat(result.targetShippingStatus()).isEqualTo(ShippingStatus.KNOWN);
        assertThat(result.targetShippingCost()).isEqualByComparingTo("25.0000");
        assertThat(result.targetFinalPriceWithShipping()).isEqualByComparingTo("415.0000");

        assertThat(result.totalPriceDifferenceWithShipping()).isNull();
        assertThat(result.totalPriceVariationPercentageWithShipping()).isNull();
        assertThat(result.shippingNotice()).contains("Custo de frete não informado ou indisponível em um ou ambos os anúncios (UNKNOWN)");
    }

    @Test
    @DisplayName("4. Deve calcular preço final com frete quando ambos tiverem frete oficialmente conhecido")
    void shouldCalculateFinalPriceWithShippingWhenBothKnown() {
        ComparableListingItem base = ComparableListingItem.builder()
                .title("Cadeira Ergonômica de Escritório")
                .price(new BigDecimal("1000.0000"))
                .shippingCost(new BigDecimal("50.0000"))
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .title("Cadeira Ergonômica de Escritório Mesh")
                .price(new BigDecimal("950.0000"))
                .shippingCost(new BigDecimal("70.0000"))
                .build();

        PriceComparisonResult result = service.comparePrices(base, target, 30);

        assertThat(result.baseShippingStatus()).isEqualTo(ShippingStatus.KNOWN);
        assertThat(result.baseFinalPriceWithShipping()).isEqualByComparingTo("1050.0000");

        assertThat(result.targetShippingStatus()).isEqualTo(ShippingStatus.KNOWN);
        assertThat(result.targetFinalPriceWithShipping()).isEqualByComparingTo("1020.0000");

        assertThat(result.totalPriceDifferenceWithShipping()).isEqualByComparingTo("-30.0000");
        assertThat(result.shippingNotice()).isNull();
    }
}
