package br.com.sellerintelligence.compliance.domain;

/**
 * Result of a compliance verification performed by MarketplaceComplianceGuard.
 */
public record ComplianceCheckResult(
        boolean allowed,
        String code,
        String reason,
        Long retryAfterSeconds
) {
    public static ComplianceCheckResult allowed(String reason) {
        return new ComplianceCheckResult(true, "OPERATION_ALLOWED", reason, null);
    }

    public static ComplianceCheckResult denied(String code, String reason) {
        return new ComplianceCheckResult(false, code, reason, null);
    }

    public static ComplianceCheckResult rateLimited(Long retryAfterSeconds, String reason) {
        return new ComplianceCheckResult(false, "RATE_LIMIT_COOLDOWN_ACTIVE", reason, retryAfterSeconds);
    }

    public static ComplianceCheckResult humanApprovalRequired(String reason) {
        return new ComplianceCheckResult(false, "HUMAN_APPROVAL_REQUIRED", reason, null);
    }
}
