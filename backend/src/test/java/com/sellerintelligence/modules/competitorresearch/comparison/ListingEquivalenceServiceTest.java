package com.sellerintelligence.modules.competitorresearch.comparison;

import com.sellerintelligence.modules.competitorresearch.comparison.model.ComparableListingItem;
import com.sellerintelligence.modules.competitorresearch.comparison.model.CompatibilityStatus;
import com.sellerintelligence.modules.competitorresearch.comparison.model.EquivalenceScoreBreakdown;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ItemCondition;
import com.sellerintelligence.modules.competitorresearch.comparison.service.ListingEquivalenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListingEquivalenceServiceTest {

    private ListingEquivalenceService service;

    @BeforeEach
    void setUp() {
        service = new ListingEquivalenceService();
    }

    @Test
    @DisplayName("1. Deve considerar itens equivalentes quando compartilham GTIN idêntico, mesma marca e modelo compatível")
    void shouldConsiderEquivalentWhenIdenticalGtinAndBrand() {
        ComparableListingItem base = ComparableListingItem.builder()
                .listingId("MLB-BASE-01")
                .title("Monitor Dell 27 Polegadas 4K Ultra HD IPS S2722QC")
                .brand("Dell")
                .model("S2722QC")
                .gtin("0884116398905")
                .condition(ItemCondition.NEW)
                .price(new BigDecimal("2499.00"))
                .attributes(Map.of("voltage", "Bivolt", "resolution", "4K"))
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .listingId("MLB-TARGET-01")
                .title("Monitor Dell S2722QC 27\" 4K UHD IPS USB-C")
                .brand("Dell")
                .model("S2722QC")
                .gtin("0884116398905")
                .condition(ItemCondition.NEW)
                .price(new BigDecimal("2399.00"))
                .attributes(Map.of("voltage", "Bivolt", "resolution", "4K"))
                .build();

        EquivalenceScoreBreakdown breakdown = service.calculateEquivalence(base, target, new BigDecimal("0.7500"));

        assertThat(breakdown.isCompatible()).isTrue();
        assertThat(breakdown.compatibilityStatus()).isEqualTo(CompatibilityStatus.COMPATIBLE);
        assertThat(breakdown.gtinScore()).isEqualByComparingTo("1.0000");
        assertThat(breakdown.brandScore()).isEqualByComparingTo("1.0000");
        assertThat(breakdown.overallScore()).isGreaterThanOrEqualTo(new BigDecimal("0.7500"));
        assertThat(breakdown.isEquivalent()).isTrue();
        assertThat(breakdown.incompatibilityReasons()).isEmpty();
        assertThat(breakdown.warningMessage()).isNull();
        assertThat(breakdown.explanation()).contains("Itens considerados equivalentes");
    }

    @Test
    @DisplayName("2. Deve sinalizar incompatibilidade estrita quando GTINs válidos forem divergentes")
    void shouldFlagIncompatibilityWhenGtinsMismatch() {
        ComparableListingItem base = ComparableListingItem.builder()
                .listingId("MLB-BASE-02")
                .title("Smartphone Samsung Galaxy S23 128GB Preto")
                .brand("Samsung")
                .model("Galaxy S23")
                .gtin("7892509127814")
                .condition(ItemCondition.NEW)
                .price(new BigDecimal("3199.00"))
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .listingId("MLB-TARGET-02")
                .title("Smartphone Samsung Galaxy S23 256GB Verde")
                .brand("Samsung")
                .model("Galaxy S23")
                .gtin("7892509127821") // GTIN diferente
                .condition(ItemCondition.NEW)
                .price(new BigDecimal("3499.00"))
                .build();

        EquivalenceScoreBreakdown breakdown = service.calculateEquivalence(base, target, new BigDecimal("0.7500"));

        assertThat(breakdown.isCompatible()).isFalse();
        assertThat(breakdown.compatibilityStatus()).isEqualTo(CompatibilityStatus.INCOMPATIBLE);
        assertThat(breakdown.isEquivalent()).isFalse(); // NUNCA equivalente se incompatível
        assertThat(breakdown.incompatibilityReasons()).anyMatch(r -> r.contains("Códigos GTIN/EAN conflitantes"));
        assertThat(breakdown.warningMessage()).contains("AVISO DE INCOMPATIBILIDADE");
        assertThat(breakdown.explanation()).contains("reprovada por incompatibilidade crítica");
    }

    @Test
    @DisplayName("3. Deve sinalizar incompatibilidade quando marcas conhecidas forem divergentes")
    void shouldFlagIncompatibilityWhenBrandsMismatch() {
        ComparableListingItem base = ComparableListingItem.builder()
                .listingId("MLB-BASE-03")
                .title("Notebook Dell Inspiron 15 Core i5 8GB 256GB")
                .brand("Dell")
                .model("Inspiron 15")
                .condition(ItemCondition.NEW)
                .price(new BigDecimal("2999.00"))
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .listingId("MLB-TARGET-03")
                .title("Notebook Lenovo Ideapad 3 Core i5 8GB 256GB")
                .brand("Lenovo")
                .model("Ideapad 3")
                .condition(ItemCondition.NEW)
                .price(new BigDecimal("2899.00"))
                .build();

        EquivalenceScoreBreakdown breakdown = service.calculateEquivalence(base, target, new BigDecimal("0.7500"));

        assertThat(breakdown.isCompatible()).isFalse();
        assertThat(breakdown.compatibilityStatus()).isEqualTo(CompatibilityStatus.INCOMPATIBLE);
        assertThat(breakdown.isEquivalent()).isFalse();
        assertThat(breakdown.brandScore()).isEqualByComparingTo("0.0000");
        assertThat(breakdown.incompatibilityReasons()).anyMatch(r -> r.contains("Marcas conflitantes"));
    }

    @Test
    @DisplayName("4. Deve sinalizar incompatibilidade quando condição do item for divergente (Novo vs Usado)")
    void shouldFlagIncompatibilityWhenConditionsMismatch() {
        ComparableListingItem base = ComparableListingItem.builder()
                .listingId("MLB-BASE-04")
                .title("Câmera Canon EOS Rebel T7 com Lente 18-55mm")
                .brand("Canon")
                .model("EOS Rebel T7")
                .condition(ItemCondition.NEW)
                .price(new BigDecimal("3200.00"))
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .listingId("MLB-TARGET-04")
                .title("Câmera Canon EOS Rebel T7 com Lente 18-55mm Usada Pouco Uso")
                .brand("Canon")
                .model("EOS Rebel T7")
                .condition(ItemCondition.USED)
                .price(new BigDecimal("2100.00"))
                .build();

        EquivalenceScoreBreakdown breakdown = service.calculateEquivalence(base, target, new BigDecimal("0.7500"));

        assertThat(breakdown.isCompatible()).isFalse();
        assertThat(breakdown.compatibilityStatus()).isEqualTo(CompatibilityStatus.INCOMPATIBLE);
        assertThat(breakdown.isEquivalent()).isFalse();
        assertThat(breakdown.incompatibilityReasons()).anyMatch(r -> r.contains("Condição divergente"));
    }

    @Test
    @DisplayName("5. Deve sinalizar incompatibilidade quando voltagem for divergente (110V vs 220V)")
    void shouldFlagIncompatibilityWhenVoltageMismatches() {
        ComparableListingItem base = ComparableListingItem.builder()
                .listingId("MLB-BASE-05")
                .title("Fritadeira Sem Óleo Air Fryer Mondial 4L 110V")
                .brand("Mondial")
                .attributes(Map.of("voltagem", "110V"))
                .condition(ItemCondition.NEW)
                .price(new BigDecimal("329.00"))
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .listingId("MLB-TARGET-05")
                .title("Fritadeira Sem Óleo Air Fryer Mondial 4L 220V")
                .brand("Mondial")
                .attributes(Map.of("voltagem", "220V"))
                .condition(ItemCondition.NEW)
                .price(new BigDecimal("319.00"))
                .build();

        EquivalenceScoreBreakdown breakdown = service.calculateEquivalence(base, target, new BigDecimal("0.7500"));

        assertThat(breakdown.isCompatible()).isFalse();
        assertThat(breakdown.compatibilityStatus()).isEqualTo(CompatibilityStatus.INCOMPATIBLE);
        assertThat(breakdown.isEquivalent()).isFalse();
        assertThat(breakdown.incompatibilityReasons()).anyMatch(r -> r.contains("voltagem incompatível"));
    }

    @Test
    @DisplayName("6. Deve sinalizar incompatibilidade estrutural quando um for acessório e o outro for o aparelho principal")
    void shouldFlagIncompatibilityWhenAccessoryVsDevice() {
        ComparableListingItem base = ComparableListingItem.builder()
                .listingId("MLB-BASE-06")
                .title("Apple iPhone 15 128GB Estelar")
                .brand("Apple")
                .condition(ItemCondition.NEW)
                .price(new BigDecimal("4899.00"))
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .listingId("MLB-TARGET-06")
                .title("Capa Case de Silicone para iPhone 15 Anti Impacto")
                .brand("Apple")
                .condition(ItemCondition.NEW)
                .price(new BigDecimal("49.90"))
                .build();

        EquivalenceScoreBreakdown breakdown = service.calculateEquivalence(base, target, new BigDecimal("0.7500"));

        assertThat(breakdown.isCompatible()).isFalse();
        assertThat(breakdown.compatibilityStatus()).isEqualTo(CompatibilityStatus.INCOMPATIBLE);
        assertThat(breakdown.isEquivalent()).isFalse();
        assertThat(breakdown.incompatibilityReasons()).anyMatch(r -> r.contains("acessório"));
    }
}
