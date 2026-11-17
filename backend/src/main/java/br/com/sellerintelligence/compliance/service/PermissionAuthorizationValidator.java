package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.compliance.domain.ComplianceCheckResult;
import br.com.sellerintelligence.connector.model.MarketplaceCredential;
import br.com.sellerintelligence.connector.model.MarketplaceCredentialRepository;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service continuously validating permissions, OAuth scopes, and token validity for marketplace stores.
 */
@Service
public class PermissionAuthorizationValidator {

    private static final Logger log = LoggerFactory.getLogger(PermissionAuthorizationValidator.class);

    private final MarketplaceCredentialRepository credentialRepository;

    public PermissionAuthorizationValidator(MarketplaceCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Transactional(readOnly = true)
    public ComplianceCheckResult validateStorePermissions(UUID tenantId, UUID storeId, Set<String> requiredScopes) {
        Optional<MarketplaceCredential> credOpt = credentialRepository.findByTenantIdAndStoreId(tenantId, storeId);
        if (credOpt.isEmpty()) {
            return ComplianceCheckResult.denied("CREDENTIALS_MISSING", "Store has no configured marketplace credentials");
        }

        MarketplaceCredential cred = credOpt.get();

        // 1. Revocation check
        if ("REVOKED".equalsIgnoreCase(cred.getStatus())) {
            return ComplianceCheckResult.denied("CREDENTIAL_REVOKED", "Store integration has been revoked");
        }

        // 2. Expiration check
        if (cred.getExpiresAt() != null && cred.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return ComplianceCheckResult.denied("TOKEN_EXPIRED", "Store OAuth token is expired");
        }

        // 3. Scopes check
        if (requiredScopes != null && !requiredScopes.isEmpty()) {
            String consented = cred.getScopesConsented() != null ? cred.getScopesConsented().toLowerCase() : "";
            for (String required : requiredScopes) {
                if (!consented.contains(required.toLowerCase())) {
                    log.warn("PermissionValidator: Missing required scope '{}' for store {} on {}",
                            required, storeId, cred.getMarketplace());
                    return ComplianceCheckResult.denied("INSUFFICIENT_SCOPES",
                            "Store lacks required OAuth scope: " + required);
                }
            }
        }

        return ComplianceCheckResult.allowed("Store permissions and scopes verified successfully");
    }
}
