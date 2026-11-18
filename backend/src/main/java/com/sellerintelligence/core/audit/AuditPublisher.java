package com.sellerintelligence.core.audit;

import java.util.UUID;

/**
 * Contrato de publicação de eventos de auditoria e segurança.
 * Implementado pelo módulo de auditoria com persistência e logging estruturado.
 */
public interface AuditPublisher {

    void publish(
            String eventType,
            UUID principalId,
            String principalEmail,
            UUID tenantId,
            String ipAddress,
            String userAgent,
            String correlationId,
            String details
    );
}
