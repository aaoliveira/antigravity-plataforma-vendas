package br.com.sellerintelligence.compliance.domain;

/**
 * Operational state of a tracked policy.
 */
public enum PolicyStatus {
    ACTIVE,
    UNDER_REVIEW,
    RESTRICTED,
    SUSPENDED,
    BLOCKED,
    PENDING_VALIDATION,
    APPROVED_FOR_REACTIVATION,
    SOURCE_UNAVAILABLE
}
