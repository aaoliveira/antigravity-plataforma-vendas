package com.sellerintelligence.modules.audit;

import com.sellerintelligence.core.audit.AuditPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Serviço de auditoria que persiste e loga eventos de segurança, autenticação e conformidade.
 */
@Service
public class AuditService implements AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void publish(
            String eventType,
            UUID principalId,
            String principalEmail,
            UUID tenantId,
            String ipAddress,
            String userAgent,
            String correlationId,
            String details
    ) {
        String sanitizedDetails = AuditSanitizer.sanitize(details);

        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                eventType,
                principalId,
                principalEmail,
                tenantId,
                ipAddress,
                userAgent,
                correlationId,
                sanitizedDetails,
                Instant.now()
        );

        auditEventRepository.save(event);

        log.info("AUDIT_EVENT: type={} principal={} tenant={} ip={} corrId={} details={}",
                eventType, principalEmail, tenantId, ipAddress, correlationId, sanitizedDetails);
    }
}
