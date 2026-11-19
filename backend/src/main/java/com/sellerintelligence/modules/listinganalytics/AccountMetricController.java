package com.sellerintelligence.modules.listinganalytics;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.listinganalytics.model.AccountMetricDto;
import com.sellerintelligence.modules.listinganalytics.model.IngestMetricsCommand;
import com.sellerintelligence.modules.listinganalytics.model.MetricAvailabilityStatus;
import com.sellerintelligence.modules.listinganalytics.model.MetricCatalogEntry;
import com.sellerintelligence.modules.listinganalytics.model.MetricScope;
import com.sellerintelligence.modules.listinganalytics.model.MetricsSummaryDto;
import com.sellerintelligence.modules.listinganalytics.model.OfficialMetricCatalog;
import com.sellerintelligence.modules.listinganalytics.service.AccountMetricIngestionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para catálogo oficial de métricas, ingestão comprovada e consultas por conta conectada.
 * Todo acesso a dados é rigidamente isolado por tenant_id.
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class AccountMetricController {

    private final AccountMetricIngestionService ingestionService;
    private final OfficialMetricCatalog metricCatalog;

    public AccountMetricController(
            AccountMetricIngestionService ingestionService,
            OfficialMetricCatalog metricCatalog
    ) {
        this.ingestionService = ingestionService;
        this.metricCatalog = metricCatalog;
    }

    /**
     * Retorna o catálogo documentado de métricas oficiais suportadas por marketplace.
     */
    @GetMapping("/catalog")
    public ResponseEntity<List<MetricCatalogEntry>> getCatalog(
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) MetricScope scope
    ) {
        List<MetricCatalogEntry> entries = metricCatalog.findByMarketplace(marketplace);
        if (scope != null) {
            entries = entries.stream().filter(e -> e.scope() == scope).toList();
        }
        return ResponseEntity.ok(entries);
    }

    /**
     * Ingesta métricas oficiais e deriva taxas de forma idempotente para a conta informada.
     */
    @PostMapping("/ingest")
    public ResponseEntity<List<AccountMetricDto>> ingestMetrics(
            @Valid @RequestBody IngestMetricsCommand command
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        List<AccountMetricDto> result = ingestionService.ingestMetrics(tenantId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Lista métricas persistidas com filtros combináveis.
     */
    @GetMapping
    public ResponseEntity<Page<AccountMetricDto>> searchMetrics(
            @RequestParam(required = false) UUID sellerAccountId,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) String externalListingId,
            @RequestParam(required = false) MetricScope scope,
            @RequestParam(required = false) String metricKey,
            @RequestParam(required = false) MetricAvailabilityStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Page<AccountMetricDto> page = ingestionService.searchMetrics(
                tenantId,
                sellerAccountId,
                marketplace,
                externalListingId,
                scope,
                metricKey,
                status,
                startDate,
                endDate,
                pageable
        );
        return ResponseEntity.ok(page);
    }

    /**
     * Retorna resumo consolidado de métricas e taxas agregadas no período.
     */
    @GetMapping("/summary")
    public ResponseEntity<MetricsSummaryDto> getSummary(
            @RequestParam(required = false) UUID sellerAccountId,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        MetricsSummaryDto summary = ingestionService.getSummary(
                tenantId,
                sellerAccountId,
                marketplace,
                startDate,
                endDate
        );
        return ResponseEntity.ok(summary);
    }
}
