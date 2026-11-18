package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import com.sellerintelligence.core.correlation.CorrelationIdFilter;
import com.sellerintelligence.core.security.AuthenticatedUser;
import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.ConnectionSummaryResponse;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.InitiateAuthResponse;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.OAuthCallbackRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST tenant-scoped para gestão de conexões oficiais OAuth 2.0 com o Mercado Livre.
 */
@RestController
@RequestMapping("/api/v1/integrations/mercadolivre")
@Tag(name = "Mercado Livre OAuth", description = "Conexão oficial, PKCE, callback, refresh e revogação")
public class MercadoLivreOAuthController {

    private final MercadoLivreOAuthService oauthService;

    public MercadoLivreOAuthController(MercadoLivreOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    @PostMapping("/authorize")
    @Operation(summary = "Inicia o fluxo OAuth 2.0 do Mercado Livre com PKCE S256 e state anti-CSRF")
    public ResponseEntity<InitiateAuthResponse> initiateAuthorization(
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = user != null ? user.getUserId() : UUID.randomUUID();
        String ipAddress = extractClientIp(request);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);

        InitiateAuthResponse response = oauthService.initiateAuthorization(tenantId, userId, ipAddress, correlationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback")
    @Operation(summary = "Processa o callback de autorização, valida o state anti-CSRF e grava as credenciais cifradas")
    public ResponseEntity<ConnectionSummaryResponse> handleCallback(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody OAuthCallbackRequest callbackRequest,
            HttpServletRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = user != null ? user.getUserId() : UUID.randomUUID();
        String ipAddress = extractClientIp(request);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);

        ConnectionSummaryResponse response = oauthService.handleCallback(
                tenantId,
                userId,
                callbackRequest,
                ipAddress,
                correlationId
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/connections/{connectionId}/refresh")
    @Operation(summary = "Força a renovação do token sob lock exclusivo sem duplicações concorrentes")
    public ResponseEntity<ConnectionSummaryResponse> refreshToken(
            @PathVariable UUID connectionId,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = user != null ? user.getUserId() : null;
        String ipAddress = extractClientIp(request);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);

        ConnectionSummaryResponse response = oauthService.refreshToken(
                tenantId,
                connectionId,
                userId,
                ipAddress,
                correlationId
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/connections/{connectionId}/revoke")
    @Operation(summary = "Revoga localmente as credenciais e anula tokens da conexão")
    public ResponseEntity<ConnectionSummaryResponse> revokeConnection(
            @PathVariable UUID connectionId,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = user != null ? user.getUserId() : null;
        String ipAddress = extractClientIp(request);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);

        ConnectionSummaryResponse response = oauthService.revokeConnection(
                tenantId,
                connectionId,
                userId,
                ipAddress,
                correlationId
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/connections")
    @Operation(summary = "Lista conexões ativas e revogadas com o Mercado Livre para o tenant")
    public ResponseEntity<List<ConnectionSummaryResponse>> listConnections() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(oauthService.listConnections(tenantId));
    }

    @GetMapping("/connections/{connectionId}")
    @Operation(summary = "Consulta detalhes resumidos de uma conexão específica")
    public ResponseEntity<ConnectionSummaryResponse> getConnection(@PathVariable UUID connectionId) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(oauthService.getConnection(tenantId, connectionId));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
