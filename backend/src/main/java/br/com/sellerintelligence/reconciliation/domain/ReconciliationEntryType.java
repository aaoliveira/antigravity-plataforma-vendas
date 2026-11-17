package br.com.sellerintelligence.reconciliation.domain;

public enum ReconciliationEntryType {
    ORDER_PAYOUT,
    COMMISSION_FEE,
    TRANSACTION_FEE,
    SHIPPING_DEBIT,
    SHIPPING_REIMBURSEMENT,
    REFUND_CHARGEBACK,
    ESCROW_RELEASE,
    ADJUSTMENT
}
