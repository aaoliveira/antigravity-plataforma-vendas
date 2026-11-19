package com.sellerintelligence.modules.aiassistant.draft.catalog;

import com.sellerintelligence.modules.aiassistant.draft.model.MarketplaceRuleLimits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MarketplaceLimitCatalog - Testes de Regras Oficiais de Limites de Caracteres (P020-BE)")
class MarketplaceLimitCatalogTest {

    private MarketplaceLimitCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new MarketplaceLimitCatalog();
    }

    @Test
    @DisplayName("Deve retornar limites oficiais homologados para Mercado Livre")
    void shouldReturnOfficialLimitsForMercadoLivre() {
        Optional<MarketplaceRuleLimits> limitsOpt = catalog.getLimits("MERCADO_LIVRE");

        assertThat(limitsOpt).isPresent();
        MarketplaceRuleLimits limits = limitsOpt.get();
        assertThat(limits.minTitleLength()).isEqualTo(1);
        assertThat(limits.maxTitleLength()).isEqualTo(60);
        assertThat(limits.minDescriptionLength()).isEqualTo(1);
        assertThat(limits.maxDescriptionLength()).isEqualTo(25000);
        assertThat(limits.plainTextOnly()).isTrue();
        assertThat(limits.officialRuleSource()).contains("Mercado Livre Developers");
    }

    @Test
    @DisplayName("Deve retornar limites oficiais homologados para Shopee")
    void shouldReturnOfficialLimitsForShopee() {
        Optional<MarketplaceRuleLimits> limitsOpt = catalog.getLimits("SHOPEE");

        assertThat(limitsOpt).isPresent();
        MarketplaceRuleLimits limits = limitsOpt.get();
        assertThat(limits.minTitleLength()).isEqualTo(20);
        assertThat(limits.maxTitleLength()).isEqualTo(120);
        assertThat(limits.minDescriptionLength()).isEqualTo(20);
        assertThat(limits.maxDescriptionLength()).isEqualTo(3000);
        assertThat(limits.plainTextOnly()).isTrue();
        assertThat(limits.officialRuleSource()).contains("Shopee Open Platform");
    }

    @Test
    @DisplayName("Deve rejeitar marketplace sem regra oficial validada no MVP")
    void shouldRejectUnvalidatedMarketplace() {
        Optional<MarketplaceRuleLimits> limitsOpt = catalog.getLimits("TIKTOK_SHOP");
        assertThat(limitsOpt).isEmpty();

        assertThatThrownBy(() -> catalog.validateLimits("TIKTOK_SHOP", "Título Válido", "Descrição Válida"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não possui regras oficiais");
    }

    @Test
    @DisplayName("Deve aprovar título e descrição dentro dos limites oficiais do Mercado Livre")
    void shouldApproveValidMercadoLivreLimits() {
        String title = "Panela Tramontina Solar Aço Inox 20cm";
        String description = "Descrição de teste com especificações confirmadas.";

        assertThatCode(() -> catalog.validateLimits("MERCADO_LIVRE", title, description))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar título do Mercado Livre que exceda 60 caracteres")
    void shouldRejectMercadoLivreTitleExceeding60Chars() {
        String titleTooLong = "Panela Tramontina Solar em Aço Inox com Fundo Triplo Original de Fábrica para Cozinha";
        assertThat(titleTooLong.length()).isGreaterThan(60);

        assertThatThrownBy(() -> catalog.validateLimits("MERCADO_LIVRE", titleTooLong, "Desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fora do limite oficial de [1-60] caracteres");
    }

    @Test
    @DisplayName("Deve rejeitar título da Shopee menor que 20 caracteres")
    void shouldRejectShopeeTitleLessThan20Chars() {
        String titleTooShort = "Cabo USB";
        assertThat(titleTooShort.length()).isLessThan(20);

        assertThatThrownBy(() -> catalog.validateLimits("SHOPEE", titleTooShort, "Descrição com mais de 20 caracteres para teste."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fora do limite oficial de [20-120] caracteres");
    }
}
