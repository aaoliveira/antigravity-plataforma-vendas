package br.com.sellerintelligence.compliance.domain;

/**
 * Categorization of detected changes in marketplace technical documentation or policies.
 */
public enum PolicyChangeType {
    RULE_ADDITION,
    RULE_REMOVAL,
    PERMISSION_CHANGED,
    SCOPE_ALTERED,
    RATE_LIMIT_CHANGED,
    ENDPOINT_CHANGED,
    API_DEPRECATED,
    PAYLOAD_CHANGED,
    AUTH_REQUIREMENT_CHANGED,
    STORAGE_RULE_CHANGED,
    AUTOMATION_RULE_CHANGED,
    FORMATTING_ONLY
}
