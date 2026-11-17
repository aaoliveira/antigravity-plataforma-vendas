package br.com.sellerintelligence.mcp.service;

import br.com.sellerintelligence.mcp.domain.McpApiKey;
import br.com.sellerintelligence.mcp.domain.McpApiKeyRepository;
import br.com.sellerintelligence.mcp.web.dto.ApiKeyCreatedResponse;
import br.com.sellerintelligence.mcp.web.dto.CreateApiKeyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpAuthServiceTest {

    @Mock
    private McpApiKeyRepository apiKeyRepository;

    private McpAuthService authService;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        authService = new McpAuthService(apiKeyRepository);
    }

    @Test
    @DisplayName("Should create MCP API key with prefix sip_mcp_, SHA-256 hash and READ_ONLY permission")
    void shouldCreateApiKey() {
        when(apiKeyRepository.save(any(McpApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateApiKeyRequest request = new CreateApiKeyRequest("Cursor MCP Client", 60L);
        ApiKeyCreatedResponse response = authService.createApiKey(tenantId, userId, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Cursor MCP Client");
        assertThat(response.plainTextApiKey()).startsWith("sip_mcp_");
        assertThat(response.plainTextApiKey().length()).isGreaterThan(30);
        assertThat(response.keyPrefix()).isEqualTo("sip_mcp_");
        assertThat(response.permissions()).isEqualTo("READ_ONLY");

        verify(apiKeyRepository).save(any(McpApiKey.class));
    }

    @Test
    @DisplayName("Should authenticate valid plain API key by computing matching SHA-256 hash")
    void shouldAuthenticateValidApiKey() {
        String plainKey = "sip_mcp_abc1234567890abcdef1234567890";
        String hash = authService.hashKey(plainKey);

        McpApiKey key = new McpApiKey(tenantId, "Valid Key", hash, "sip_mcp_", userId, null);
        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(key));

        Optional<McpApiKey> authenticated = authService.authenticateApiKey("Bearer " + plainKey);

        assertThat(authenticated).isPresent();
        assertThat(authenticated.get().getTenantId()).isEqualTo(tenantId);
        assertThat(authenticated.get().isActive()).isTrue();
        assertThat(key.getLastUsedAt()).isNotNull();
        verify(apiKeyRepository).save(key);
    }

    @Test
    @DisplayName("Should reject authentication for invalid or un-prefixed token")
    void shouldRejectInvalidToken() {
        assertThat(authService.authenticateApiKey(null)).isEmpty();
        assertThat(authService.authenticateApiKey("")).isEmpty();
        assertThat(authService.authenticateApiKey("Bearer invalid-token-without-prefix")).isEmpty();

        String unmatchingKey = "sip_mcp_unknown_key_999";
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.empty());
        assertThat(authService.authenticateApiKey("Bearer " + unmatchingKey)).isEmpty();
    }

    @Test
    @DisplayName("Should revoke existing API key")
    void shouldRevokeApiKey() {
        UUID keyId = UUID.randomUUID();
        McpApiKey key = new McpApiKey(tenantId, "To Revoke", "hash", "sip_mcp_", userId, null);

        when(apiKeyRepository.findByIdAndTenantId(keyId, tenantId)).thenReturn(Optional.of(key));

        boolean revoked = authService.revokeApiKey(tenantId, keyId);

        assertThat(revoked).isTrue();
        assertThat(key.getStatus()).isEqualTo("REVOKED");
        assertThat(key.isActive()).isFalse();
        verify(apiKeyRepository).save(key);
    }
}
