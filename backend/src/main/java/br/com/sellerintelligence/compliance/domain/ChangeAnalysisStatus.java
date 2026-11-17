package br.com.sellerintelligence.compliance.domain;

/**
 * Status of the automated and manual analysis of a detected policy change.
 */
public enum ChangeAnalysisStatus {
    NEW,
    ANALYZED,
    INCIDENT_OPENED,
    DISMISSED
}
