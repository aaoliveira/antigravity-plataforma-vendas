package com.sellerintelligence.modules.competitorresearch;

import com.sellerintelligence.modules.competitorresearch.model.MarketSearchSourcePolicy;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchSourcePolicyRepository;
import com.sellerintelligence.modules.competitorresearch.service.SearchSourcePolicyRegistry;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityStatus;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchSourcePolicyRegistryTest {

    @Mock
    private MarketSearchSourcePolicyRepository repository;

    private SearchSourcePolicyRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SearchSourcePolicyRegistry(repository);
    }

    @Test
    @DisplayName("Deve resolver política para Mercado Livre como SUPPORTED e com busca pública habilitada")
    void shouldResolveMlbPolicyAsSupported() {
        when(repository.findByIdAndIsActiveTrue(SearchSourcePolicyRegistry.POLICY_MLB_PUBLIC))
                .thenReturn(Optional.empty()); // Aciona fallback

        MarketSearchSourcePolicy policy = registry.resolvePolicyForSearch("MERCADO_LIVRE", false);

        assertThat(policy).isNotNull();
        assertThat(policy.getId()).isEqualTo(SearchSourcePolicyRegistry.POLICY_MLB_PUBLIC);
        assertThat(policy.getCapabilityStatus()).isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(policy.getCapability()).isEqualTo(MarketplaceCapability.MARKET_SEARCH);
        assertThat(policy.isAllowsCompetitorSearch()).isTrue();
        assertThat(policy.getRateLimitPerMinute()).isEqualTo(60);
    }

    @Test
    @DisplayName("Deve resolver política para Shopee como RESTRICTED e proibir busca concorrencial aberta")
    void shouldResolveShopeePolicyAsRestricted() {
        when(repository.findByIdAndIsActiveTrue(SearchSourcePolicyRegistry.POLICY_SHOPEE_RESTRICTED))
                .thenReturn(Optional.empty());

        MarketSearchSourcePolicy policy = registry.resolvePolicyForSearch("SHOPEE", false);

        assertThat(policy).isNotNull();
        assertThat(policy.getId()).isEqualTo(SearchSourcePolicyRegistry.POLICY_SHOPEE_RESTRICTED);
        assertThat(policy.getCapabilityStatus()).isEqualTo(CapabilityStatus.RESTRICTED);
        assertThat(policy.isAllowsCompetitorSearch()).isFalse();
    }

    @Test
    @DisplayName("Deve resolver política para Amazon como UNVERIFIED e com rate limit zero")
    void shouldResolveAmazonPolicyAsUnverified() {
        when(repository.findByIdAndIsActiveTrue(SearchSourcePolicyRegistry.POLICY_AMAZON_UNVERIFIED))
                .thenReturn(Optional.empty());

        MarketSearchSourcePolicy policy = registry.resolvePolicyForSearch("AMAZON", false);

        assertThat(policy).isNotNull();
        assertThat(policy.getId()).isEqualTo(SearchSourcePolicyRegistry.POLICY_AMAZON_UNVERIFIED);
        assertThat(policy.getCapabilityStatus()).isEqualTo(CapabilityStatus.UNVERIFIED);
        assertThat(policy.isAllowsCompetitorSearch()).isFalse();
        assertThat(policy.getRateLimitPerMinute()).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve resolver política para Catálogo Próprio quando flag onlyOwnCatalog for true")
    void shouldResolveOwnCatalogPolicyWhenOnlyOwnCatalogIsTrue() {
        when(repository.findByIdAndIsActiveTrue(SearchSourcePolicyRegistry.POLICY_OWN_CATALOG))
                .thenReturn(Optional.empty());

        MarketSearchSourcePolicy policy = registry.resolvePolicyForSearch("MERCADO_LIVRE", true);

        assertThat(policy).isNotNull();
        assertThat(policy.getId()).isEqualTo(SearchSourcePolicyRegistry.POLICY_OWN_CATALOG);
        assertThat(policy.getCapabilityStatus()).isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(policy.getCapability()).isEqualTo(MarketplaceCapability.READ_OWN_CATALOG);
        assertThat(policy.isAllowsCompetitorSearch()).isFalse();
    }

    @Test
    @DisplayName("Deve listar todas as políticas ativas padrão quando banco estiver vazio")
    void shouldReturnDefaultActivePoliciesWhenDbIsEmpty() {
        when(repository.findAllByIsActiveTrue()).thenReturn(Collections.emptyList());

        List<MarketSearchSourcePolicy> policies = registry.getAllActivePolicies();

        assertThat(policies).hasSize(5);
        assertThat(policies).extracting(MarketSearchSourcePolicy::getId)
                .containsExactlyInAnyOrder(
                        SearchSourcePolicyRegistry.POLICY_MLB_PUBLIC,
                        SearchSourcePolicyRegistry.POLICY_SHOPEE_RESTRICTED,
                        SearchSourcePolicyRegistry.POLICY_AMAZON_UNVERIFIED,
                        SearchSourcePolicyRegistry.POLICY_OWN_CATALOG,
                        SearchSourcePolicyRegistry.POLICY_LICENSED_PROVIDER
                );
    }
}
