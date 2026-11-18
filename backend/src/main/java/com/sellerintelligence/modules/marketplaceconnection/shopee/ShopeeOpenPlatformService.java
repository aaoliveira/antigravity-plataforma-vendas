package com.sellerintelligence.modules.marketplaceconnection.shopee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.marketplaceconnection.DistributedConnectionLock;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnection;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnectionRepository;
import com.sellerintelligence.modules.marketplaceconnection.dto.ConnectionSummaryResponse;
import com.sellerintelligence.modules.marketplaceconnection.dto.InitiateAuthResponse;
import com.sellerintelligence.modules.marketplaceconnection.exception.ShopeeOAuthException;
import com.sellerintelligence.modules.marketplaceconnection.exception.ShopeeWebhookSignatureException;
import com.sellerintelligence.modules.marketplaceconnection.security.TokenEncryptionService;
import com.sellerintelligence.modules.marketplaceconnection.shopee.dto.ShopeeCallbackRequest;
import com.sellerintelligence.modules.marketplaceconnection.shopee.dto.ShopeeWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de orquestração da integração oficial com a Shopee Open Platform v2.
 * Gerencia autenticação HMAC-SHA256, troca de tokens, cifragem AES-256-GCM,
 * rotação de refresh token com lock, revogação e processamento seguro de push webhooks.
 */
@Service
public class ShopeeOpenPlatformService {

    private static final Logger log = LoggerFactory.getLogger(ShopeeOpenPlatformService.class);
    public static final String MARKETPLACE_ID = "SHOPEE";
    private static final String DEFAULT_SCOPES = "Shop Level (OpenAPI v2)";

    private final ShopeeOAuthProperties properties;
    private final ShopeeOpenPlatformClient client;
    private final TokenEncryptionService encryptionService;
    private final MarketplaceConnectionRepository connectionRepository;
    private final DistributedConnectionLock connectionLock;
    private final AuditPublisher auditPublisher;
    private final ShopeeWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;

    public ShopeeOpenPlatformService(
            ShopeeOAuthProperties properties,
            ShopeeOpenPlatformClient client,
            TokenEncryptionService encryptionService,
            MarketplaceConnectionRepository connectionRepository,
            DistributedConnectionLock connectionLock,
            AuditPublisher auditPublisher,
            ShopeeWebhookVerifier webhookVerifier,
            ObjectMapper objectMapper
    ) {
        this.properties = Objects.requireNonNull(properties, "properties não pode ser nulo");
        this.client = Objects.requireNonNull(client, "client não pode ser nulo");
        this.encryptionService = Objects.requireNonNull(encryptionService, "encryptionService não pode ser nulo");
        this.connectionRepository = Objects.requireNonNull(connectionRepository, "connectionRepository não pode ser nulo");
        this.connectionLock = Objects.requireNonNull(connectionLock, "connectionLock não pode ser nulo");
        this.auditPublisher = Objects.requireNonNull(auditPublisher, "auditPublisher não pode ser nulo");
        this.webhookVerifier = Objects.requireNonNull(webhookVerifier, "webhookVerifier não pode ser nulo");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper não pode ser nulo");
    }

    /**
     * Inicia o fluxo de autorização da Shopee gerando a URL oficial com assinatura HMAC-SHA256.
     */
    public InitiateAuthResponse initiateAuthorization(UUID tenantId, UUID userId, String ipAddress, String correlationId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");

        if (!properties.isEnabled()) {
            throw new ShopeeOAuthException("shopee_disabled", "A integração com a Shopee está desativada por política de governança.");
        }

        String authUrl = client.buildAuthorizationUrl(
                properties.getPartnerId(),
                properties.getPartnerKey(),
                properties.getRedirectUrl(),
                properties.getAuthBaseUrl()
        );

        auditPublisher.publish(
                "SHOPEE_AUTH_INITIATED",
                userId,
                null,
                tenantId,
                ipAddress,
                null,
                correlationId,
                "Iniciado fluxo de autorização oficial com Shopee Open Platform v2 (HMAC-SHA256)"
        );

        return new InitiateAuthResponse(authUrl, "HMAC-SHA256", Instant.now().plusSeconds(300));
    }

    /**
     * Processa o callback de autorização da Shopee com code e shop_id,
     * realiza a troca por tokens, cifra-os com AES-256-GCM e persiste a conexão tenant-scoped.
     */
    @Transactional
    public ConnectionSummaryResponse handleCallback(
            UUID tenantId,
            UUID userId,
            ShopeeCallbackRequest request,
            String ipAddress,
            String correlationId
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(request, "request não pode ser nulo");

        if (!properties.isEnabled()) {
            throw new ShopeeOAuthException("shopee_disabled", "A integração com a Shopee está desabilitada.");
        }

        ShopeeTokenResponse tokenResponse = client.getAccessToken(
                request.code(),
                request.shopId(),
                properties.getPartnerId(),
                properties.getPartnerKey(),
                properties.getApiBaseUrl()
        );

        String encryptedAccessToken = encryptionService.encrypt(tokenResponse.accessToken());
        String encryptedRefreshToken = encryptionService.encrypt(tokenResponse.refreshToken());
        long expireInSeconds = tokenResponse.expireIn() != null ? tokenResponse.expireIn() : 14400L;
        Instant tokenExpiresAt = Instant.now().plusSeconds(expireInSeconds);

        String shopIdStr = String.valueOf(request.shopId());

        Optional<MarketplaceConnection> existingConnection = connectionRepository
                .findByTenantIdAndMarketplaceAndSellerAccountId(tenantId, MARKETPLACE_ID, shopIdStr);

        MarketplaceConnection connection;
        if (existingConnection.isPresent()) {
            connection = existingConnection.get();
            connection.setEncryptedAccessToken(encryptedAccessToken);
            connection.setEncryptedRefreshToken(encryptedRefreshToken);
            connection.setStatus("ACTIVE");
            connection.setTokenExpiresAt(tokenExpiresAt);
            connection.setAuthorizedScopes(DEFAULT_SCOPES);
        } else {
            connection = new MarketplaceConnection(
                    UUID.randomUUID(),
                    tenantId,
                    MARKETPLACE_ID,
                    shopIdStr,
                    "Shopee Shop #" + shopIdStr,
                    encryptedAccessToken,
                    encryptedRefreshToken,
                    "ACTIVE"
            );
            connection.setTokenExpiresAt(tokenExpiresAt);
            connection.setAuthorizedScopes(DEFAULT_SCOPES);
        }

        MarketplaceConnection saved = connectionRepository.save(connection);

        auditPublisher.publish(
                "SHOPEE_CALLBACK_SUCCESS",
                userId,
                null,
                tenantId,
                ipAddress,
                null,
                correlationId,
                "Conexão oficial com Shopee realizada com sucesso para a loja #" + shopIdStr + ". Tokens cifrados via AES-256-GCM."
        );

        return ConnectionSummaryResponse.fromEntity(saved);
    }

    /**
     * Renova o access_token utilizando o refresh_token de uso único sob lock atômico por conexão.
     */
    @Transactional
    public ConnectionSummaryResponse refreshToken(
            UUID tenantId,
            UUID connectionId,
            UUID userId,
            String ipAddress,
            String correlationId
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(connectionId, "connectionId não pode ser nulo");

        MarketplaceConnection connection = connectionRepository.findByIdAndTenantId(connectionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conexão Shopee não encontrada para este tenant"));

        if (!MARKETPLACE_ID.equals(connection.getMarketplace())) {
            throw new IllegalArgumentException("A conexão " + connectionId + " não pertence ao marketplace Shopee");
        }

        if ("REVOKED".equals(connection.getStatus())) {
            throw new IllegalStateException("Não é possível renovar credenciais de uma conexão que foi revogada");
        }

        return connectionLock.executeWithLock(connectionId, () -> {
            MarketplaceConnection freshConnection = connectionRepository.findByIdAndTenantId(connectionId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conexão Shopee não encontrada"));

            Instant now = Instant.now();
            if (freshConnection.getTokenExpiresAt() != null
                    && freshConnection.getTokenExpiresAt().isAfter(now.plus(Duration.ofHours(1)))
                    && freshConnection.getLastRefreshedAt() != null
                    && Duration.between(freshConnection.getLastRefreshedAt(), now).toMinutes() < 5) {
                log.info("Token da Shopee da conexão {} já foi renovado recentemente por outra thread. Reaproveitando.", connectionId);
                return ConnectionSummaryResponse.fromEntity(freshConnection);
            }

            String plainRefreshToken = encryptionService.decrypt(freshConnection.getEncryptedRefreshToken());
            long shopId = Long.parseLong(freshConnection.getSellerAccountId());

            ShopeeTokenResponse tokenResponse = client.refreshAccessToken(
                    plainRefreshToken,
                    shopId,
                    properties.getPartnerId(),
                    properties.getPartnerKey(),
                    properties.getApiBaseUrl()
            );

            String newEncryptedAccess = encryptionService.encrypt(tokenResponse.accessToken());
            String newEncryptedRefresh = encryptionService.encrypt(tokenResponse.refreshToken());
            long expireIn = tokenResponse.expireIn() != null ? tokenResponse.expireIn() : 14400L;
            Instant newExpiresAt = Instant.now().plusSeconds(expireIn);

            freshConnection.setEncryptedAccessToken(newEncryptedAccess);
            freshConnection.setEncryptedRefreshToken(newEncryptedRefresh);
            freshConnection.setTokenExpiresAt(newExpiresAt);
            freshConnection.setLastRefreshedAt(now);

            MarketplaceConnection saved = connectionRepository.save(freshConnection);

            auditPublisher.publish(
                    "SHOPEE_TOKEN_REFRESH_SUCCESS",
                    userId,
                    null,
                    tenantId,
                    ipAddress,
                    null,
                    correlationId,
                    "Token da Shopee renovado sob lock com rotação de refresh token para loja #" + freshConnection.getSellerAccountId()
            );

            return ConnectionSummaryResponse.fromEntity(saved);
        });
    }

    /**
     * Revoga localmente uma conexão com a Shopee, anulando tokens cifrados e marcando status REVOKED.
     */
    @Transactional
    public ConnectionSummaryResponse revokeConnection(
            UUID tenantId,
            UUID connectionId,
            UUID userId,
            String ipAddress,
            String correlationId
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(connectionId, "connectionId não pode ser nulo");

        MarketplaceConnection connection = connectionRepository.findByIdAndTenantId(connectionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conexão Shopee não encontrada para este tenant"));

        if (!MARKETPLACE_ID.equals(connection.getMarketplace())) {
            throw new IllegalArgumentException("A conexão " + connectionId + " não pertence à Shopee");
        }

        connection.setEncryptedAccessToken(null);
        connection.setEncryptedRefreshToken(null);
        connection.setStatus("REVOKED");

        MarketplaceConnection saved = connectionRepository.save(connection);

        auditPublisher.publish(
                "SHOPEE_TOKEN_REVOCATION_SUCCESS",
                userId,
                null,
                tenantId,
                ipAddress,
                null,
                correlationId,
                "Conexão Shopee #" + connection.getSellerAccountId() + " revogada com sucesso."
        );

        return ConnectionSummaryResponse.fromEntity(saved);
    }

    /**
     * Processa Push Notifications (Webhooks) da Shopee com validação obrigatória de HMAC-SHA256.
     * Caso o evento seja de cancelamento de autorização (code: 2), as conexões correspondentes são revogadas.
     */
    @Transactional
    public void handleWebhook(String authorizationHeader, String rawBody, String requestUrl) {
        if (!webhookVerifier.verify(authorizationHeader, rawBody, requestUrl)) {
            throw new ShopeeWebhookSignatureException("Assinatura HMAC-SHA256 do webhook da Shopee é inválida ou foi adulterada.");
        }

        try {
            ShopeeWebhookPayload payload = objectMapper.readValue(rawBody, ShopeeWebhookPayload.class);
            if (payload == null) return;

            log.info("Webhook da Shopee recebido com sucesso: code={}, shop_id={}", payload.code(), payload.shopId());

            if (payload.isAuthorizationCanceled() && payload.shopId() != null) {
                String shopIdStr = String.valueOf(payload.shopId());
                List<MarketplaceConnection> connections = connectionRepository.findAll().stream()
                        .filter(c -> MARKETPLACE_ID.equals(c.getMarketplace()) && shopIdStr.equals(c.getSellerAccountId()))
                        .toList();

                for (MarketplaceConnection connection : connections) {
                    connection.setEncryptedAccessToken(null);
                    connection.setEncryptedRefreshToken(null);
                    connection.setStatus("REVOKED");
                    connectionRepository.save(connection);

                    auditPublisher.publish(
                            "SHOPEE_WEBHOOK_AUTHORIZATION_REVOKED",
                            null,
                            null,
                            connection.getTenantId(),
                            "Shopee-Push-Server",
                            null,
                            UUID.randomUUID().toString(),
                            "Autorização da loja Shopee #" + shopIdStr + " cancelada pelo seller via Push Notification (code: 2)"
                    );
                    log.info("Conexão Shopee {} revogada automaticamente via Push Notification (code: 2).", connection.getId());
                }
            }
        } catch (ShopeeWebhookSignatureException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao processar corpo do webhook da Shopee: {}", e.getMessage());
            throw new IllegalArgumentException("Payload do webhook da Shopee malformado ou inválido", e);
        }
    }

    /**
     * Lista todas as conexões da Shopee pertencentes ao tenant.
     */
    @Transactional(readOnly = true)
    public List<ConnectionSummaryResponse> listConnections(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        return connectionRepository.findByTenantIdAndMarketplace(tenantId, MARKETPLACE_ID)
                .stream()
                .map(ConnectionSummaryResponse::fromEntity)
                .toList();
    }

    /**
     * Consulta detalhes resumidos de uma conexão específica da Shopee.
     */
    @Transactional(readOnly = true)
    public ConnectionSummaryResponse getConnection(UUID tenantId, UUID connectionId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(connectionId, "connectionId não pode ser nulo");

        MarketplaceConnection connection = connectionRepository.findByIdAndTenantId(connectionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conexão Shopee não encontrada"));

        return ConnectionSummaryResponse.fromEntity(connection);
    }
}
