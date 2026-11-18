package com.sellerintelligence.modules.competitorresearch.service;

import com.sellerintelligence.modules.competitorresearch.model.MarketSearchSourcePolicy;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchSourcePolicyRepository;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityStatus;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Registro de políticas oficiais de fontes de dados e pesquisa de mercado.
 * Centraliza os requisitos de autorização, rate limits e conformidade com os Termos de Serviço dos canais.
 */
@Service
public class SearchSourcePolicyRegistry {

    private static final Logger log = LoggerFactory.getLogger(SearchSourcePolicyRegistry.class);

    public static final String POLICY_MLB_PUBLIC = "POL-MLB-PUBLIC-01";
    public static final String POLICY_SHOPEE_RESTRICTED = "POL-SHOPEE-RESTRICTED-01";
    public static final String POLICY_AMAZON_UNVERIFIED = "POL-AMAZON-UNVERIFIED-01";
    public static final String POLICY_OWN_CATALOG = "POL-OWN-CATALOG-01";
    public static final String POLICY_LICENSED_PROVIDER = "POL-LICENSED-PROVIDER-01";

    private final MarketSearchSourcePolicyRepository policyRepository;

    public SearchSourcePolicyRegistry(MarketSearchSourcePolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public List<MarketSearchSourcePolicy> getAllActivePolicies() {
        List<MarketSearchSourcePolicy> policies = policyRepository.findAllByIsActiveTrue();
        if (policies.isEmpty()) {
            return getDefaultFallbackPolicies();
        }
        return policies;
    }

    public Optional<MarketSearchSourcePolicy> getPolicyById(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            return Optional.empty();
        }
        Optional<MarketSearchSourcePolicy> found = policyRepository.findByIdAndIsActiveTrue(policyId);
        if (found.isPresent()) {
            return found;
        }
        return getDefaultFallbackPolicies().stream()
                .filter(p -> p.getId().equalsIgnoreCase(policyId))
                .findFirst();
    }

    public MarketSearchSourcePolicy resolvePolicyForSearch(String marketplace, boolean onlyOwnCatalog) {
        if (onlyOwnCatalog) {
            return getPolicyById(POLICY_OWN_CATALOG)
                    .orElseGet(this::createDefaultOwnCatalogPolicy);
        }

        if (marketplace == null || marketplace.isBlank() || "ALL".equalsIgnoreCase(marketplace)) {
            // Padrão: Catálogo Próprio ou MLB público se busca aberta
            return getPolicyById(POLICY_MLB_PUBLIC)
                    .orElseGet(this::createDefaultMlbPolicy);
        }

        String normalizedMkt = marketplace.trim().toUpperCase();
        return switch (normalizedMkt) {
            case "MERCADO_LIVRE", "MLB" -> getPolicyById(POLICY_MLB_PUBLIC).orElseGet(this::createDefaultMlbPolicy);
            case "SHOPEE" -> getPolicyById(POLICY_SHOPEE_RESTRICTED).orElseGet(this::createDefaultShopeePolicy);
            case "AMAZON" -> getPolicyById(POLICY_AMAZON_UNVERIFIED).orElseGet(this::createDefaultAmazonPolicy);
            case "LICENSED", "LICENSED_PROVIDER" -> getPolicyById(POLICY_LICENSED_PROVIDER).orElseGet(this::createDefaultLicensedPolicy);
            default -> getPolicyById(POLICY_OWN_CATALOG).orElseGet(this::createDefaultOwnCatalogPolicy);
        };
    }

    private List<MarketSearchSourcePolicy> getDefaultFallbackPolicies() {
        return List.of(
                createDefaultMlbPolicy(),
                createDefaultShopeePolicy(),
                createDefaultAmazonPolicy(),
                createDefaultOwnCatalogPolicy(),
                createDefaultLicensedPolicy()
        );
    }

    private MarketSearchSourcePolicy createDefaultMlbPolicy() {
        return new MarketSearchSourcePolicy(
                POLICY_MLB_PUBLIC,
                "Mercado Livre Pesquisa Pública e Catálogo",
                "MERCADO_LIVRE",
                MarketplaceCapability.MARKET_SEARCH,
                CapabilityStatus.SUPPORTED,
                "PUBLIC_RATE_LIMITED",
                "https://api.mercadolibre.com/sites/MLB/search",
                "https://developers.mercadolibre.com.br/pt_br/pesquisa-de-produtos",
                60,
                true,
                "Uso estrito de endpoints públicos permitidos. Scraping hostil e extração de dados privados são estritamente vedados.",
                true
        );
    }

    private MarketSearchSourcePolicy createDefaultShopeePolicy() {
        return new MarketSearchSourcePolicy(
                POLICY_SHOPEE_RESTRICTED,
                "Shopee Open Platform Product Search",
                "SHOPEE",
                MarketplaceCapability.MARKET_SEARCH,
                CapabilityStatus.RESTRICTED,
                "HMAC_PARTNER_APP",
                "https://partner.shopeemobile.com/api/v2/product/search_item",
                "https://open.shopee.com/documents/v2/v2.product.search_item",
                10,
                false,
                "Pesquisa aberta e scraping de concorrentes são expressamente vedados pelas diretrizes da Shopee Open Platform.",
                true
        );
    }

    private MarketSearchSourcePolicy createDefaultAmazonPolicy() {
        return new MarketSearchSourcePolicy(
                POLICY_AMAZON_UNVERIFIED,
                "Amazon SP-API Pricing & Catalog",
                "AMAZON",
                MarketplaceCapability.MARKET_SEARCH,
                CapabilityStatus.UNVERIFIED,
                "LWA_IAM_CREDENTIALS",
                "https://sellingpartnerapi-na.amazon.com/catalog/2022-04-01/items",
                "https://developer-docs.amazon.com/sp-api",
                0,
                false,
                "Conector e escopos da Amazon SP-API em fase de homologação (UNVERIFIED). Todas as chamadas bloqueadas via Fail-Closed.",
                true
        );
    }

    private MarketSearchSourcePolicy createDefaultOwnCatalogPolicy() {
        return new MarketSearchSourcePolicy(
                POLICY_OWN_CATALOG,
                "Catálogo Próprio Canônico e Anúncios do Tenant",
                "INTERNAL",
                MarketplaceCapability.READ_OWN_CATALOG,
                CapabilityStatus.SUPPORTED,
                "TENANT_INTERNAL_SESSION",
                "internal://catalog-service",
                "internal://docs/catalog-and-listings",
                300,
                false,
                "Consulta restrita e isolada ao inventário próprio do tenant autenticado.",
                true
        );
    }

    private MarketSearchSourcePolicy createDefaultLicensedPolicy() {
        return new MarketSearchSourcePolicy(
                POLICY_LICENSED_PROVIDER,
                "Provedor Homologado de Dados e Benchmarking de Mercado",
                "LICENSED_PROVIDER",
                MarketplaceCapability.MARKET_SEARCH,
                CapabilityStatus.SUPPORTED,
                "LICENSED_DATA_KEY",
                "https://api.licensed-provider.internal/v1/search",
                "https://sellerintelligence.com/compliance/licensed-data",
                120,
                true,
                "Base de dados agregada e licenciada comercialmente para inteligência de mercado, em conformidade com as leis aplicáveis.",
                true
        );
    }
}
