package com.sellerintelligence.modules.financialanalytics.aggregation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener de eventos para invalidação automática de cache de KPIs.
 * Escuta mutações financeiras (cálculo de fatos, conciliação de liquidações)
 * e invalida seletivamente o cache do tenant correspondente.
 */
@Component
public class KpiCacheInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(KpiCacheInvalidationListener.class);
    private final TenantAwareKpiCacheService cacheService;

    public KpiCacheInvalidationListener(TenantAwareKpiCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @EventListener
    public void onFinancialFactMutated(FinancialFactMutatedEvent event) {
        if (event == null || event.tenantId() == null) return;
        log.info("Evento de mutação financeira recebido para tenant={}: tipo={}. Invalidando cache de KPIs.",
                event.tenantId(), event.mutationType());
        cacheService.invalidateTenant(event.tenantId());
    }
}
