package br.com.sellerintelligence.compliance.web;

import br.com.sellerintelligence.compliance.domain.*;
import br.com.sellerintelligence.compliance.service.*;
import br.com.sellerintelligence.compliance.web.dto.ReactivateRequest;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Administrative REST controller for continuous marketplace compliance governance.
 */
@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceDashboardController {

    private final ComplianceDashboardService dashboardService;
    private final MarketplacePolicyRegistryService policyRegistryService;
    private final PolicyMonitoringEngine monitoringEngine;
    private final ComplianceIncidentService incidentService;
    private final SafeReactivationService reactivationService;
    private final ApiLifecycleMonitor apiLifecycleMonitor;
    private final MarketplacePolicyChangeRepository changeRepository;

    public ComplianceDashboardController(
            ComplianceDashboardService dashboardService,
            MarketplacePolicyRegistryService policyRegistryService,
            PolicyMonitoringEngine monitoringEngine,
            ComplianceIncidentService incidentService,
            SafeReactivationService reactivationService,
            ApiLifecycleMonitor apiLifecycleMonitor,
            MarketplacePolicyChangeRepository changeRepository) {
        this.dashboardService = dashboardService;
        this.policyRegistryService = policyRegistryService;
        this.monitoringEngine = monitoringEngine;
        this.incidentService = incidentService;
        this.reactivationService = reactivationService;
        this.apiLifecycleMonitor = apiLifecycleMonitor;
        this.changeRepository = changeRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }

    @GetMapping("/policies")
    public ResponseEntity<List<MarketplacePolicy>> listPolicies(
            @RequestParam(required = false) Marketplace marketplace,
            @RequestParam(required = false) PolicyStatus status) {
        if (marketplace != null) {
            return ResponseEntity.ok(policyRegistryService.listByMarketplace(marketplace));
        }
        if (status != null) {
            return ResponseEntity.ok(policyRegistryService.listByStatus(status));
        }
        return ResponseEntity.ok(policyRegistryService.listAll());
    }

    @GetMapping("/changes")
    public ResponseEntity<List<MarketplacePolicyChange>> listChanges(
            @RequestParam(required = false) Marketplace marketplace) {
        if (marketplace != null) {
            return ResponseEntity.ok(changeRepository.findByMarketplaceOrderByDetectedAtDesc(marketplace));
        }
        return ResponseEntity.ok(changeRepository.findTop20ByOrderByDetectedAtDesc());
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<ComplianceIncident>> listIncidents(
            @RequestParam(required = false) Marketplace marketplace,
            @RequestParam(required = false) Boolean openOnly) {
        if (Boolean.TRUE.equals(openOnly)) {
            return ResponseEntity.ok(incidentService.listOpenIncidents());
        }
        return ResponseEntity.ok(incidentService.listAll());
    }

    @PostMapping("/incidents/{id}/status")
    public ResponseEntity<ComplianceIncident> updateIncidentStatus(
            @PathVariable UUID id,
            @RequestParam IncidentStatus status,
            @RequestParam(required = false) String notes) {
        String actorId = TenantContext.getCurrentUserId().map(UUID::toString).orElse("ADMIN");
        return ResponseEntity.ok(incidentService.updateStatus(id, status, actorId, notes));
    }

    @GetMapping("/apis")
    public ResponseEntity<List<MarketplaceApiLifecycle>> listApis(
            @RequestParam(required = false) Marketplace marketplace) {
        if (marketplace != null) {
            return ResponseEntity.ok(apiLifecycleMonitor.listByMarketplace(marketplace));
        }
        return ResponseEntity.ok(apiLifecycleMonitor.listAll());
    }

    @PostMapping("/policies/{id}/check")
    public ResponseEntity<Void> checkPolicy(
            @PathVariable UUID id,
            @RequestParam(required = false) String simulatedContent) {
        String actorId = TenantContext.getCurrentUserId().map(UUID::toString).orElse("ADMIN");
        monitoringEngine.checkPolicy(id, simulatedContent, actorId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reactivate")
    public ResponseEntity<SafeReactivationLog> reactivateFeature(@RequestBody ReactivateRequest request) {
        String actorId = TenantContext.getCurrentUserId().map(UUID::toString).orElse("ADMIN_OFFICER");
        SafeReactivationLog result = reactivationService.reactivateFeature(
                request.marketplace(),
                request.featureName(),
                actorId,
                request.evidence(),
                request.policyVersion(),
                request.testsExecuted()
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/reactivations")
    public ResponseEntity<List<SafeReactivationLog>> listReactivations(
            @RequestParam(required = false) Marketplace marketplace) {
        return ResponseEntity.ok(reactivationService.listHistory(marketplace));
    }
}
