package br.com.sellerintelligence.tenancy.domain;

import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

    private static final String MDC_TENANT_ID_KEY = "tenant_id";
    private static final ThreadLocal<TenantInfo> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenant(TenantInfo tenantInfo) {
        if (tenantInfo != null) {
            CURRENT_TENANT.set(tenantInfo);
            if (tenantInfo.tenantId() != null) {
                MDC.put(MDC_TENANT_ID_KEY, tenantInfo.tenantId().toString());
            }
        }
    }

    public static Optional<TenantInfo> getCurrentTenant() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static UUID getTenantId() {
        TenantInfo info = CURRENT_TENANT.get();
        if (info == null || info.tenantId() == null) {
            throw new TenantAccessDeniedException("No active tenant context found for the current operation");
        }
        return info.tenantId();
    }

    public static Optional<UUID> findTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get()).map(TenantInfo::tenantId);
    }

    public static Optional<Role> getCurrentRole() {
        return Optional.ofNullable(CURRENT_TENANT.get()).map(TenantInfo::role);
    }

    public static Optional<UUID> getCurrentUserId() {
        return Optional.ofNullable(CURRENT_TENANT.get()).map(TenantInfo::userId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        MDC.remove(MDC_TENANT_ID_KEY);
    }

    public record TenantInfo(
            UUID tenantId,
            UUID userId,
            String userSub,
            String userEmail,
            Role role
    ) {
    }
}
