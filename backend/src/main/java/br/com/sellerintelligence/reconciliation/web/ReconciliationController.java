package br.com.sellerintelligence.reconciliation.web;

import br.com.sellerintelligence.reconciliation.domain.*;
import br.com.sellerintelligence.reconciliation.service.*;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliation")
public class ReconciliationController {

    private final SettlementIngestionService ingestionService;
    private final ManualReconciliationService manualService;
    private final DreCalculationService dreCalculationService;
    private final ReconciliationSettlementRepository settlementRepository;
    private final ReconciliationSettlementItemRepository itemRepository;

    public ReconciliationController(SettlementIngestionService ingestionService,
                                  ManualReconciliationService manualService,
                                  DreCalculationService dreCalculationService,
                                  ReconciliationSettlementRepository settlementRepository,
                                  ReconciliationSettlementItemRepository itemRepository) {
        this.ingestionService = ingestionService;
        this.manualService = manualService;
        this.dreCalculationService = dreCalculationService;
        this.settlementRepository = settlementRepository;
        this.itemRepository = itemRepository;
    }

    // 1. Ingestão Idempotente de Repasses
    @PostMapping("/settlements/import")
    public ResponseEntity<ReconciliationSettlement> importSettlement(
            @Valid @RequestBody SettlementIngestionService.SettlementIngestRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        ReconciliationSettlement settlement = ingestionService.ingestSettlement(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(settlement);
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<ReconciliationSettlement>> listSettlements() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(settlementRepository.findByTenantIdOrderBySettlementDateDesc(tenantId));
    }

    @GetMapping("/settlements/{id}/items")
    public ResponseEntity<List<ReconciliationSettlementItem>> listSettlementItems(
            @PathVariable UUID id,
            @RequestParam(required = false) ReconciliationItemStatus status) {
        UUID tenantId = TenantContext.getTenantId();
        List<ReconciliationSettlementItem> items = itemRepository.findBySettlementIdAndTenantId(id, tenantId);
        if (status != null) {
            items = items.stream().filter(i -> i.getReconciliationStatus() == status).toList();
        }
        return ResponseEntity.ok(items);
    }

    // 2. Ações Manuais com Auditoria
    @PostMapping("/items/{id}/associate")
    public ResponseEntity<ReconciliationSettlementItem> associateOrder(
            @PathVariable UUID id,
            @Valid @RequestBody AssociateOrderRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        ReconciliationSettlementItem item = manualService.manuallyAssociateOrder(
                tenantId, id, request.orderId(), request.justification(), null
        );
        return ResponseEntity.ok(item);
    }

    @PostMapping("/items/{id}/ignore-discrepancy")
    public ResponseEntity<ReconciliationSettlementItem> ignoreDiscrepancy(
            @PathVariable UUID id,
            @Valid @RequestBody IgnoreDiscrepancyRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        ReconciliationSettlementItem item = manualService.ignoreDiscrepancy(
                tenantId, id, request.justification(), null
        );
        return ResponseEntity.ok(item);
    }

    @PostMapping("/items/{id}/dispute")
    public ResponseEntity<ReconciliationDispute> logDispute(
            @PathVariable UUID id,
            @Valid @RequestBody LogDisputeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        ReconciliationDispute dispute = manualService.logDispute(
                tenantId, id, request.protocolNumber(), request.marketplace(),
                request.disputedAmount(), request.channel(), request.notes(), null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dispute);
    }

    @GetMapping("/disputes")
    public ResponseEntity<List<ReconciliationDispute>> listDisputes() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(manualService.listDisputes(tenantId));
    }

    @GetMapping("/items/{id}/audit")
    public ResponseEntity<List<ReconciliationAuditLog>> listAuditLogs(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(manualService.listAuditLogs(tenantId, id));
    }

    // 3. DRE Gerencial
    @GetMapping("/dre")
    public ResponseEntity<DreCalculationService.ManagerialDreReportDto> getDre(
            @RequestParam(defaultValue = "ACCRUAL") DreAccountingBasis basis,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(required = false) String marketplace) {
        UUID tenantId = TenantContext.getTenantId();
        Instant end = endDate != null ? endDate : Instant.now();
        Instant start = startDate != null ? startDate : end.minus(30, ChronoUnit.DAYS);

        DreCalculationService.ManagerialDreReportDto report = dreCalculationService.calculateDre(
                tenantId, basis, start, end, marketplace
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/dre/export")
    public ResponseEntity<String> exportDreCsv(
            @RequestParam(defaultValue = "ACCRUAL") DreAccountingBasis basis,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(required = false) String marketplace) {
        UUID tenantId = TenantContext.getTenantId();
        Instant end = endDate != null ? endDate : Instant.now();
        Instant start = startDate != null ? startDate : end.minus(30, ChronoUnit.DAYS);

        DreCalculationService.ManagerialDreReportDto report = dreCalculationService.calculateDre(
                tenantId, basis, start, end, marketplace
        );
        String csv = dreCalculationService.exportDreToCsv(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dre_gerencial_" + basis.name().toLowerCase() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
