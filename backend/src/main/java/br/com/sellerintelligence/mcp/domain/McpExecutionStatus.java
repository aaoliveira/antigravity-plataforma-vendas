package br.com.sellerintelligence.mcp.domain;

public enum McpExecutionStatus {
    SUCCESS,
    ERROR,
    BLOCKED_MUTABLE_DENIED,
    BLOCKED_CROSS_TENANT,
    BLOCKED_INJECTION
}
