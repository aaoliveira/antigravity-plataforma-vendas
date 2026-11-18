package com.sellerintelligence.modules.productmonitoring.model;

/**
 * Resultado auditável de execução ou tentativa de job de monitoramento.
 * Rastreia formalmente execuções com sucesso, cancelamentos por Kill Switch,
 * rejeições por Rate Limit, janelas mínimas e bloqueios de fontes não autorizadas.
 */
public enum JobExecutionStatus {
    SUCCESS,
    SKIPPED_KILL_SWITCH,
    SKIPPED_RATE_LIMIT,
    SKIPPED_WINDOW_MINIMA,
    FAILED_UNAUTHORIZED_SOURCE,
    FAILED_ERROR
}
