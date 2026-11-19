package com.sellerintelligence.modules.financialanalytics;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.financialanalytics.model.FinancialFactSummaryDto;
import com.sellerintelligence.modules.financialanalytics.model.OrderFinancialFactDto;
import com.sellerintelligence.modules.financialanalytics.model.ReconcileSettlementCommand;
import com.sellerintelligence.modules.financialanalytics.model.ReconciliationStatus;
import com.sellerintelligence.modules.financialanalytics.service.OrderFinancialFactService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * REST Controller para Fatos Financeiros por Pedido e Item, Reconciliação Externa e Métricas Consolidadas.
 * Todos os dados são rigorosamente escopados por tenant_id via RLS e contexto seguro.
 */
@RestController
@RequestMapping("/api/v1/financial-facts")
public class OrderFinancialFactController {

    private final OrderFinancialFactService factService;

    public OrderFinancialFactController(OrderFinancialFactService factService) {
        this.factService = factService;
    }

    /**
     * Calcula deterministamente ou reprocessa os fatos financeiros de um pedido de forma idempotente.
     */
    @PostMapping("/orders/{orderId}/compute")
    public ResponseEntity<OrderFinancialFactDto> computeOrderFacts(@PathVariable UUID orderId) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        OrderFinancialFactDto dto = factService.computeOrReprocessOrderFacts(tenantId, orderId);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    /**
     * Obtém os fatos financeiros consolidados e granulares de um pedido específico.
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderFinancialFactDto> getOrderFacts(@PathVariable UUID orderId) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        OrderFinancialFactDto dto = factService.getOrderFinancialFacts(tenantId, orderId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Reconcilia os fatos financeiros com um extrato de liquidação externa do canal.
     */
    @PostMapping("/orders/{orderId}/reconcile")
    public ResponseEntity<OrderFinancialFactDto> reconcileSettlement(
            @PathVariable UUID orderId,
            @Valid @RequestBody ReconcileSettlementCommand command
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        OrderFinancialFactDto dto = factService.reconcileWithExternalSettlement(tenantId, orderId, command);
        return ResponseEntity.ok(dto);
    }

    /**
     * Lista fatos financeiros com filtros e paginação.
     */
    @GetMapping
    public ResponseEntity<Page<OrderFinancialFactDto>> listFacts(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) ReconciliationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Page<OrderFinancialFactDto> page = factService.listFinancialFacts(
                tenantId, companyId, marketplace, status, startDate, endDate, pageable
        );
        return ResponseEntity.ok(page);
    }

    /**
     * Resumo consolidado de métricas financeiras agregadas (Margem de Contribuição, deduções, etc.).
     */
    @GetMapping("/summary")
    public ResponseEntity<FinancialFactSummaryDto> getSummary(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        FinancialFactSummaryDto summary = factService.getFinancialSummary(
                tenantId, companyId, marketplace, startDate, endDate
        );
        return ResponseEntity.ok(summary);
    }
}
