package br.com.sellerintelligence.connector.web;

import br.com.sellerintelligence.connector.model.ConnectorCapability;
import br.com.sellerintelligence.connector.model.OAuthAuthorizationUrl;
import br.com.sellerintelligence.connector.registry.MarketplaceConnectorRegistry;
import br.com.sellerintelligence.connector.service.MarketplaceConnectionService;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserService;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreService;
import br.com.sellerintelligence.web.dto.StoreResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketplaces")
public class MarketplaceOAuthController {

    private final MarketplaceConnectionService connectionService;
    private final MarketplaceConnectorRegistry connectorRegistry;
    private final StoreService storeService;
    private final UserService userService;

    public MarketplaceOAuthController(
            MarketplaceConnectionService connectionService,
            MarketplaceConnectorRegistry connectorRegistry,
            StoreService storeService,
            UserService userService) {
        this.connectionService = connectionService;
        this.connectorRegistry = connectorRegistry;
        this.storeService = storeService;
        this.userService = userService;
    }

    @GetMapping("/capabilities")
    public ResponseEntity<List<MarketplaceCapabilitiesResponse>> getAllCapabilities() {
        Map<br.com.sellerintelligence.tenancy.store.Marketplace, List<ConnectorCapability>> map = connectorRegistry.getAllCapabilities();
        List<MarketplaceCapabilitiesResponse> responses = new ArrayList<>();
        map.forEach((mkt, caps) -> responses.add(new MarketplaceCapabilitiesResponse(mkt, caps)));
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/capabilities/{marketplace}")
    public ResponseEntity<MarketplaceCapabilitiesResponse> getCapabilities(
            @PathVariable br.com.sellerintelligence.tenancy.store.Marketplace marketplace) {
        List<ConnectorCapability> capabilities = connectionService.getMarketplaceCapabilities(marketplace);
        return ResponseEntity.ok(new MarketplaceCapabilitiesResponse(marketplace, capabilities));
    }

    @PostMapping("/connect/authorize")
    public ResponseEntity<OAuthAuthorizationUrl> initiateOAuth(
            @Valid @RequestBody InitiateOAuthRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        OAuthAuthorizationUrl authUrl = connectionService.initiateOAuthFlow(
                tenantId,
                request.storeId(),
                request.redirectUri(),
                actor
        );

        return ResponseEntity.ok(authUrl);
    }

    @PostMapping("/connect/callback")
    public ResponseEntity<StoreResponse> handleOAuthCallback(
            @Valid @RequestBody OAuthCallbackRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        Store connectedStore = connectionService.handleOAuthCallback(
                request.state(),
                request.code(),
                tenantId,
                actor
        );

        return ResponseEntity.ok(StoreResponse.from(connectedStore));
    }

    @PostMapping("/connect/{storeId}/disconnect")
    public ResponseEntity<StoreResponse> disconnectStore(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        Store store = connectionService.disconnectStore(tenantId, storeId, actor);
        return ResponseEntity.ok(StoreResponse.from(store));
    }

    @PostMapping("/connect/{storeId}/refresh")
    public ResponseEntity<Void> refreshStoreToken(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        connectionService.refreshStoreToken(tenantId, storeId, actor);
        return ResponseEntity.ok().build();
    }
}
