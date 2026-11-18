package com.sellerintelligence.core.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Gerencia a sessão de banco de dados transacional para PostgreSQL Row-Level Security (RLS).
 * Executa `SELECT set_config('app.current_tenant', ?, true)` garantindo escopo estrito
 * na transação local e limpeza mandatória para evitar vazamento em pools de conexão (HikariCP).
 */
@Component
public class TenantDatabaseSession {

    private static final Logger log = LoggerFactory.getLogger(TenantDatabaseSession.class);

    private final JdbcTemplate jdbcTemplate;

    public TenantDatabaseSession(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Define o tenant ativo para a transação PostgreSQL em curso via SET LOCAL (is_local = true).
     *
     * @param tenantId UUID do tenant
     */
    public void setTenantSession(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo ao definir sessão de banco");
        try {
            jdbcTemplate.queryForObject(
                    "SELECT set_config('app.current_tenant', ?, true)",
                    String.class,
                    tenantId.toString()
            );
            log.trace("Tenant session configurado no PostgreSQL RLS: {}", tenantId);
        } catch (Exception ex) {
            log.debug("Aviso: Falha ou dialeto não-PostgreSQL ao executar set_config: {}", ex.getMessage());
        }
    }

    /**
     * Limpa explicitamente o tenant configurado na sessão atual.
     */
    public void resetTenantSession() {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT set_config('app.current_tenant', '', true)",
                    String.class
            );
            log.trace("Tenant session resetado no PostgreSQL");
        } catch (Exception ex) {
            log.debug("Aviso: Falha ao resetar tenant session: {}", ex.getMessage());
        }
    }

    /**
     * Consulta o valor atualmente registrado em 'app.current_tenant' na conexão.
     *
     * @return String contendo o UUID ou string vazia/nula se não definido
     */
    public String getCurrentTenantSession() {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT current_setting('app.current_tenant', true)",
                    String.class
            );
        } catch (Exception ex) {
            log.debug("Aviso: Falha ao consultar current_setting: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Executa uma ação encapsulada configurando o contexto de tenant na ThreadLocal e no PostgreSQL RLS,
     * restaurando o estado anterior ao término.
     */
    public <T> T executeInTenantSession(UUID tenantId, Supplier<T> action) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(action, "action não pode ser nula");

        UUID previousTenant = TenantContextHolder.get().map(TenantContext::tenantId).orElse(null);
        String previousRole = TenantContextHolder.get().map(TenantContext::role).orElse("UNKNOWN");

        try {
            TenantContextHolder.set(new TenantContext(tenantId, "SESSION_RUNNER"));
            setTenantSession(tenantId);
            return action.get();
        } finally {
            if (previousTenant != null) {
                TenantContextHolder.set(new TenantContext(previousTenant, previousRole));
                setTenantSession(previousTenant);
            } else {
                TenantContextHolder.clear();
                resetTenantSession();
            }
        }
    }

    /**
     * Sobrecarga para executar ações que não retornam valor (Runnable).
     */
    public void runInTenantSession(UUID tenantId, Runnable action) {
        executeInTenantSession(tenantId, () -> {
            action.run();
            return null;
        });
    }
}
