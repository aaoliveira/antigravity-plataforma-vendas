package br.com.sellerintelligence.compliance.domain;

/**
 * Lifecycle status of a compliance incident.
 */
public enum IncidentStatus {
    OPEN,
    INVESTIGATING,
    MITIGATING,
    AWAITING_APPROVAL,
    RESOLVED,
    CLOSED
}
