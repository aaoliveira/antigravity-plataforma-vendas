package com.sellerintelligence.core.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Utilitário para execução segura de jobs assíncronos e tarefas em background
 * vinculadas estritamente a um tenant. Garante limpeza mandatória do TenantContextHolder
 * no bloco finally.
 */
@Component
public class TenantJobExecutor {

    private static final Logger log = LoggerFactory.getLogger(TenantJobExecutor.class);

    /**
     * Executa um Runnable em contexto tenant-scoped com limpeza garantida.
     *
     * @param tenantId identificador do tenant
     * @param task tarefa a ser executada
     */
    public void execute(UUID tenantId, Runnable task) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo para execução de job");
        Objects.requireNonNull(task, "task não pode ser nula");

        try {
            TenantContextHolder.set(new TenantContext(tenantId, "SYSTEM_JOB"));
            log.trace("Job iniciado para tenant={}", tenantId);
            task.run();
        } finally {
            TenantContextHolder.clear();
            log.trace("Job finalizado e contexto limpo para tenant={}", tenantId);
        }
    }

    /**
     * Executa um Callable que retorna valor em contexto tenant-scoped com limpeza garantida.
     *
     * @param tenantId identificador do tenant
     * @param task tarefa que retorna resultado
     * @param <T> tipo do retorno
     * @return resultado da tarefa
     * @throws Exception caso a tarefa lance exceção
     */
    public <T> T execute(UUID tenantId, Callable<T> task) throws Exception {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo para execução de job");
        Objects.requireNonNull(task, "task não pode ser nula");

        try {
            TenantContextHolder.set(new TenantContext(tenantId, "SYSTEM_JOB"));
            log.trace("Callable job iniciado para tenant={}", tenantId);
            return task.call();
        } finally {
            TenantContextHolder.clear();
            log.trace("Callable job finalizado e contexto limpo para tenant={}", tenantId);
        }
    }
}
