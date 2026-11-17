package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.compliance.domain.ComplianceCheckResult;
import br.com.sellerintelligence.compliance.domain.ComplianceStatus;
import br.com.sellerintelligence.compliance.domain.OperationRiskLevel;
import br.com.sellerintelligence.compliance.exception.ComplianceViolationException;
import br.com.sellerintelligence.connector.model.MarketplaceCredential;
import br.com.sellerintelligence.connector.model.MarketplaceCredentialRepository;
import br.com.sellerintelligence.tenancy.store.ConnectionStatus;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import br.com.sellerintelligence.tenancy.store.StoreStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central compliance and policy enforcement component for all marketplace interactions.
 * Strictly operates under a FAIL CLOSED architecture:
 * If an operation, credential, store state, or channel compliance cannot be verified,
 * execution is rejected immediately.
 */
@Service
public class MarketplaceComplianceGuard {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceComplianceGuard.class);

    private static final int DEFAULT_CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final long DEFAULT_CIRCUIT_COOLDOWN_SECONDS = 60L;
    private static final long DEFAULT_429_COOLDOWN_SECONDS = 60L;

    private final StoreRepository storeRepository;
    private final MarketplaceCredentialRepository credentialRepository;
    private final AuditService auditService;

    private final int circuitBreakerThreshold;
    private final long circuitCooldownSeconds;
    private final boolean allowFakeMarketplace;

    // In-memory operational protection state
    private final Map<UUID, Integer> consecutiveFailuresByStore = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> circuitOpenUntilByStore = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> rateLimitCooldownUntilByStore = new ConcurrentHashMap<>();

    // Static compliance registry per marketplace
    private final Map<Marketplace, ComplianceStatus> marketplaceComplianceMap = new ConcurrentHashMap<>();

    public MarketplaceComplianceGuard(
            StoreRepository storeRepository,
            MarketplaceCredentialRepository credentialRepository,
            AuditService auditService,
            @Value("${seller.compliance.circuit-breaker.threshold:5}") int circuitBreakerThreshold,
            @Value("${seller.compliance.circuit-breaker.cooldown-seconds:60}") long circuitCooldownSeconds,
            @Value("${seller.compliance.allow-fake-marketplace:false}") boolean allowFakeMarketplace) {
        this.storeRepository = storeRepository;
        this.credentialRepository = credentialRepository;
        this.auditService = auditService;
        this.circuitBreakerThreshold = circuitBreakerThreshold > 0 ? circuitBreakerThreshold : DEFAULT_CIRCUIT_BREAKER_THRESHOLD;
        this.circuitCooldownSeconds = circuitCooldownSeconds > 0 ? circuitCooldownSeconds : DEFAULT_CIRCUIT_COOLDOWN_SECONDS;
        this.allowFakeMarketplace = allowFakeMarketplace;

        initMarketplaceComplianceRegistry();
    }

    private void initMarketplaceComplianceRegistry() {
        // Officially supported connectors with compliant API integrations
        marketplaceComplianceMap.put(Marketplace.MERCADO_LIVRE, ComplianceStatus.COMPLIANT);
        marketplaceComplianceMap.put(Marketplace.SHOPEE, ComplianceStatus.COMPLIANT);
        marketplaceComplianceMap.put(Marketplace.AMAZON, ComplianceStatus.COMPLIANT);

        // Channels without formal certified partnership or official production onboarding
        // strictly designated UNVERIFIED_COMPLIANCE (Fail Closed)
        marketplaceComplianceMap.put(Marketplace.TIKTOK_SHOP, ComplianceStatus.UNVERIFIED_COMPLIANCE);
        marketplaceComplianceMap.put(Marketplace.MAGALU, ComplianceStatus.UNVERIFIED_COMPLIANCE);
        marketplaceComplianceMap.put(Marketplace.SHEIN, ComplianceStatus.UNVERIFIED_COMPLIANCE);

        if (allowFakeMarketplace) {
            marketplaceComplianceMap.put(Marketplace.OUTRO, ComplianceStatus.COMPLIANT);
        } else {
            marketplaceComplianceMap.put(Marketplace.OUTRO, ComplianceStatus.UNVERIFIED_COMPLIANCE);
        }
    }

    /**
     * Evaluates whether an operation is permitted under compliance, security, and operational safety rules.
     */
    public ComplianceCheckResult checkOperation(
            UUID tenantId,
            UUID storeId,
            Marketplace marketplace,
            OperationRiskLevel riskLevel,
            boolean humanApproved,
            String actorId) {

        if (tenantId == null || marketplace == null) {
            return failClosed(tenantId, storeId, marketplace, actorId,
                    "INVALID_INPUT", "TenantId and Marketplace must not be null.");
        }

        // 1. Channel compliance status check
        ComplianceStatus status = getComplianceStatus(marketplace);
        if (status == ComplianceStatus.UNVERIFIED_COMPLIANCE || status == ComplianceStatus.NON_COMPLIANT) {
            return failClosed(tenantId, storeId, marketplace, actorId,
                    "UNVERIFIED_MARKETPLACE_BLOCKED",
                    "Marketplace " + marketplace + " compliance status is " + status +
                            ". Operations are blocked in production under FAIL CLOSED policy.");
        }

        // 2. Store verification and multi-tenant isolation
        if (storeId != null) {
            Optional<Store> storeOpt = storeRepository.findByIdAndTenantId(storeId, tenantId);
            if (storeOpt.isEmpty()) {
                return failClosed(tenantId, storeId, marketplace, actorId,
                        "STORE_NOT_FOUND_OR_CROSS_TENANT",
                        "Store not found or does not belong to tenant " + tenantId);
            }

            Store store = storeOpt.get();
            if (store.getStatus() != StoreStatus.ACTIVE) {
                return failClosed(tenantId, storeId, marketplace, actorId,
                        "STORE_INACTIVE",
                        "Store " + store.getInternalName() + " is marked as INACTIVE.");
            }

            if (store.getConnectionStatus() != ConnectionStatus.CONNECTED) {
                return failClosed(tenantId, storeId, marketplace, actorId,
                        "STORE_CONNECTION_INVALID",
                        "Store connection status is " + store.getConnectionStatus() + ". Operations cannot proceed.");
            }

            // 3. Circuit breaker check
            if (isCircuitOpen(storeId)) {
                Instant openUntil = circuitOpenUntilByStore.get(storeId);
                long remaining = openUntil != null ? Math.max(1, Duration.between(Instant.now(), openUntil).toSeconds()) : circuitCooldownSeconds;
                return failClosed(tenantId, storeId, marketplace, actorId,
                        "CIRCUIT_BREAKER_OPEN",
                        "Circuit breaker is OPEN due to repeated errors. Integration paused for cooldown. Remaining: " + remaining + "s");
            }

            // 4. Rate limit (HTTP 429) cooldown check
            if (isRateLimited(storeId)) {
                Instant cooldownUntil = rateLimitCooldownUntilByStore.get(storeId);
                long remaining = cooldownUntil != null ? Math.max(1, Duration.between(Instant.now(), cooldownUntil).toSeconds()) : DEFAULT_429_COOLDOWN_SECONDS;
                return ComplianceCheckResult.rateLimited(remaining,
                        "Store is currently in HTTP 429 rate limit cooldown. Retry after " + remaining + "s");
            }

            // 5. Credential and token expiration check
            Optional<MarketplaceCredential> credOpt = credentialRepository.findByTenantIdAndStoreId(tenantId, storeId);
            if (credOpt.isEmpty() || "REVOKED".equalsIgnoreCase(credOpt.get().getStatus())) {
                return failClosed(tenantId, storeId, marketplace, actorId,
                        "CREDENTIAL_REVOKED",
                        "Marketplace credentials have been revoked or not configured for store " + storeId);
            }

            MarketplaceCredential credential = credOpt.get();
            if (credential.getExpiresAt() != null && credential.getExpiresAt().isBefore(OffsetDateTime.now())) {
                return failClosed(tenantId, storeId, marketplace, actorId,
                        "TOKEN_EXPIRED",
                        "Marketplace access token expired at " + credential.getExpiresAt() + ". Token renewal required.");
            }
        }

        // 6. Mandatory Human-in-the-Loop approval for mutable critical actions
        if (riskLevel == OperationRiskLevel.MUTABLE_CRITICAL && !humanApproved) {
            auditService.recordEvent(
                    tenantId,
                    actorId != null ? actorId : "SYSTEM",
                    "COMPLIANCE_MUTABLE_ACTION_REJECTED",
                    "MarketplaceOperation",
                    storeId != null ? storeId.toString() : marketplace.name(),
                    "Mutable critical action blocked: missing mandatory human approval.",
                    AuditResult.DENIED
            );
            return ComplianceCheckResult.humanApprovalRequired(
                    "Mutable critical operations (e.g. price change, listing publish, stock modification) mandatorily require explicit human-in-the-loop approval.");
        }

        // All checks passed
        return ComplianceCheckResult.allowed("Operation verified and authorized by Marketplace Compliance Guard.");
    }

    /**
     * Enforces compliance check, throwing ComplianceViolationException if denied.
     */
    public void enforceOperation(
            UUID tenantId,
            UUID storeId,
            Marketplace marketplace,
            OperationRiskLevel riskLevel,
            boolean humanApproved,
            String actorId) {

        ComplianceCheckResult result = checkOperation(tenantId, storeId, marketplace, riskLevel, humanApproved, actorId);
        if (!result.allowed()) {
            HttpStatus status = HttpStatus.FORBIDDEN;
            if ("RATE_LIMIT_COOLDOWN_ACTIVE".equals(result.code())) {
                status = HttpStatus.TOO_MANY_REQUESTS;
            } else if ("STORE_NOT_FOUND_OR_CROSS_TENANT".equals(result.code())) {
                status = HttpStatus.NOT_FOUND;
            }
            throw new ComplianceViolationException(result.code(), result.reason(), status, result.retryAfterSeconds());
        }
    }

    /**
     * Records a failure response from a marketplace API to track circuit breaker and rate limiting.
     */
    public void recordFailure(UUID storeId, int httpStatus) {
        if (storeId == null) {
            return;
        }

        if (httpStatus == 429) {
            recordRateLimit429(storeId, DEFAULT_429_COOLDOWN_SECONDS);
            return;
        }

        if (httpStatus == 401 || httpStatus == 403 || httpStatus >= 500) {
            int failures = consecutiveFailuresByStore.compute(storeId, (k, v) -> (v == null ? 0 : v) + 1);
            log.warn("ComplianceGuard: Store {} consecutive failure count = {} (HTTP {})", storeId, failures, httpStatus);

            if (failures >= circuitBreakerThreshold) {
                Instant openUntil = Instant.now().plusSeconds(circuitCooldownSeconds);
                circuitOpenUntilByStore.put(storeId, openUntil);
                log.error("ComplianceGuard: CIRCUIT BREAKER OPENED for store {} until {} after {} consecutive failures.",
                        storeId, openUntil, failures);
            }
        }
    }

    /**
     * Records an HTTP 429 response with a specified or default retry-after duration.
     */
    public void recordRateLimit429(UUID storeId, long retryAfterSeconds) {
        if (storeId == null) {
            return;
        }
        long duration = retryAfterSeconds > 0 ? retryAfterSeconds : DEFAULT_429_COOLDOWN_SECONDS;
        Instant cooldownUntil = Instant.now().plusSeconds(duration);
        rateLimitCooldownUntilByStore.put(storeId, cooldownUntil);
        log.warn("ComplianceGuard: Store {} entered HTTP 429 cooldown for {} seconds until {}", storeId, duration, cooldownUntil);
    }

    /**
     * Resets failure counter upon a successful marketplace API interaction.
     */
    public void recordSuccess(UUID storeId) {
        if (storeId == null) {
            return;
        }
        consecutiveFailuresByStore.remove(storeId);
    }

    /**
     * Manually resets circuit breaker and cooldown states for a store.
     */
    public void resetCircuit(UUID storeId) {
        if (storeId == null) {
            return;
        }
        consecutiveFailuresByStore.remove(storeId);
        circuitOpenUntilByStore.remove(storeId);
        rateLimitCooldownUntilByStore.remove(storeId);
        log.info("ComplianceGuard: Circuit breaker and rate limits manually reset for store {}", storeId);
    }

    public boolean isCircuitOpen(UUID storeId) {
        if (storeId == null) {
            return false;
        }
        Instant openUntil = circuitOpenUntilByStore.get(storeId);
        if (openUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(openUntil)) {
            circuitOpenUntilByStore.remove(storeId);
            consecutiveFailuresByStore.remove(storeId);
            return false;
        }
        return true;
    }

    public boolean isRateLimited(UUID storeId) {
        if (storeId == null) {
            return false;
        }
        Instant cooldownUntil = rateLimitCooldownUntilByStore.get(storeId);
        if (cooldownUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(cooldownUntil)) {
            rateLimitCooldownUntilByStore.remove(storeId);
            return false;
        }
        return true;
    }

    public ComplianceStatus getComplianceStatus(Marketplace marketplace) {
        if (marketplace == null) {
            return ComplianceStatus.UNVERIFIED_COMPLIANCE;
        }
        return marketplaceComplianceMap.getOrDefault(marketplace, ComplianceStatus.UNVERIFIED_COMPLIANCE);
    }

    private ComplianceCheckResult failClosed(
            UUID tenantId,
            UUID storeId,
            Marketplace marketplace,
            String actorId,
            String code,
            String reason) {

        log.warn("ComplianceGuard FAIL_CLOSED: code={}, tenant={}, store={}, marketplace={}, reason={}",
                code, tenantId, storeId, marketplace, reason);

        if (tenantId != null) {
            try {
                auditService.recordEvent(
                        tenantId,
                        actorId != null ? actorId : "SYSTEM",
                        "COMPLIANCE_BLOCKED",
                        "MarketplaceOperation",
                        storeId != null ? storeId.toString() : (marketplace != null ? marketplace.name() : "UNKNOWN"),
                        "Blocked by Compliance Guard [" + code + "]: " + reason,
                        AuditResult.DENIED
                );
            } catch (Exception e) {
                log.warn("Failed to persist compliance audit event: {}", e.getMessage());
            }
        }

        return ComplianceCheckResult.denied(code, reason);
    }
}
