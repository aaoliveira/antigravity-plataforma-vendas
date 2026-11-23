package com.sellerintelligence.modules.financialanalytics.aggregation;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST read-only para consulta de KPIs agregados, resumos gerenciais,
 * visualização periódica e por canal, com cache tenant-aware e catálogo documentado.
 *
 * Não contém lógica de cálculo financeiro: todas as agregações são delegadas ao AggregatedKpiService.
 */
@RestController
@RequestMapping("/api/v1/analytics/kpis")
public class AggregatedKpiController {

    private final AggregatedKpiService kpiService;

    public AggregatedKpiController(AggregatedKpiService kpiService) {
        this.kpiService = kpiService;
    }

    /**
     * Resumo consolidado de KPIs do tenant com filtros combináveis e cache tenant-aware.
     */
    @GetMapping("/summary")
    public ResponseEntity<AggregatedKpiSummaryDto> getSummary(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) UUID sellerAccountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        AggregatedKpiFilter filter = new AggregatedKpiFilter(companyId, marketplace, sellerAccountId, startDate, endDate);
        AggregatedKpiSummaryDto summary = kpiService.getAggregatedSummary(tenantId, filter);
        return ResponseEntity.ok(summary);
    }

    /**
     * Agregação analítica comparativa por Canal / Marketplace.
     */
    @GetMapping("/by-channel")
    public ResponseEntity<List<ChannelKpiAggregateDto>> getByChannel(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID sellerAccountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        AggregatedKpiFilter filter = new AggregatedKpiFilter(companyId, null, sellerAccountId, startDate, endDate);
        List<ChannelKpiAggregateDto> list = kpiService.getKpisByChannel(tenantId, filter);
        return ResponseEntity.ok(list);
    }

    /**
     * Agregação analítica periódica (diária, semanal, mensal) para gráficos de tendência.
     */
    @GetMapping("/by-period")
    public ResponseEntity<List<PeriodicKpiAggregateDto>> getByPeriod(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) UUID sellerAccountId,
            @RequestParam(required = false, defaultValue = "DAILY") KpiGranularity granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        AggregatedKpiFilter filter = new AggregatedKpiFilter(companyId, marketplace, sellerAccountId, startDate, endDate);
        List<PeriodicKpiAggregateDto> list = kpiService.getKpisByPeriod(tenantId, filter, granularity);
        return ResponseEntity.ok(list);
    }

    /**
     * Agregação analítica comparativa por Empresa / Unidade de Negócio.
     */
    @GetMapping("/by-company")
    public ResponseEntity<List<CompanyKpiAggregateDto>> getByCompany(
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) UUID sellerAccountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        AggregatedKpiFilter filter = new AggregatedKpiFilter(null, marketplace, sellerAccountId, startDate, endDate);
        List<CompanyKpiAggregateDto> list = kpiService.getKpisByCompany(tenantId, filter);
        return ResponseEntity.ok(list);
    }

    /**
     * Retorna o catálogo oficial com definições, fórmulas e proveniência de cada KPI.
     */
    @GetMapping("/definitions")
    public ResponseEntity<List<KpiDefinitionDto>> getDefinitions() {
        List<KpiDefinitionDto> definitions = kpiService.getKpiDefinitions();
        return ResponseEntity.ok(definitions);
    }

    /**
     * Invalidação sob demanda do cache do tenant autenticado.
     */
    @PostMapping("/cache/invalidate")
    public ResponseEntity<Void> invalidateCache() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        kpiService.invalidateTenantCache(tenantId);
        return ResponseEntity.noContent().build();
    }
}
