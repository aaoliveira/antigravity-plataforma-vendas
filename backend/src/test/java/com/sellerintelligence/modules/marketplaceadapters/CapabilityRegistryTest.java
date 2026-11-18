package com.sellerintelligence.modules.marketplaceadapters;

import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityEvidence;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityRegistry;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityStatus;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilityRegistryTest {

    @Test
    @DisplayName("Capacidade sem evidência registrada deve retornar status UNVERIFIED por padrão")
    void testUnregisteredCapabilityIsUnverified() {
        CapabilityRegistry registry = new CapabilityRegistry("MERCADO_LIVRE");

        assertThat(registry.getStatus(MarketplaceCapability.MARKET_SEARCH))
                .isEqualTo(CapabilityStatus.UNVERIFIED);
        assertThat(registry.getEvidence(MarketplaceCapability.MARKET_SEARCH))
                .isEmpty();
        assertThat(registry.isExecutable(MarketplaceCapability.MARKET_SEARCH))
                .isFalse();
    }

    @Test
    @DisplayName("Capacidade declarada como SUPPORTED sem evidência documental deve ser rebaixada para UNVERIFIED (Fail-Closed)")
    void testSupportedWithoutEvidenceDemotedToUnverified() {
        CapabilityRegistry registry = new CapabilityRegistry("MERCADO_LIVRE");

        // Tentativa de registrar SUPPORTED sem evidência
        registry.register(MarketplaceCapability.READ_OWN_CATALOG, CapabilityStatus.SUPPORTED, null);

        assertThat(registry.getStatus(MarketplaceCapability.READ_OWN_CATALOG))
                .isEqualTo(CapabilityStatus.UNVERIFIED);
        assertThat(registry.isExecutable(MarketplaceCapability.READ_OWN_CATALOG))
                .isFalse();
    }

    @Test
    @DisplayName("Capacidade SUPPORTED com evidência válida deve estar apta para execução")
    void testSupportedWithEvidenceIsExecutable() {
        CapabilityRegistry registry = new CapabilityRegistry("MERCADO_LIVRE");

        CapabilityEvidence evidence = new CapabilityEvidence(
                "https://developers.mercadolivre.com.br/pt_br/gerenciamento-de-pedidos",
                LocalDate.of(2026, 11, 15),
                List.of("read", "offline_access"),
                "20 req/s por token",
                "Apenas leitura de pedidos próprios",
                "v1.0",
                "Principal Architect"
        );

        registry.register(MarketplaceCapability.READ_OWN_ORDERS, CapabilityStatus.SUPPORTED, evidence);

        assertThat(registry.getStatus(MarketplaceCapability.READ_OWN_ORDERS))
                .isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(registry.getEvidence(MarketplaceCapability.READ_OWN_ORDERS))
                .contains(evidence);
        assertThat(registry.isExecutable(MarketplaceCapability.READ_OWN_ORDERS))
                .isTrue();

        // Valida campos da evidência
        assertThat(evidence.officialUrl()).contains("developers.mercadolivre.com.br");
        assertThat(evidence.requiredScopes()).contains("read", "offline_access");
        assertThat(evidence.rateLimitsAndQuotas()).isEqualTo("20 req/s por token");
        assertThat(evidence.responsibleReviewer()).isEqualTo("Principal Architect");
    }

    @Test
    @DisplayName("Capacidades UNSUPPORTED e RESTRICTED não devem ser executáveis")
    void testUnsupportedAndRestrictedNotExecutable() {
        CapabilityRegistry registry = new CapabilityRegistry("SHOPEE");

        registry.register(MarketplaceCapability.COMPETITOR_SALES_VOLUME, CapabilityStatus.UNSUPPORTED, null);
        registry.register(MarketplaceCapability.WRITE_OPERATIONS, CapabilityStatus.RESTRICTED, null);

        assertThat(registry.isExecutable(MarketplaceCapability.COMPETITOR_SALES_VOLUME)).isFalse();
        assertThat(registry.isExecutable(MarketplaceCapability.WRITE_OPERATIONS)).isFalse();
    }

    @Test
    @DisplayName("Validação de integridade nos campos obrigatórios de CapabilityEvidence")
    void testCapabilityEvidenceValidation() {
        assertThatThrownBy(() -> new CapabilityEvidence(
                "",
                LocalDate.now(),
                List.of("read"),
                "10 req/s",
                "None",
                "v1",
                "Reviewer"
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new CapabilityEvidence(
                null,
                LocalDate.now(),
                List.of("read"),
                "10 req/s",
                "None",
                "v1",
                "Reviewer"
        )).isInstanceOf(NullPointerException.class);
    }
}
