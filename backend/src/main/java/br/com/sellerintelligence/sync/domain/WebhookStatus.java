package br.com.sellerintelligence.sync.domain;

public enum WebhookStatus {
    RECEIVED,
    PROCESSED,
    DUPLICATE_IGNORED,
    OUT_OF_ORDER_IGNORED,
    FAILED
}
