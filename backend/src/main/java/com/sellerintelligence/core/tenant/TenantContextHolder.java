package com.sellerintelligence.core.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * Armazena o contexto do tenant na ThreadLocal da requisição atual.
 * Sempre deve ser limpo ao final do ciclo de vida da requisição (via finally no filtro web).
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext context) {
        CONTEXT.set(context);
    }

    public static Optional<TenantContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static UUID getRequiredTenantId() {
        return get()
                .map(TenantContext::tenantId)
                .orElseThrow(() -> new TenantContextMissingException(
                        "Nenhum tenant ativo associado ao contexto da requisição. Operação tenant-scoped negada."
                ));
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
