package com.sellerintelligence.modules.marketplaceconnection.shopee;

import com.sellerintelligence.core.correlation.CorrelationIdFilter;
import com.sellerintelligence.core.security.AuthenticatedUser;
import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.marketplaceconnection.dto.ConnectionSummaryResponse;
import com.sellerintelligence.modules.marketplaceconnection.dto.InitiateAuthResponse;
import com.sellerintelligence.modules.marketplaceconnection.shopee.dto.ShopeeCallbackRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST tenant-scoped para gestão de conexões oficiais com a Shopee Open Platform v2
 * e recepção autenticada de webhooks (Push Notifications).
 */
@RestController
@RequestMapping("/api/v1/integrations/shopee")
@Tag(name = "Shopee Open Platform", description = "Conexão oficial Shopee, HMAC-SHA256, rotação de tokens e webhooks")
public class ShopeeConnectionController {

    private final ShopeeOpenPlatformService shopeeService;

    public ShopeeConnectionController(ShopeeOpenPlatformService shopeeService) {
        this.shopeeService = shopeeService;
    }

    @PostMapping("/authorize")
    @Operation(summary = "Inicia o fluxo de autorização da Shopee com assinatura HMAC-SHA256")
    public ResponseEntity<InitiateAuthResponse> initiateAuthorization(
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = user != null ? user.getUserId() : UUID.randomUUID();
        String ipAddress = extractClientIp(request);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);

        InitiateAuthResponse response = shopeeService.initiateAuthorization(tenantId, userId, ipAddress, correlationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback")
    @Operation(summary = "Processa o callback da Shopee com code e shop_id, cifrando credenciais com AES-256-GCM")
    public ResponseEntity<ConnectionSummaryResponse> handleCallback(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ShopeeCallbackRequest callbackRequest,
            HttpServletRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = user != null ? user.getUserId() : UUID.randomUUID();
        String ipAddress = extractClientIp(request);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);

        ConnectionSummaryResponse response = shopeeService.handleCallback(
                tenantId,
                userId,
                callbackRequest,
                ipAddress,
                correlationId
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/connections/{connectionId}/refresh")
    @Operation(summary = "Força a renovação do token da Shopee sob lock atômico sem duplicações concorrentes")
    public ResponseEntity<ConnectionSummaryResponse> refreshToken(
            @PathVariable UUID connectionId,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = user != null ? user.getUserId() : null;
        String ipAddress = extractClientIp(request);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);

        ConnectionSummaryResponse response = shopeeService.refreshToken(
                tenantId,
                connectionId,
                userId,
                ipAddress,
                correlationId
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/connections/{connectionId}/revoke")
    @Operation(summary = "Revoga localmente as credenciais e anula tokens da conexão Shopee")
    public ResponseEntity<ConnectionSummaryResponse> revokeConnection(
            @PathVariable UUID connectionId,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = user != null ? user.getUserId() : null;
        String ipAddress = extractClientIp(request);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);

        ConnectionSummaryResponse response = shopeeService.revokeConnection(
                tenantId,
                connectionId,
                userId,
                ipAddress,
                correlationId
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/connections")
    @Operation(summary = "Lista conexões ativas e revogadas com a Shopee para o tenant")
    public ResponseEntity<List<ConnectionSummaryResponse>> listConnections() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(shopeeService.listConnections(tenantId));
    }

    @GetMapping("/connections/{connectionId}")
    @Operation(summary = "Consulta detalhes resumidos de uma conexão Shopee específica")
    public ResponseEntity<ConnectionSummaryResponse> getConnection(@PathVariable UUID connectionId) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(shopeeService.getConnection(tenantId, connectionId));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Receptor oficial de Push Notifications (Webhooks) da Shopee com validação HMAC-SHA256")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody String rawBody,
            HttpServletRequest request
    ) {
        String requestUrl = request.getRequestURL().toString();
        shopeeService.handleWebhook(authorizationHeader, rawBody, requestUrl);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success"));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
