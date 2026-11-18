package com.sellerintelligence.modules.marketplaceconnection.shopee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.modules.marketplaceconnection.DistributedConnectionLock;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnection;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnectionRepository;
import com.sellerintelligence.modules.marketplaceconnection.dto.ConnectionSummaryResponse;
import com.sellerintelligence.modules.marketplaceconnection.dto.InitiateAuthResponse;
import com.sellerintelligence.modules.marketplaceconnection.exception.ShopeeOAuthException;
import com.sellerintelligence.modules.marketplaceconnection.exception.ShopeeRateLimitException;
import com.sellerintelligence.modules.marketplaceconnection.security.KmsKeyProvider;
import com.sellerintelligence.modules.marketplaceconnection.security.TokenEncryptionService;
import com.sellerintelligence.modules.marketplaceconnection.shopee.dto.ShopeeCallbackRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ShopeeOpenPlatformServiceTest {

    private ShopeeOAuthProperties properties;
    private FakeShopeeOpenPlatformClient fakeClient;
    private TokenEncryptionService encryptionService;
    private MarketplaceConnectionRepository connectionRepository;
    private DistributedConnectionLock connectionLock;
    private AuditPublisher auditPublisher;
    private ShopeeWebhookVerifier webhookVerifier;
    private ObjectMapper objectMapper;
    private ShopeeOpenPlatformService shopeeService;

    private final Map<UUID, MarketplaceConnection> database = new ConcurrentHashMap<>();

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final long PARTNER_ID = 100001L;
    private static final String PARTNER_KEY = "test_partner_key_shopee_secret_12345";
    private static final String WEBHOOK_URL = "http://localhost:8082/api/v1/integrations/shopee/webhook";

    @BeforeEach
    void setUp() {
        properties = new ShopeeOAuthProperties();
        properties.setPartnerId(PARTNER_ID);
        properties.setPartnerKey(PARTNER_KEY);
        properties.setAuthBaseUrl("https://partner.shopeemobile.com");
        properties.setApiBaseUrl("https://partner.shopeemobile.com");
        properties.setRedirectUrl("http://localhost:3000/api/v1/integrations/shopee/callback");
        properties.setWebhookUrl(WEBHOOK_URL);
        properties.setEnabled(true);
        properties.setSandbox(true);

        fakeClient = new FakeShopeeOpenPlatformClient();

        KmsKeyProvider keyProvider = new KmsKeyProvider("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        encryptionService = new TokenEncryptionService(keyProvider);

        database.clear();
        connectionRepository = mock(MarketplaceConnectionRepository.class);

        when(connectionRepository.save(any(MarketplaceConnection.class))).thenAnswer(invocation -> {
            MarketplaceConnection entity = invocation.getArgument(0);
            database.put(entity.getId(), entity);
            return entity;
        });

        when(connectionRepository.findByIdAndTenantId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            UUID tenant = invocation.getArgument(1);
            MarketplaceConnection entity = database.get(id);
            if (entity != null && entity.getTenantId().equals(tenant)) {
                return Optional.of(entity);
            }
            return Optional.empty();
        });

        when(connectionRepository.findByTenantIdAndId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID tenant = invocation.getArgument(0);
            UUID id = invocation.getArgument(1);
            MarketplaceConnection entity = database.get(id);
            if (entity != null && entity.getTenantId().equals(tenant)) {
                return Optional.of(entity);
            }
            return Optional.empty();
        });

        when(connectionRepository.findByTenantIdAndMarketplaceAndSellerAccountId(any(UUID.class), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    UUID tenant = invocation.getArgument(0);
                    String mkt = invocation.getArgument(1);
                    String sellerAcc = invocation.getArgument(2);
                    return database.values().stream()
                            .filter(c -> c.getTenantId().equals(tenant) && c.getMarketplace().equals(mkt) && c.getSellerAccountId().equals(sellerAcc))
                            .findFirst();
                });

        when(connectionRepository.findByTenantIdAndMarketplace(any(UUID.class), anyString()))
                .thenAnswer(invocation -> {
                    UUID tenant = invocation.getArgument(0);
                    String mkt = invocation.getArgument(1);
                    return database.values().stream()
                            .filter(c -> c.getTenantId().equals(tenant) && c.getMarketplace().equals(mkt))
                            .toList();
                });

        when(connectionRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(database.values()));

        connectionLock = new DistributedConnectionLock();
        auditPublisher = mock(AuditPublisher.class);
        webhookVerifier = new ShopeeWebhookVerifier(properties);
        objectMapper = new ObjectMapper();

        shopeeService = new ShopeeOpenPlatformService(
                properties,
                fakeClient,
                encryptionService,
                connectionRepository,
                connectionLock,
                auditPublisher,
                webhookVerifier,
                objectMapper
        );
    }

    @Test
    @DisplayName("1. Inicia autorização Shopee gerando URL oficial assinada com HMAC-SHA256")
    void testInitiateAuthorizationReturnsValidSignedUrl() {
        InitiateAuthResponse response = shopeeService.initiateAuthorization(TENANT_ID, USER_ID, "127.0.0.1", "corr-1");

        assertThat(response).isNotNull();
        assertThat(response.authorizationUrl()).contains("https://partner.shopeemobile.com/api/v2/shop/auth_partner");
        assertThat(response.authorizationUrl()).contains("partner_id=100001");
        assertThat(response.authorizationUrl()).contains("timestamp=");
        assertThat(response.authorizationUrl()).contains("sign=");
        assertThat(response.authorizationUrl()).contains("redirect=");

        verify(auditPublisher).publish(
                eq("SHOPEE_AUTH_INITIATED"),
                eq(USER_ID),
                isNull(),
                eq(TENANT_ID),
                eq("127.0.0.1"),
                isNull(),
                eq("corr-1"),
                anyString()
        );
    }

    @Test
    @DisplayName("2. Processa callback de autorização, cifra tokens com AES-256-GCM e cria conexão ativa")
    void testHandleCallbackExchangesTokensAndEncryptsWithAesGcm() {
        ShopeeCallbackRequest request = new ShopeeCallbackRequest("test_shopee_auth_code", 987654321L, null);

        ConnectionSummaryResponse summary = shopeeService.handleCallback(TENANT_ID, USER_ID, request, "127.0.0.1", "corr-2");

        assertThat(summary).isNotNull();
        assertThat(summary.marketplace()).isEqualTo("SHOPEE");
        assertThat(summary.sellerAccountId()).isEqualTo("987654321");
        assertThat(summary.status()).isEqualTo("ACTIVE");
        assertThat(summary.authorizedScopes()).contains("Shop Level (OpenAPI v2)");

        // Verifica no banco simulado que os tokens foram gravados cifrados
        MarketplaceConnection persisted = database.get(summary.id());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getEncryptedAccessToken()).isNotBlank();
        assertThat(persisted.getEncryptedRefreshToken()).isNotBlank();

        // O token gravado NÃO deve ser o valor puro
        assertThat(persisted.getEncryptedAccessToken()).doesNotContain("shopee-access-token-");
        assertThat(persisted.getEncryptedRefreshToken()).doesNotContain("shopee-refresh-token-");

        // Decifrando com TokenEncryptionService deve recuperar o token
        String decryptedAccess = encryptionService.decrypt(persisted.getEncryptedAccessToken());
        assertThat(decryptedAccess).startsWith("shopee-access-token-");
    }

    @Test
    @DisplayName("3. Trilha de auditoria não vaza tokens nem segredos no callback")
    void testZeroTokensInAuditLogs() {
        ShopeeCallbackRequest request = new ShopeeCallbackRequest("code-secret-audit", 987654321L, null);

        shopeeService.handleCallback(TENANT_ID, USER_ID, request, "127.0.0.1", "corr-audit");

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditPublisher).publish(
                eq("SHOPEE_CALLBACK_SUCCESS"),
                eq(USER_ID),
                isNull(),
                eq(TENANT_ID),
                eq("127.0.0.1"),
                isNull(),
                eq("corr-audit"),
                detailsCaptor.capture()
        );

        String details = detailsCaptor.getValue();
        assertThat(details).doesNotContain("access_token");
        assertThat(details).doesNotContain("refresh_token");
        assertThat(details).doesNotContain("secret");
    }

    @Test
    @DisplayName("4. Renova token da Shopee sob lock exclusivo e armazena novo refresh_token de uso único")
    void testRefreshAccessTokenRotatesSingleUseTokenUnderLock() {
        // Cria conexão inicial via callback
        ShopeeCallbackRequest request = new ShopeeCallbackRequest("code-1", 987654321L, null);
        ConnectionSummaryResponse initial = shopeeService.handleCallback(TENANT_ID, USER_ID, request, "127.0.0.1", "corr-3");

        String firstRefreshToken = encryptionService.decrypt(database.get(initial.id()).getEncryptedRefreshToken());

        // Força renovação
        ConnectionSummaryResponse refreshed = shopeeService.refreshToken(TENANT_ID, initial.id(), USER_ID, "127.0.0.1", "corr-4");
        assertThat(refreshed).isNotNull();

        MarketplaceConnection updated = database.get(initial.id());
        String secondRefreshToken = encryptionService.decrypt(updated.getEncryptedRefreshToken());

        // Novo refresh_token deve ser diferente do anterior (single-use rotation)
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);
        assertThat(fakeClient.getRefreshAccessTokenCalls()).isEqualTo(1);
    }

    @Test
    @DisplayName("5. Concorrência: 10 threads renovando simultaneamente disparam exatamente 1 chamada externa sob lock")
    void testConcurrentRefreshTokenUnderLockDoesNotDuplicate() throws Exception {
        ShopeeCallbackRequest request = new ShopeeCallbackRequest("code-concurrent", 12345000L, null);
        ConnectionSummaryResponse initial = shopeeService.handleCallback(TENANT_ID, USER_ID, request, "127.0.0.1", "corr-init");

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Future<ConnectionSummaryResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                return shopeeService.refreshToken(TENANT_ID, initial.id(), USER_ID, "127.0.0.1", "corr-conc");
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Libera todas simultaneamente

        for (Future<ConnectionSummaryResponse> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // O lock distribuído e a verificação de frescor garantem apenas 1 chamada à API
        assertThat(fakeClient.getRefreshAccessTokenCalls()).isEqualTo(1);
    }

    @Test
    @DisplayName("6. Revoga conexão localmente, anulando tokens cifrados e marcando status REVOKED")
    void testRevokeConnectionInvalidatesCredentialsAndMarksRevoked() {
        ShopeeCallbackRequest request = new ShopeeCallbackRequest("code-revoke", 555666777L, null);
        ConnectionSummaryResponse initial = shopeeService.handleCallback(TENANT_ID, USER_ID, request, "127.0.0.1", "corr-rev-init");

        ConnectionSummaryResponse revoked = shopeeService.revokeConnection(TENANT_ID, initial.id(), USER_ID, "127.0.0.1", "corr-rev");
        assertThat(revoked.status()).isEqualTo("REVOKED");

        MarketplaceConnection persisted = database.get(initial.id());
        assertThat(persisted.getEncryptedAccessToken()).isNull();
        assertThat(persisted.getEncryptedRefreshToken()).isNull();
        assertThat(persisted.getStatus()).isEqualTo("REVOKED");

        // Tentar renovar conexão revogada deve ser rejeitado
        assertThatThrownBy(() -> shopeeService.refreshToken(TENANT_ID, initial.id(), USER_ID, "127.0.0.1", "corr-rev-fail"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("revogada");
    }

    @Test
    @DisplayName("7. Push Webhook (Code: 2 - Shop Authorization Canceled) revoga automaticamente a conexão")
    void testWebhookPushCode2RevokesShopAuthorization() {
        // Cria conexão inicial
        long shopId = 77889900L;
        ShopeeCallbackRequest request = new ShopeeCallbackRequest("code-webhook", shopId, null);
        ConnectionSummaryResponse initial = shopeeService.handleCallback(TENANT_ID, USER_ID, request, "127.0.0.1", "corr-wh-init");

        // Simula recebimento de push notification da Shopee com code 2
        String rawBody = String.format("{\"code\":2,\"shop_id\":%d,\"timestamp\":%d,\"data\":{}}",
                shopId, Instant.now().getEpochSecond());

        String baseString = ShopeeSignatureUtil.buildWebhookBaseString(WEBHOOK_URL, rawBody);
        String validAuthorizationHeader = ShopeeSignatureUtil.calculateHmacSha256(baseString, PARTNER_KEY);

        shopeeService.handleWebhook(validAuthorizationHeader, rawBody, WEBHOOK_URL);

        MarketplaceConnection updated = database.get(initial.id());
        assertThat(updated.getStatus()).isEqualTo("REVOKED");
        assertThat(updated.getEncryptedAccessToken()).isNull();
        assertThat(updated.getEncryptedRefreshToken()).isNull();
    }

    @Test
    @DisplayName("8. Rejeita Push Webhook com assinatura HMAC-SHA256 adulterada ou ausente")
    void testWebhookWithForgedSignatureRejected() {
        String rawBody = "{\"code\":2,\"shop_id\":12345,\"timestamp\":1234567890}";

        assertThatThrownBy(() -> shopeeService.handleWebhook("forged_signature_hex", rawBody, WEBHOOK_URL))
                .isInstanceOf(com.sellerintelligence.modules.marketplaceconnection.exception.ShopeeWebhookSignatureException.class)
                .hasMessageContaining("inválida");
    }

    @Test
    @DisplayName("9. Trata erro de limite de quota (error_rate_limit) lançado pelo cliente Shopee")
    void testShopeeRateLimitExceptionHandled() {
        fakeClient.setSimulateRateLimit(true);

        ShopeeCallbackRequest request = new ShopeeCallbackRequest("code-rate", 12345L, null);

        assertThatThrownBy(() -> shopeeService.handleCallback(TENANT_ID, USER_ID, request, "127.0.0.1", "corr-rate"))
                .isInstanceOf(ShopeeRateLimitException.class)
                .hasMessageContaining("rate limit");
    }

    @Test
    @DisplayName("10. Bloqueia operações se a integração Shopee estiver desabilitada por governança")
    void testShopeeDisabledBlocksCallback() {
        properties.setEnabled(false);

        ShopeeCallbackRequest request = new ShopeeCallbackRequest("code-dis", 12345L, null);

        assertThatThrownBy(() -> shopeeService.handleCallback(TENANT_ID, USER_ID, request, "127.0.0.1", "corr-dis"))
                .isInstanceOf(ShopeeOAuthException.class)
                .hasMessageContaining("desabilitada");
    }
}
