package br.com.sellerintelligence.compliance.domain;

/**
 * Status of marketplace compliance evaluation.
 * In accordance with FAIL CLOSED architecture, any unverified status
 * strictly prevents production execution.
 */
public enum ComplianceStatus {
    /**
     * Fully compliant with official APIs, developer terms of service, and privacy standards.
     */
    COMPLIANT,

    /**
     * Non-compliant: requires scraping, CAPTCHA bypass, bot evasion, or violates platform policies.
     */
    NON_COMPLIANT,

    /**
     * Partially compliant: certain official APIs are compliant (e.g. catalog/orders), while others
     * (e.g. ads or scraping) are locked or require special partnership whitelisting.
     */
    PARTIALLY_COMPLIANT,

    /**
     * Compliance not verified or official partnership not yet established.
     * Strictly blocked in production under FAIL CLOSED policy.
     */
    UNVERIFIED_COMPLIANCE,

    /**
     * Not applicable to the evaluated context.
     */
    NOT_APPLICABLE
}
