package br.com.sellerintelligence.mcp.service;

import br.com.sellerintelligence.mcp.domain.McpApiKey;
import br.com.sellerintelligence.mcp.domain.McpApiKeyRepository;
import br.com.sellerintelligence.mcp.web.dto.ApiKeyCreatedResponse;
import br.com.sellerintelligence.mcp.web.dto.CreateApiKeyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class McpAuthService {

    private static final Logger log = LoggerFactory.getLogger(McpAuthService.class);
    private static final String KEY_PREFIX = "sip_mcp_";

    private final McpApiKeyRepository apiKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public McpAuthService(McpApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Transactional
    public ApiKeyCreatedResponse createApiKey(UUID tenantId, UUID userId, CreateApiKeyRequest request) {
        String keyName = request.name() != null && !request.name().isBlank() ? request.name().trim() : "MCP Integration Key";
        long expiresInDays = request.expiresInDays() != null && request.expiresInDays() > 0 ? request.expiresInDays() : 90;

        // Gera 24 bytes aleatórios em hexadecimal (48 chars)
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }
        String plainKey = KEY_PREFIX + sb;
        String keyHash = hashKey(plainKey);

        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(expiresInDays);

        McpApiKey apiKey = new McpApiKey(
                tenantId,
                keyName,
                keyHash,
                KEY_PREFIX,
                userId,
                expiresAt
        );

        McpApiKey saved = apiKeyRepository.save(apiKey);
        log.info("Created new MCP API key: id={}, tenantId={}, name={}, expiresAt={}",
                saved.getId(), tenantId, keyName, expiresAt);

        return new ApiKeyCreatedResponse(
                saved.getId(),
                saved.getName(),
                plainKey,
                saved.getKeyPrefix(),
                saved.getPermissions(),
                saved.getExpiresAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
    }

    @Transactional
    public Optional<McpApiKey> authenticateApiKey(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        String token = rawToken.trim();
        if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
            token = token.substring(7).trim();
        }

        if (!token.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }

        String keyHash = hashKey(token);
        Optional<McpApiKey> keyOpt = apiKeyRepository.findByKeyHash(keyHash);

        if (keyOpt.isPresent()) {
            McpApiKey key = keyOpt.get();
            if (key.isActive()) {
                key.markUsed();
                apiKeyRepository.save(key);
                return Optional.of(key);
            } else {
                log.warn("MCP API key is inactive or expired: id={}, status={}", key.getId(), key.getStatus());
            }
        }

        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public List<McpApiKey> listApiKeys(UUID tenantId) {
        return apiKeyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public boolean revokeApiKey(UUID tenantId, UUID keyId) {
        Optional<McpApiKey> keyOpt = apiKeyRepository.findByIdAndTenantId(keyId, tenantId);
        if (keyOpt.isPresent()) {
            McpApiKey key = keyOpt.get();
            key.revoke();
            apiKeyRepository.save(key);
            log.info("Revoked MCP API key: id={}, tenantId={}", keyId, tenantId);
            return true;
        }
        return false;
    }

    public String hashKey(String plainKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(plainKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encoded) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
