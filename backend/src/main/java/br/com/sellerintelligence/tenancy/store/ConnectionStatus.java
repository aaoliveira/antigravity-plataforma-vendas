package br.com.sellerintelligence.tenancy.store;

public enum ConnectionStatus {
    DISCONNECTED,
    CONNECTED,
    NEEDS_REAUTH,
    PAUSED,
    REVOKED
}
