package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.compliance.domain.*;
import br.com.sellerintelligence.infrastructure.security.UrlSecurityValidator;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing the centralized registry of marketplace policies and technical documentation sources.
 */
@Service
public class MarketplacePolicyRegistryService {

    private static final Logger log = LoggerFactory.getLogger(MarketplacePolicyRegistryService.class);

    private final MarketplacePolicyRepository policyRepository;
    private final UrlSecurityValidator urlSecurityValidator;
    private final AuditService auditService;

    public MarketplacePolicyRegistryService(
            MarketplacePolicyRepository policyRepository,
            UrlSecurityValidator urlSecurityValidator,
            AuditService auditService) {
        this.policyRepository = policyRepository;
        this.urlSecurityValidator = urlSecurityValidator;
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        seedDefaultPoliciesIfEmpty();
    }

    @Transactional
    public MarketplacePolicy registerPolicy(
            Marketplace marketplace,
            PolicyType policyType,
            String policyName,
            String officialUrl,
            PolicySourceType sourceType,
            String policyVersion,
            OffsetDateTime effectiveDate,
            String rawContent,
            PolicyStatus status,
            ComplianceRiskLevel riskLevel,
            String affectedFeature,
            String evidence,
            String actorId) {

        urlSecurityValidator.validatePolicyMonitoringUrl(officialUrl);

        String contentHash = computeHash(rawContent != null ? rawContent : policyName + ":" + officialUrl);

        MarketplacePolicy policy = new MarketplacePolicy(
                UUID.randomUUID(),
                marketplace,
                policyType,
                policyName,
                officialUrl,
                sourceType,
                policyVersion,
                effectiveDate,
                contentHash,
                rawContent,
                status != null ? status : PolicyStatus.ACTIVE,
                riskLevel != null ? riskLevel : ComplianceRiskLevel.MEDIUM,
                affectedFeature,
                evidence
        );

        policy = policyRepository.save(policy);

        auditService.recordEvent(
                null,
                actorId != null ? actorId : "SYSTEM",
                "POLICY_REGISTERED",
                "MarketplacePolicy",
                policy.getId().toString(),
                "Registered policy " + policyName + " for " + marketplace + " (" + policyType + ")",
                AuditResult.SUCCESS
        );

        log.info("PolicyRegistry: Registered policy '{}' for {} (hash: {})", policyName, marketplace, contentHash);
        return policy;
    }

    @Transactional(readOnly = true)
    public Optional<MarketplacePolicy> getPolicy(UUID id) {
        return policyRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<MarketplacePolicy> listAll() {
        return policyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MarketplacePolicy> listByMarketplace(Marketplace marketplace) {
        return policyRepository.findByMarketplace(marketplace);
    }

    @Transactional(readOnly = true)
    public List<MarketplacePolicy> listByStatus(PolicyStatus status) {
        return policyRepository.findByStatus(status);
    }

    @Transactional
    public void seedDefaultPoliciesIfEmpty() {
        if (policyRepository.count() > 0) {
            return;
        }

        log.info("PolicyRegistry: Seeding default verified marketplace policies...");

        // 1. Mercado Livre
        registerPolicy(
                Marketplace.MERCADO_LIVRE,
                PolicyType.DEVELOPER_POLICY,
                "Mercado Livre Developer Terms & OAuth Guidelines",
                "https://developers.mercadolivre.com.br/pt_br/autenticacao-e-autorizacao",
                PolicySourceType.DOCUMENTATION,
                "v2026.1",
                OffsetDateTime.now().minusMonths(6),
                "Mercado Livre API Terms: Mandatory OAuth 2.0 with PKCE, state token CSRF validation and rate limits.",
                PolicyStatus.ACTIVE,
                ComplianceRiskLevel.HIGH,
                "OAUTH_CONNECTION",
                "Docs verification on 2026-11-17",
                "SYSTEM_SEED"
        );

        registerPolicy(
                Marketplace.MERCADO_LIVRE,
                PolicyType.RATE_LIMIT_POLICY,
                "Mercado Livre Orders & Catalog Rate Limits",
                "https://developers.mercadolivre.com.br/pt_br/gestao-de-vendas",
                PolicySourceType.DOCUMENTATION,
                "v2026.1",
                OffsetDateTime.now().minusMonths(3),
                "Rate limit: dynamic sliding windows, max 50 orders per page, mandatory handling of 429 Too Many Requests.",
                PolicyStatus.ACTIVE,
                ComplianceRiskLevel.HIGH,
                "SYNC_ORDERS",
                "Docs verification on 2026-11-17",
                "SYSTEM_SEED"
        );

        // 2. Shopee
        registerPolicy(
                Marketplace.SHOPEE,
                PolicyType.DEVELOPER_POLICY,
                "Shopee Open Platform Developer Service Agreement",
                "https://open.shopee.com/documents/v2/v2.auth.auth_partner",
                PolicySourceType.TERMS,
                "v2.0",
                OffsetDateTime.now().minusMonths(4),
                "Shopee Open Platform Agreement: Mandatory HMAC-SHA256 signature, partner_key security and strict prohibition of web scraping.",
                PolicyStatus.ACTIVE,
                ComplianceRiskLevel.CRITICAL,
                "SYNC_CATALOG",
                "Docs verification on 2026-11-17",
                "SYSTEM_SEED"
        );

        // 3. Amazon
        registerPolicy(
                Marketplace.AMAZON,
                PolicyType.API_POLICY,
                "Amazon SP-API Acceptable Use Policy & DPP",
                "https://developer-docs.amazon.com/sp-api/docs/authorization-process",
                PolicySourceType.DOCUMENTATION,
                "v2026",
                OffsetDateTime.now().minusMonths(2),
                "Amazon SP-API Policy: LWA token expiration in 3600s, token bucket rate limits, PII minimization under DPP.",
                PolicyStatus.ACTIVE,
                ComplianceRiskLevel.CRITICAL,
                "SYNC_ORDERS",
                "Docs verification on 2026-11-17",
                "SYSTEM_SEED"
        );

        // 4. TikTok Shop (Unverified / Blocked)
        registerPolicy(
                Marketplace.TIKTOK_SHOP,
                PolicyType.DEVELOPER_POLICY,
                "TikTok Shop Partner Agreement Brazil",
                "https://partner.tiktokshop.com/doc/page/63fe7fb0317e13028c29ffc1",
                PolicySourceType.TERMS,
                "v1.0",
                OffsetDateTime.now().minusMonths(1),
                "TikTok Shop Partner terms: requires formal regional partner app registration before production calls.",
                PolicyStatus.BLOCKED,
                ComplianceRiskLevel.CRITICAL,
                "ALL_OPERATIONS",
                "Status UNVERIFIED_COMPLIANCE (Fail Closed)",
                "SYSTEM_SEED"
        );

        // 5. Magalu (Unverified / Blocked)
        registerPolicy(
                Marketplace.MAGALU,
                PolicyType.TERMS_OF_SERVICE,
                "LuizaLabs Marketplace API Terms",
                "https://developers.magazineluiza.com.br",
                PolicySourceType.TERMS,
                "v1.0",
                OffsetDateTime.now().minusMonths(1),
                "Magalu Open API Terms: Requires signed integrator agreement with LuizaLabs.",
                PolicyStatus.BLOCKED,
                ComplianceRiskLevel.CRITICAL,
                "ALL_OPERATIONS",
                "Status UNVERIFIED_COMPLIANCE (Fail Closed)",
                "SYSTEM_SEED"
        );

        // 6. SHEIN (Unverified / Blocked)
        registerPolicy(
                Marketplace.SHEIN,
                PolicyType.DEVELOPER_POLICY,
                "SHEIN Open Platform Terms of Use",
                "https://open.shein.com",
                PolicySourceType.TERMS,
                "v1.0",
                OffsetDateTime.now().minusMonths(1),
                "SHEIN Open Platform Terms: Invitation-only developer credentials per category.",
                PolicyStatus.BLOCKED,
                ComplianceRiskLevel.CRITICAL,
                "ALL_OPERATIONS",
                "Status UNVERIFIED_COMPLIANCE (Fail Closed)",
                "SYSTEM_SEED"
        );

        log.info("PolicyRegistry: Default verified policies seeded successfully.");
    }

    public static String computeHash(String input) {
        if (input == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}
