package br.com.sellerintelligence.audit.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private static final Pattern SENSITIVE_TOKEN_PATTERN = Pattern.compile("(?i)(bearer\\s+[a-z0-9._~+/-]+=*|password\\s*=\\s*[^\\s,]+|secret\\s*=\\s*[^\\s,]+)");

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Persists an audit event in a new transaction to ensure audit records are kept
     * even if the calling business transaction fails or is rolled back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent recordEvent(UUID tenantId, String actorId, String action,
                                  String targetEntity, String targetId, String details,
                                  AuditResult result) {
        String sanitizedDetails = sanitize(details);
        AuditEvent event = new AuditEvent(
                tenantId,
                actorId != null ? actorId : "ANONYMOUS",
                action,
                targetEntity,
                targetId,
                sanitizedDetails,
                result
        );

        log.info("AUDIT_LOG: actor={}, tenant={}, action={}, target={}, result={}",
                event.getActorId(), event.getTenantId(), event.getAction(), event.getTargetEntity(), event.getResult());

        return auditEventRepository.save(event);
    }

    private String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return SENSITIVE_TOKEN_PATTERN.matcher(input).replaceAll("[REDACTED]");
    }
}
