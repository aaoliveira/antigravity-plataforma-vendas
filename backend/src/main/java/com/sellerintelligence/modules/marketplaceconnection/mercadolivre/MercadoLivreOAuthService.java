package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.marketplaceconnection.DistributedConnectionLock;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnection;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnectionRepository;
import com.sellerintelligence.modules.marketplaceconnection.exception.InvalidStateCsrfException;
import com.sellerintelligence.modules.marketplaceconnection.exception.UnauthorizedScopeException;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.ConnectionSummaryResponse;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.InitiateAuthResponse;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.OAuthCallbackRequest;
import com.sellerintelligence.modules.marketplaceconnection.security.TokenEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de orquestração do ciclo de vida OAuth 2.0 do Mercado Livre com PKCE,
 * proteção anti-CSRF, cifragem de tokens via AES-256-GCM, lock para refresh concorrente
 * e auditoria estruturada sem exposição de credenciais.
 */
@Service
public class MercadoLivreOAuthService {

    private static final Logger log = LoggerFactory.getLogger(MercadoLivreOAuthService.class);
    public static final String MARKETPLACE_ID = "MERCADO_LIVRE";

    private final MeliOAuthProperties properties;
    private final MeliOAuthClient oauthClient;
    private final OAuthStateManager stateManager;
    private final TokenEncryptionService encryptionService;
    private final MarketplaceConnectionRepository connectionRepository;
    private final DistributedConnectionLock connectionLock;
    private final AuditPublisher auditPublisher;

    public MercadoLivreOAuthService(
            MeliOAuthProperties properties,
            MeliOAuthClient oauthClient,
            OAuthStateManager stateManager,
            TokenEncryptionService encryptionService,
            MarketplaceConnectionRepository connectionRepository,
            DistributedConnectionLock connectionLock,
            AuditPublisher auditPublisher
    ) {
        this.properties = properties;
        this.oauthClient = oauthClient;
        this.stateManager = stateManager;
        this.encryptionService = encryptionService;
        this.connectionRepository = connectionRepository;
        this.connectionLock = connectionLock;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Inicia o fluxo de autorização OAuth gerando o par PKCE e o token state anti-CSRF.
     */
    public InitiateAuthResponse initiateAuthorization(UUID tenantId, UUID userId, String ipAddress, String correlationId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");

        validateConfiguredScopes(properties.getScopes());

        PkceChallenge challenge = PkceUtil.generateChallenge();
        OAuthState state = stateManager.generateAndStoreState(tenantId, userId, challenge.codeVerifier());

        String authUrl = oauthClient.buildAuthorizationUrl(state.stateToken(), challenge.codeChallenge());

        auditPublisher.publish(
                "OAUTH_AUTHORIZE_INITIATED",
                userId,
                null,
                tenantId,
                ipAddress,
                null,
                correlationId,
                "Iniciado fluxo OAuth 2.0 com Mercado Livre utilizando PKCE S256 e state anti-CSRF."
        );

        return new InitiateAuthResponse(authUrl, state.stateToken(), state.expiresAt());
    }

    /**
     * Processa o callback de autorização, valida o state anti-CSRF, troca o code por tokens,
     * cifra-os com AES-256-GCM e persiste a conexão vinculada ao tenant ativo.
     */
    @Transactional
    public ConnectionSummaryResponse handleCallback(
            UUID tenantId,
            UUID userId,
            OAuthCallbackRequest request,
            String ipAddress,
            String correlationId
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(request, "request não pode ser nulo");

        // 1. Validação mandatória e consumo do State Anti-CSRF
        OAuthState state;
        try {
            state = stateManager.consumeAndValidateState(request.state(), tenantId);
        } catch (InvalidStateCsrfException e) {
            auditPublisher.publish(
                    "OAUTH_CALLBACK_INVALID_STATE",
                    userId,
                    null,
                    tenantId,
                    ipAddress,
                    null,
                    correlationId,
                    "Falha de validação do state anti-CSRF no callback do Mercado Livre: " + e.getMessage()
            );
            throw e;
        }

        // 2. Troca do Authorization Code temporário por tokens via PKCE
        MeliTokenResponse tokenResponse = oauthClient.exchangeAuthorizationCode(
                request.code(),
                state.codeVerifier(),
                request.redirectUri()
        );

        // 3. Cifragem imediata dos tokens com AES-256-GCM (KMS Pattern)
        String encryptedAccessToken = encryptionService.encrypt(tokenResponse.accessToken());
        String encryptedRefreshToken = encryptionService.encrypt(tokenResponse.refreshToken());

        String sellerAccountId = tokenResponse.getFormattedUserId();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(tokenResponse.expiresIn());

        // 4. Criação ou atualização idempotente da conexão para o tenant
        Optional<MarketplaceConnection> existingConnection = connectionRepository
                .findByTenantIdAndMarketplaceAndSellerAccountId(tenantId, MARKETPLACE_ID, sellerAccountId);

        MarketplaceConnection connection;
        if (existingConnection.isPresent()) {
            connection = existingConnection.get();
            connection.setEncryptedAccessToken(encryptedAccessToken);
            connection.setEncryptedRefreshToken(encryptedRefreshToken);
            connection.setStatus("ACTIVE");
            connection.setTokenExpiresAt(expiresAt);
            connection.setAuthorizedScopes(tokenResponse.scope());
            // lastRefreshedAt permanece null até o primeiro refresh explícito
        } else {
            connection = new MarketplaceConnection(
                    UUID.randomUUID(),
                    tenantId,
                    MARKETPLACE_ID,
                    sellerAccountId,
                    "Mercado Livre (" + sellerAccountId + ")",
                    encryptedAccessToken,
                    encryptedRefreshToken,
                    "ACTIVE"
            );
            connection.setTokenExpiresAt(expiresAt);
            connection.setAuthorizedScopes(tokenResponse.scope());
            // lastRefreshedAt permanece null até o primeiro refresh explícito
        }

        MarketplaceConnection saved = connectionRepository.save(connection);

        // 5. Auditoria de sucesso sem expor tokens
        auditPublisher.publish(
                "OAUTH_CALLBACK_SUCCESS",
                userId,
                null,
                tenantId,
                ipAddress,
                null,
                correlationId,
                "Conexão oficial com Mercado Livre estabelecida com sucesso. Conta: " + sellerAccountId
        );

        return ConnectionSummaryResponse.fromEntity(saved);
    }

    /**
     * Renova o token de acesso sob lock exclusivo por conexão, garantindo que
     * requisições concorrentes não dupliquem a rotação do refresh_token.
     */
    public ConnectionSummaryResponse refreshToken(
            UUID tenantId,
            UUID connectionId,
            UUID userId,
            String ipAddress,
            String correlationId
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(connectionId, "connectionId não pode ser nulo");

        return connectionLock.executeWithLock(connectionId, () -> {
            MarketplaceConnection connection = connectionRepository.findByTenantIdAndId(tenantId, connectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conexão não encontrada para o tenant informado"));

            if (!"ACTIVE".equals(connection.getStatus())) {
                throw new IllegalStateException("Conexão não está ativa para renovação de token. Status atual: " + connection.getStatus());
            }

            // Verificação de frescor (Double-Check): se o token foi renovado há menos de 60s, reaproveita
            Instant now = Instant.now();
            if (connection.getLastRefreshedAt() != null
                    && Duration.between(connection.getLastRefreshedAt(), now).getSeconds() < 60
                    && connection.getTokenExpiresAt() != null
                    && connection.getTokenExpiresAt().isAfter(now.plus(Duration.ofMinutes(10)))) {
                log.info("Token da conexão {} já foi renovado recentemente por outra thread. Reaproveitando.", connectionId);
                return ConnectionSummaryResponse.fromEntity(connection);
            }

            String currentRefreshToken = encryptionService.decrypt(connection.getEncryptedRefreshToken());
            MeliTokenResponse tokenResponse = oauthClient.refreshAccessToken(currentRefreshToken);

            // Cifra e persiste os novos tokens (Rotação do refresh token)
            connection.setEncryptedAccessToken(encryptionService.encrypt(tokenResponse.accessToken()));
            connection.setEncryptedRefreshToken(encryptionService.encrypt(tokenResponse.refreshToken()));
            connection.setTokenExpiresAt(now.plusSeconds(tokenResponse.expiresIn()));
            connection.setLastRefreshedAt(now);
            connection.setStatus("ACTIVE");

            MarketplaceConnection updated = connectionRepository.save(connection);

            auditPublisher.publish(
                    "TOKEN_REFRESH_SUCCESS",
                    userId,
                    null,
                    tenantId,
                    ipAddress,
                    null,
                    correlationId,
                    "Tokens da conexão com Mercado Livre rotacionados e atualizados com sucesso sob lock."
            );

            return ConnectionSummaryResponse.fromEntity(updated);
        });
    }

    /**
     * Revoga localmente a conexão, limpando as credenciais cifradas e impedindo novas chamadas.
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

        MarketplaceConnection connection = connectionRepository.findByTenantIdAndId(tenantId, connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conexão não encontrada para o tenant informado"));

        // Limpa os tokens sensíveis
        connection.setEncryptedAccessToken(null);
        connection.setEncryptedRefreshToken(null);
        connection.setStatus("REVOKED");

        MarketplaceConnection saved = connectionRepository.save(connection);

        auditPublisher.publish(
                "TOKEN_REVOCATION_SUCCESS",
                userId,
                null,
                tenantId,
                ipAddress,
                null,
                correlationId,
                "Conexão do Mercado Livre revogada com sucesso. Credenciais locais invalidadas."
        );

        return ConnectionSummaryResponse.fromEntity(saved);
    }

    /**
     * Retorna o token de acesso em texto puro para uso interno por adaptadores autorizados.
     * Se o token estiver prestes a expirar (< 5 minutos), renova automaticamente.
     */
    public String getDecryptedAccessToken(UUID tenantId, UUID connectionId) {
        MarketplaceConnection connection = connectionRepository.findByTenantIdAndId(tenantId, connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conexão não encontrada"));

        if (!"ACTIVE".equals(connection.getStatus())) {
            throw new IllegalStateException("Conexão com Mercado Livre inativa ou revogada");
        }

        // Se expira em menos de 5 minutos, executa refresh automático sob lock
        Instant now = Instant.now();
        if (connection.getTokenExpiresAt() != null && connection.getTokenExpiresAt().isBefore(now.plus(Duration.ofMinutes(5)))) {
            ConnectionSummaryResponse refreshed = refreshToken(tenantId, connectionId, null, "127.0.0.1", "auto-refresh");
            connection = connectionRepository.findByTenantIdAndId(tenantId, refreshed.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Conexão não encontrada após refresh"));
        }

        return encryptionService.decrypt(connection.getEncryptedAccessToken());
    }

    public List<ConnectionSummaryResponse> listConnections(UUID tenantId) {
        return connectionRepository.findAllByTenantId(tenantId).stream()
                .filter(conn -> MARKETPLACE_ID.equals(conn.getMarketplace()))
                .map(ConnectionSummaryResponse::fromEntity)
                .toList();
    }

    public ConnectionSummaryResponse getConnection(UUID tenantId, UUID connectionId) {
        return connectionRepository.findByTenantIdAndId(tenantId, connectionId)
                .map(ConnectionSummaryResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Conexão não encontrada"));
    }

    private void validateConfiguredScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new UnauthorizedScopeException("Nenhum escopo configurado para a integração com o Mercado Livre");
        }
        for (String scope : scopes) {
            if ("write".equalsIgnoreCase(scope.trim())) {
                throw new UnauthorizedScopeException("O escopo 'write' está proibido pela governança do MVP da plataforma");
            }
        }
    }
}
