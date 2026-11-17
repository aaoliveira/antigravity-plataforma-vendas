package br.com.sellerintelligence.compliance.domain;

/**
 * Categorization of operation risk level for marketplace interactions.
 */
public enum OperationRiskLevel {
    /**
     * Read-only operations such as fetching listings, reading orders, or querying public catalog search.
     */
    READ_ONLY,

    /**
     * Mutable critical operations that alter seller external state on marketplaces:
     * publishing listings, updating prices, modifying inventory, or dispatching customer messages.
     * Mandatorily requires explicit human-in-the-loop approval.
     */
    MUTABLE_CRITICAL,

    /**
     * Operations touching restricted personally identifiable information (PII).
     * Requires authorized scope and strict minimization under LGPD.
     */
    RESTRICTED_PII
}
