package br.com.sellerintelligence.connector.service;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.connector.crypto.TokenCryptoService;
import br.com.sellerintelligence.connector.model.*;
import br.com.sellerintelligence.connector.ports.MarketplaceConnector;
import br.com.sellerintelligence.connector.registry.MarketplaceConnectorRegistry;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.TenantAccessDeniedException;
import br.com.sellerintelligence.tenancy.scope.ScopeAuthorizationService;
import br.com.sellerintelligence.tenancy.store.ConnectionStatus;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MarketplaceConnectionService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceConnectionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MarketplaceConnectorRegistry connectorRegistry;
    private final MarketplaceCredentialRepository credentialRepository;
    private final OAuthStateRepository oauthStateRepository;
    private final StoreRepository storeRepository;
    private final MembershipRepository membershipRepository;
    private final ScopeAuthorizationService scopeAuthorizationService;
    private final TokenCryptoService tokenCryptoService;
    private final AuditService auditService;

    public MarketplaceConnectionService(
            MarketplaceConnectorRegistry connectorRegistry,
            MarketplaceCredentialRepository credentialRepository,
            OAuthStateRepository oauthStateRepository,
            StoreRepository storeRepository,
            MembershipRepository membershipRepository,
            ScopeAuthorizationService scopeAuthorizationService,
            TokenCryptoService tokenCryptoService,
            AuditService auditService) {
        this.connectorRegistry = connectorRegistry;
        this.credentialRepository = credentialRepository;
        this.oauthStateRepository = oauthStateRepository;
        this.storeRepository = storeRepository;
        this.membershipRepository = membershipRepository;
        this.scopeAuthorizationService = scopeAuthorizationService;
        this.tokenCryptoService = tokenCryptoService;
        this.auditService = auditService;
    }

    @Transactional
    public OAuthAuthorizationUrl initiateOAuthFlow(UUID tenantId, UUID storeId, String redirectUri, User actor) {
        Membership membership = verifyManagementPermission(tenantId, actor.getId());

        Store store = storeRepository.findByIdAndTenantIdWithCompany(storeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Store not found or does not belong to this organization"));

        MarketplaceConnector connector = connectorRegistry.getConnector(store.getMarketplace());

        String stateToken = generateSecureState();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(15);

        OAuthState oauthState = new OAuthState(
                stateToken,
                tenantId,
                store.getId(),
                actor.getId(),
                store.getMarketplace(),
                redirectUri,
                expiresAt
        );
        oauthStateRepository.save(oauthState);

        auditService.recordEvent(
                tenantId,
                actor.getId().toString(),
                "OAUTH_INITIATED",
                "Store",
                store.getId().toString(),
                "Initiated OAuth flow for store " + store.getInternalName() + " on " + store.getMarketplace(),
                AuditResult.SUCCESS
        );

        return connector.generateAuthorizationUrl(stateToken, redirectUri);
    }

    @Transactional
    public Store handleOAuthCallback(String state, String code, UUID tenantId, User actor) {
        verifyManagementPermission(tenantId, actor.getId());

        OAuthState oauthState = oauthStateRepository.findByState(state)
                .orElseThrow(() -> new BusinessException("INVALID_OAUTH_STATE", "Invalid or missing OAuth state token", HttpStatus.BAD_REQUEST));

        if (oauthState.isUsed()) {
            throw new BusinessException("OAUTH_STATE_USED", "OAuth state token has already been used", HttpStatus.BAD_REQUEST);
        }

        if (oauthState.isExpired()) {
            throw new BusinessException("OAUTH_STATE_EXPIRED", "OAuth state token has expired", HttpStatus.BAD_REQUEST);
        }

        if (!oauthState.getTenantId().equals(tenantId)) {
            auditService.recordEvent(
                    tenantId,
                    actor.getId().toString(),
                    "ACCESS_DENIED",
                    "OAuthState",
                    oauthState.getId().toString(),
                    "Attempted to complete OAuth callback for another tenant's state",
                    AuditResult.DENIED
            );
            throw new TenantAccessDeniedException("OAuth state does not match the current organization");
        }

        oauthState.setUsed(true);
        oauthStateRepository.save(oauthState);

        final Store store = storeRepository.findByIdAndTenantIdWithCompany(oauthState.getStoreId(), tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Store associated with OAuth state not found"));

        MarketplaceConnector connector = connectorRegistry.getConnector(store.getMarketplace());
        OAuthTokenResult tokenResult = connector.exchangeAuthorizationCode(code, oauthState.getRedirectUri());

        String encryptedAccess = tokenCryptoService.encrypt(tokenResult.accessToken());
        String encryptedRefresh = tokenCryptoService.encrypt(tokenResult.refreshToken());

        MarketplaceCredential credential = credentialRepository.findByStoreId(store.getId())
                .orElseGet(() -> new MarketplaceCredential(
                        tenantId,
                        store,
                        store.getMarketplace(),
                        encryptedAccess,
                        encryptedRefresh,
                        tokenResult.externalAccountId(),
                        tokenResult.scopesConsented(),
                        tokenResult.expiresAt(),
                        tokenResult.refreshExpiresAt(),
                        tokenResult.environment()
                ));

        credential.setEncryptedAccessToken(encryptedAccess);
        credential.setEncryptedRefreshToken(encryptedRefresh);
        credential.setExternalAccountId(tokenResult.externalAccountId());
        credential.setScopesConsented(tokenResult.scopesConsented());
        credential.setExpiresAt(tokenResult.expiresAt());
        credential.setRefreshExpiresAt(tokenResult.refreshExpiresAt());
        credential.setEnvironment(tokenResult.environment());
        credential.setStatus("CONNECTED");
        credential.setLastValidatedAt(OffsetDateTime.now());
        credentialRepository.save(credential);

        store.setConnectionStatus(ConnectionStatus.CONNECTED);
        store.setExternalAccountId(tokenResult.externalAccountId());
        Store updatedStore = storeRepository.save(store);

        auditService.recordEvent(
                tenantId,
                actor.getId().toString(),
                "STORE_CONNECTED",
                "Store",
                updatedStore.getId().toString(),
                "Connected store " + updatedStore.getInternalName() + " (Account: " + tokenResult.externalAccountId() + ", Env: " + tokenResult.environment() + ")",
                AuditResult.SUCCESS
        );

        return updatedStore;
    }

    @Transactional
    public Store disconnectStore(UUID tenantId, UUID storeId, User actor) {
        verifyManagementPermission(tenantId, actor.getId());

        Store store = storeRepository.findByIdAndTenantIdWithCompany(storeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Store not found in organization"));

        Optional<MarketplaceCredential> credentialOpt = credentialRepository.findByStoreId(storeId);
        if (credentialOpt.isPresent()) {
            MarketplaceCredential credential = credentialOpt.get();
            try {
                MarketplaceConnector connector = connectorRegistry.getConnector(store.getMarketplace());
                String decryptedAccess = tokenCryptoService.decrypt(credential.getEncryptedAccessToken());
                connector.revokeToken(decryptedAccess, credential.getExternalAccountId());
            } catch (Exception e) {
                log.warn("Failed to revoke token externally for store {}: {}", storeId, e.getMessage());
            }

            credential.setStatus("REVOKED");
            credentialRepository.save(credential);
        }

        store.setConnectionStatus(ConnectionStatus.DISCONNECTED);
        store = storeRepository.save(store);

        auditService.recordEvent(
                tenantId,
                actor.getId().toString(),
                "STORE_DISCONNECTED",
                "Store",
                store.getId().toString(),
                "Disconnected store " + store.getInternalName(),
                AuditResult.SUCCESS
        );

        return store;
    }

    @Transactional
    public MarketplaceCredential refreshStoreToken(UUID tenantId, UUID storeId, User actor) {
        verifyManagementPermission(tenantId, actor.getId());

        Store store = storeRepository.findByIdAndTenantIdWithCompany(storeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Store not found"));

        MarketplaceCredential credential = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("NO_CREDENTIALS", "Store has no saved credentials", HttpStatus.BAD_REQUEST));

        String decryptedRefresh = tokenCryptoService.decrypt(credential.getEncryptedRefreshToken());
        if (decryptedRefresh == null || decryptedRefresh.isBlank()) {
            throw new BusinessException("NO_REFRESH_TOKEN", "No refresh token available for this store", HttpStatus.BAD_REQUEST);
        }

        MarketplaceConnector connector = connectorRegistry.getConnector(store.getMarketplace());
        OAuthTokenResult result = connector.refreshAccessToken(decryptedRefresh);

        credential.setEncryptedAccessToken(tokenCryptoService.encrypt(result.accessToken()));
        if (result.refreshToken() != null && !result.refreshToken().isBlank()) {
            credential.setEncryptedRefreshToken(tokenCryptoService.encrypt(result.refreshToken()));
        }
        credential.setExpiresAt(result.expiresAt());
        credential.setLastValidatedAt(OffsetDateTime.now());
        credential.setStatus("CONNECTED");

        return credentialRepository.save(credential);
    }

    @Transactional(readOnly = true)
    public List<ConnectorCapability> getMarketplaceCapabilities(Marketplace marketplace) {
        MarketplaceConnector connector = connectorRegistry.getConnector(marketplace);
        return connector.getCapabilities();
    }

    private Membership verifyManagementPermission(UUID tenantId, UUID userId) {
        Membership membership = membershipRepository.findByOrganizationIdAndUserId(tenantId, userId)
                .filter(m -> m.getStatus().name().equals("ACTIVE"))
                .orElseThrow(() -> new TenantAccessDeniedException("Access denied: user is not an active member of this organization"));

        if (!membership.getRole().canManageMembers()) {
            throw new TenantAccessDeniedException("Insufficient permissions: Only Owner or Admin can manage marketplace connections");
        }

        return membership;
    }

    private String generateSecureState() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
