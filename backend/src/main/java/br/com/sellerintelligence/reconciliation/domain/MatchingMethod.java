package br.com.sellerintelligence.reconciliation.domain;

public enum MatchingMethod {
    ORDER_ID_EXACT,
    TRANSACTION_ID_EXACT,
    HEURISTIC_VALUE_DATE,
    MANUAL_ASSOCIATION,
    UNMATCHED
}
