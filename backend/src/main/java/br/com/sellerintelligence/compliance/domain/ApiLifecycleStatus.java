package br.com.sellerintelligence.compliance.domain;

/**
 * Lifecycle status of a marketplace API version.
 */
public enum ApiLifecycleStatus {
    ACTIVE,
    UNDER_REVIEW,
    DEPRECATED,
    SUNSET_SCHEDULED,
    RETIRED
}
