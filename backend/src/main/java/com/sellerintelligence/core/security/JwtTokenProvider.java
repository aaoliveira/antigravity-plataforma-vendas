package com.sellerintelligence.core.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Provedor e validador de tokens JWT (RFC 7519) baseado em HMAC-SHA256.
 * Implementação autocontida de alta performance e determinística.
 */
@Component
public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HEADER_BASE64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));

    private final byte[] secretKeyBytes;
    private final long defaultExpirationSeconds;
    private final ObjectMapper objectMapper;

    public JwtTokenProvider(
            @Value("${security.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${security.jwt.expiration-seconds:900}") long defaultExpirationSeconds,
            ObjectMapper objectMapper
    ) {
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.defaultExpirationSeconds = defaultExpirationSeconds;
        this.objectMapper = objectMapper;
    }

    public String generateToken(UUID userId, String email, UUID tenantId, List<String> roles) {
        return generateToken(userId, email, tenantId, roles, defaultExpirationSeconds);
    }

    public String generateToken(UUID userId, String email, UUID tenantId, List<String> roles, long expirationSeconds) {
        long now = Instant.now().getEpochSecond();
        long exp = now + expirationSeconds;

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("sub", userId.toString());
        payload.put("email", email);
        if (tenantId != null) {
            payload.put("tenantId", tenantId.toString());
        }
        if (roles != null && !roles.isEmpty()) {
            ArrayNode rolesArray = payload.putArray("roles");
            roles.forEach(rolesArray::add);
        }
        payload.put("iat", now);
        payload.put("exp", exp);

        String payloadJson = payload.toString();
        String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String contentToSign = HEADER_BASE64 + "." + payloadBase64;
        String signature = sign(contentToSign);

        return contentToSign + "." + signature;
    }

    public Claims validateAndParseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token JWT ausente ou vazio");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException("Estrutura do token JWT inválida (formato esperado: header.payload.signature)");
        }

        String headerBase64 = parts[0];
        String payloadBase64 = parts[1];
        String signatureProvided = parts[2];

        String contentToSign = headerBase64 + "." + payloadBase64;
        String expectedSignature = sign(contentToSign);

        if (!MessageDigest.isEqual(
                signatureProvided.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new InvalidTokenException("Assinatura do token JWT adulterada ou inválida");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadBase64);
            JsonNode jsonNode = objectMapper.readTree(payloadBytes);

            long exp = jsonNode.path("exp").asLong(0);
            long now = Instant.now().getEpochSecond();
            if (exp > 0 && now >= exp) {
                throw new TokenExpiredException("Token JWT expirado em " + Instant.ofEpochSecond(exp));
            }

            UUID userId = UUID.fromString(jsonNode.path("sub").asText());
            String email = jsonNode.path("email").asText();
            UUID tenantId = jsonNode.hasNonNull("tenantId")
                    ? UUID.fromString(jsonNode.path("tenantId").asText())
                    : null;

            List<String> roles = new ArrayList<>();
            JsonNode rolesNode = jsonNode.path("roles");
            if (rolesNode.isArray()) {
                rolesNode.forEach(r -> roles.add(r.asText()));
            }

            return new Claims(userId, email, tenantId, roles, exp);
        } catch (TokenExpiredException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Falha ao decodificar conteúdo do payload JWT: " + e.getMessage(), e);
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Falha criptográfica ao assinar token HMAC-SHA256", e);
        }
    }

    public record Claims(UUID userId, String email, UUID tenantId, List<String> roles, long expirationEpochSecond) {
    }
}
