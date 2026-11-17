package br.com.sellerintelligence.compliance.exception;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

/**
 * Exception thrown when an operation violates marketplace compliance, fail-closed policy,
 * rate limit quarantine, circuit breaker, or missing human approval.
 */
public class ComplianceViolationException extends BusinessException {

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile("(?i)(bearer\\s+[a-z0-9._~+/-]+=*|token=[^\\s,]+|secret=[^\\s,]+|password=[^\\s,]+)");

    private final Long retryAfterSeconds;

    public ComplianceViolationException(String code, String message) {
        this(code, message, HttpStatus.FORBIDDEN, null);
    }

    public ComplianceViolationException(String code, String message, HttpStatus status) {
        this(code, message, status, null);
    }

    public ComplianceViolationException(String code, String message, HttpStatus status, Long retryAfterSeconds) {
        super(code, sanitize(message), status);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return "Compliance violation";
        }
        return SENSITIVE_PATTERN.matcher(raw).replaceAll("[REDACTED]");
    }
}
