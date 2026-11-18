package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.modules.marketplaceconnection.DistributedConnectionLock;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnection;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnectionRepository;
import com.sellerintelligence.modules.marketplaceconnection.exception.InvalidStateCsrfException;
import com.sellerintelligence.modules.marketplaceconnection.exception.UnauthorizedScopeException;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.ConnectionSummaryResponse;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.InitiateAuthResponse;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.OAuthCallbackRequest;
import com.sellerintelligence.modules.marketplaceconnection.security.KmsKeyProvider;
import com.sellerintelligence.modules.marketplaceconnection.security.TokenEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MercadoLivreOAuthServiceTest {

    private MeliOAuthProperties properties;
    private FakeMeliOAuthClient fakeClient;
    private OAuthStateManager stateManager;
    private TokenEncryptionService encryptionService;
    private MarketplaceConnectionRepository connectionRepository;
    private DistributedConnectionLock connectionLock;
    private AuditPublisher auditPublisher;
    private MercadoLivreOAuthService oauthService;

    private final Map<UUID, MarketplaceConnection> database = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        properties = new MeliOAuthProperties();
        properties.setAppId("123456789");
        properties.setClientSecret("mock-secret-key");
        properties.setRedirectUri("http://localhost:3000/callback");
        properties.setScopes(List.of("read", "offline_access"));

        fakeClient = new FakeMeliOAuthClient();
        stateManager = new OAuthStateManager();

        KmsKeyProvider keyProvider = new KmsKeyProvider("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        encryptionService = new TokenEncryptionService(keyProvider);

        connectionRepository = mock(MarketplaceConnectionRepository.class);
        database.clear();

        when(connectionRepository.save(any(MarketplaceConnection.class))).thenAnswer(invocation -> {
            MarketplaceConnection conn = invocation.getArgument(0);
            database.put(conn.getId(), conn);
            return conn;
        });

        when(connectionRepository.findByTenantIdAndId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID tenantId = invocation.getArgument(0);
            UUID id = invocation.getArgument(1);
            MarketplaceConnection conn = database.get(id);
            if (conn != null && conn.getTenantId().equals(tenantId)) {
                return Optional.of(conn);
            }
            return Optional.empty();
        });

        when(connectionRepository.findByTenantIdAndMarketplaceAndSellerAccountId(any(UUID.class), anyString(), anyString())).thenAnswer(invocation -> {
            UUID tenantId = invocation.getArgument(0);
            String marketplace = invocation.getArgument(1);
            String accountId = invocation.getArgument(2);
            return database.values().stream()
                    .filter(c -> c.getTenantId().equals(tenantId) && c.getMarketplace().equals(marketplace) && c.getSellerAccountId().equals(accountId))
                    .findFirst();
        });

        connectionLock = new DistributedConnectionLock();
        auditPublisher = mock(AuditPublisher.class);

        oauthService = new MercadoLivreOAuthService(
                properties,
                fakeClient,
                stateManager,
                encryptionService,
                connectionRepository,
                connectionLock,
                auditPublisher
        );
    }

    @Test
    @DisplayName("Inicia o fluxo OAuth com URL oficial, PKCE S256 e state anti-CSRF")
    void testInitiateAuthorizationSuccess() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        InitiateAuthResponse response = oauthService.initiateAuthorization(tenantId, userId, "127.0.0.1", "corr-1");

        assertThat(response).isNotNull();
        assertThat(response.stateToken()).isNotBlank();
        assertThat(response.authorizationUrl()).contains("https://auth.mercadolivre.com.br/authorization");
        assertThat(response.authorizationUrl()).contains("code_challenge=");
        assertThat(response.authorizationUrl()).contains("code_challenge_method=S256");
        assertThat(response.authorizationUrl()).contains("state=" + response.stateToken());

        verify(auditPublisher).publish(
                eq("OAUTH_AUTHORIZE_INITIATED"),
                eq(userId),
                isNull(),
                eq(tenantId),
                eq("127.0.0.1"),
                isNull(),
                eq("corr-1"),
                anyString()
        );
    }

    @Test
    @DisplayName("Processa callback com sucesso, cifra tokens com AES-256-GCM e cria conexão tenant-scoped")
    void testHandleCallbackSuccess() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // 1. Inicia
        InitiateAuthResponse init = oauthService.initiateAuthorization(tenantId, userId, "127.0.0.1", "corr-1");

        // 2. Callback
        OAuthCallbackRequest callbackRequest = new OAuthCallbackRequest("TG-auth-code-123", init.stateToken(), "http://localhost:3000/callback");
        ConnectionSummaryResponse connResponse = oauthService.handleCallback(tenantId, userId, callbackRequest, "127.0.0.1", "corr-2");

        assertThat(connResponse).isNotNull();
        assertThat(connResponse.tenantId()).isEqualTo(tenantId);
        assertThat(connResponse.status()).isEqualTo("ACTIVE");
        assertThat(connResponse.marketplace()).isEqualTo("MERCADO_LIVRE");

        // Verifica que no banco os tokens estão cifrados e não em texto puro
        MarketplaceConnection persisted = database.get(connResponse.id());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getEncryptedAccessToken()).doesNotContain("APP_USR");
        assertThat(persisted.getEncryptedRefreshToken()).doesNotContain("TG-");

        // Decifra com sucesso usando o serviço
        String plainAccessToken = oauthService.getDecryptedAccessToken(tenantId, connResponse.id());
        assertThat(plainAccessToken).startsWith("APP_USR-mock-access-token");

        verify(auditPublisher).publish(
                eq("OAUTH_CALLBACK_SUCCESS"),
                eq(userId),
                isNull(),
                eq(tenantId),
                eq("127.0.0.1"),
                isNull(),
                eq("corr-2"),
                anyString()
        );
    }

    @Test
    @DisplayName("Rejeita callback com state anti-CSRF adulterado ou inexistente")
    void testHandleCallbackRejectsInvalidState() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        OAuthCallbackRequest callbackRequest = new OAuthCallbackRequest("TG-code-123", "fake-tampered-state", "http://localhost:3000/callback");

        assertThatThrownBy(() -> oauthService.handleCallback(tenantId, userId, callbackRequest, "127.0.0.1", "corr-3"))
                .isInstanceOf(InvalidStateCsrfException.class);

        assertThat(fakeClient.getExchangeCount()).isEqualTo(0);

        verify(auditPublisher).publish(
                eq("OAUTH_CALLBACK_INVALID_STATE"),
                eq(userId),
                isNull(),
                eq(tenantId),
                eq("127.0.0.1"),
                isNull(),
                eq("corr-3"),
                anyString()
        );
    }

    @Test
    @DisplayName("Bloqueia tentativa de configurar ou solicitar escopo 'write' (Menor Privilégio no MVP)")
    void testRejectWriteScope() {
        properties.setScopes(List.of("read", "write", "offline_access"));

        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> oauthService.initiateAuthorization(tenantId, userId, "127.0.0.1", "corr-4"))
                .isInstanceOf(UnauthorizedScopeException.class)
                .hasMessageContaining("O escopo 'write' está proibido pela governança do MVP");
    }

    @Test
    @DisplayName("Renovação concorrente: 10 threads simultâneas executam sob lock e NÃO duplicam a chamada ao ML")
    void testConcurrentRefreshTokenDoesNotDuplicate() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // 1. Cria conexão inicial
        InitiateAuthResponse init = oauthService.initiateAuthorization(tenantId, userId, "127.0.0.1", "corr-1");
        OAuthCallbackRequest callback = new OAuthCallbackRequest("TG-code-1", init.stateToken(), "http://localhost:3000/callback");
        ConnectionSummaryResponse conn = oauthService.handleCallback(tenantId, userId, callback, "127.0.0.1", "corr-2");

        // Simula que a conexão necessita de renovação (não renovada recentemente)
        MarketplaceConnection persisted = database.get(conn.id());
        persisted.setLastRefreshedAt(null);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<ConnectionSummaryResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await(); // Todas as threads disparam juntas
                return oauthService.refreshToken(tenantId, conn.id(), userId, "127.0.0.1", "corr-refresh");
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Libera concorrência

        for (Future<ConnectionSummaryResponse> future : futures) {
            ConnectionSummaryResponse result = future.get(10, TimeUnit.SECONDS);
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("ACTIVE");
        }

        executor.shutdown();

        // Verificação de ouro: o cliente Fake do Mercado Livre deve ter sido chamado exatamente 1 vez
        // graças ao lock e à verificação de frescor (Double-Check), prevenindo a duplicação
        assertThat(fakeClient.getRefreshCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Revogação de conexão anula credenciais cifradas e impede obtenção de token de acesso")
    void testRevokeConnection() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        InitiateAuthResponse init = oauthService.initiateAuthorization(tenantId, userId, "127.0.0.1", "corr-1");
        OAuthCallbackRequest callback = new OAuthCallbackRequest("TG-code-1", init.stateToken(), "http://localhost:3000/callback");
        ConnectionSummaryResponse conn = oauthService.handleCallback(tenantId, userId, callback, "127.0.0.1", "corr-2");

        ConnectionSummaryResponse revoked = oauthService.revokeConnection(tenantId, conn.id(), userId, "127.0.0.1", "corr-revoke");

        assertThat(revoked.status()).isEqualTo("REVOKED");

        MarketplaceConnection persisted = database.get(conn.id());
        assertThat(persisted.getEncryptedAccessToken()).isNull();
        assertThat(persisted.getEncryptedRefreshToken()).isNull();
        assertThat(persisted.getStatus()).isEqualTo("REVOKED");

        assertThatThrownBy(() -> oauthService.getDecryptedAccessToken(tenantId, conn.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inativa ou revogada");

        verify(auditPublisher).publish(
                eq("TOKEN_REVOCATION_SUCCESS"),
                eq(userId),
                isNull(),
                eq(tenantId),
                eq("127.0.0.1"),
                isNull(),
                eq("corr-revoke"),
                anyString()
        );
    }

    @Test
    @DisplayName("Trilha de auditoria não registra tokens em texto puro em nenhum evento")
    void testAuditTrailDoesNotLeakTokens() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        InitiateAuthResponse init = oauthService.initiateAuthorization(tenantId, userId, "127.0.0.1", "corr-1");
        OAuthCallbackRequest callback = new OAuthCallbackRequest("TG-code-1", init.stateToken(), "http://localhost:3000/callback");
        oauthService.handleCallback(tenantId, userId, callback, "127.0.0.1", "corr-2");

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditPublisher, atLeastOnce()).publish(
                anyString(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                detailsCaptor.capture()
        );

        for (String details : detailsCaptor.getAllValues()) {
            assertThat(details).doesNotContain("APP_USR-");
            assertThat(details).doesNotContain("TG-");
            assertThat(details).doesNotContain("mock-secret-key");
        }
    }
}
