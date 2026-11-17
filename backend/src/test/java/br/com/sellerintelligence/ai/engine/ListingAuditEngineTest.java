package br.com.sellerintelligence.ai.engine;

import br.com.sellerintelligence.ai.dto.QualityAuditResultDto;
import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.sync.domain.Listing;
import br.com.sellerintelligence.sync.domain.Product;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ListingAuditEngineTest {

    private ListingAuditEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ListingAuditEngine();
    }

    @Test
    @DisplayName("Anúncio de alta qualidade obtém score elevado e critérios detalhados")
    void shouldScoreHighForOptimizedListing() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Product product = new Product(
                tenantId, storeId, "SKU-LOGITECH-MX3", "Mouse Sem Fio Logitech MX Master 3S",
                "Logitech", "Periféricos", "7891234567890", new BigDecimal("450.00"),
                DataProvenance.IMPORTED
        );

        Listing listing = new Listing(
                tenantId, storeId, product.getId(), Marketplace.MERCADO_LIVRE, "MLB99887766",
                "SKU-LOGITECH-MX3", "Mouse Sem Fio Logitech MX Master 3S Grafite 8000 DPI", "ACTIVE",
                new BigDecimal("699.90"), "BRL", 50, 12, "gold_pro",
                "https://mlb.com/item", "https://img.com/thumb.jpg", DataProvenance.IMPORTED,
                "{}", Instant.now()
        );

        String description = """
                ## Apresentação do Produto
                O mouse sem fio Logitech MX Master 3S proporciona precisão cirúrgica e cliques silenciosos.
                
                ## Ficha Técnica
                - Marca: Logitech
                - Modelo: MX Master 3S
                - Sensor: Óptico Darkfield 8000 DPI
                - Conexão: Bluetooth e receptor Logi Bolt
                - Bateria: Recarregável via USB-C
                
                ## Itens Inclusos
                - 1x Mouse Logitech MX Master 3S
                - 1x Receptor Logi Bolt
                - 1x Cabo USB-C
                """;

        Map<String, String> attributes = new HashMap<>();
        attributes.put("marca", "Logitech");
        attributes.put("modelo", "MX Master 3S");
        attributes.put("condicao", "Novo");
        attributes.put("garantia", "12 meses");
        attributes.put("cor", "Grafite");

        QualityAuditResultDto audit = engine.auditListing(listing, product, description, attributes, 5);

        assertThat(audit.overallScore()).isGreaterThanOrEqualTo(85);
        assertThat(audit.titleScore()).isEqualTo(25); // 10 len + 5 brand + 5 model + 5 no-spam
        assertThat(audit.descriptionScore()).isEqualTo(25); // 5 pres + 10 len + 10 struct
        assertThat(audit.attributesScore()).isEqualTo(30); // 5 * 6 pts
        assertThat(audit.imagesScore()).isEqualTo(20); // 5 fotos = 20 pts
        assertThat(audit.missingRequiredAttributes()).isEmpty();
    }

    @Test
    @DisplayName("Título contendo termos proibidos de spam comercial perde pontos e gera recomendação")
    void shouldPenalizeSpamWordsInTitle() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Listing listing = new Listing(
                tenantId, storeId, null, Marketplace.MERCADO_LIVRE, "MLB11223344",
                "SKU-1", "Mouse Gamer Super Promoção Frete Grátis Imperdível!!!", "ACTIVE",
                new BigDecimal("99.90"), "BRL", 10, 2, "gold_special",
                "https://mlb.com", null, DataProvenance.IMPORTED,
                "{}", Instant.now()
        );

        QualityAuditResultDto audit = engine.auditListing(listing, null, "", Map.of(), 0);

        // Verifica que o critério TITLE_NO_SPAM zerou
        assertThat(audit.criteriaBreakdown()).anyMatch(c ->
                "TITLE_NO_SPAM".equals(c.id()) && c.pointsEarned() == 0 && "CRITICAL".equals(c.status())
        );
        assertThat(audit.actionableRecommendations()).anyMatch(r -> r.contains("Remova termos promocionais do título"));
    }

    @Test
    @DisplayName("Anúncio incompleto recebe score baixo e elenca todos os atributos essenciais faltantes")
    void shouldIdentifyMissingAttributesAndLowScore() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Listing listing = new Listing(
                tenantId, storeId, null, Marketplace.SHOPEE, "SHP55443322",
                "SKU-2", "Mouse", "ACTIVE",
                new BigDecimal("29.90"), "BRL", 5, 0, "standard",
                "https://shopee.com", null, DataProvenance.IMPORTED,
                "{}", Instant.now()
        );

        QualityAuditResultDto audit = engine.auditListing(listing, null, "", Map.of(), 0);

        assertThat(audit.overallScore()).isLessThan(30);
        assertThat(audit.missingRequiredAttributes()).contains("Marca", "Modelo", "Condicao", "Garantia", "Cor");
    }
}
