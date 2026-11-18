package com.sellerintelligence.core.tenant;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Validador de cabeçalhos de mensageria assíncrona (RabbitMQ / SQS).
 * Garante que toda mensagem contenha o cabeçalho 'x-tenant-id' obrigatório
 * e corresponda a um identificador de organização válido.
 */
public final class TenantQueueValidator {

    public static final String HEADER_TENANT_ID = "x-tenant-id";

    private TenantQueueValidator() {
    }

    /**
     * Extrai e valida o UUID do tenant a partir dos cabeçalhos da mensagem.
     *
     * @param headers mapa de cabeçalhos recebido da fila
     * @return UUID do tenant validado
     * @throws TenantContextMissingException se o cabeçalho estiver ausente ou vazio
     * @throws IllegalArgumentException se o formato não for um UUID válido
     */
    public static UUID extractTenantId(Map<String, Object> headers) {
        if (headers == null || !headers.containsKey(HEADER_TENANT_ID)) {
            throw new TenantContextMissingException("Mensagem rejeitada: cabeçalho '" + HEADER_TENANT_ID + "' ausente");
        }

        Object value = headers.get(HEADER_TENANT_ID);
        if (value == null || value.toString().isBlank()) {
            throw new TenantContextMissingException("Mensagem rejeitada: cabeçalho '" + HEADER_TENANT_ID + "' vazio");
        }

        try {
            return UUID.fromString(value.toString().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Mensagem rejeitada: formato de '" + HEADER_TENANT_ID + "' inválido", ex);
        }
    }

    /**
     * Valida se o cabeçalho da mensagem confere com o tenant atualmente esperado.
     *
     * @param headers mapa de cabeçalhos
     * @param expectedTenantId tenant esperado no contexto do processador
     * @throws TenantAccessDeniedException caso os tenants não correspondam
     */
    public static void validateTenantMatch(Map<String, Object> headers, UUID expectedTenantId) {
        Objects.requireNonNull(expectedTenantId, "expectedTenantId não pode ser nulo");
        UUID messageTenantId = extractTenantId(headers);

        if (!expectedTenantId.equals(messageTenantId)) {
            throw new TenantAccessDeniedException(
                    String.format("Tentativa de consumo cross-tenant bloqueada: esperado=%s, recebido=%s",
                            expectedTenantId, messageTenantId)
            );
        }
    }
}
