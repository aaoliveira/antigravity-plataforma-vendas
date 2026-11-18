package com.sellerintelligence.modules.productmonitoring;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.productmonitoring.model.AddMonitoredItemRequest;
import com.sellerintelligence.modules.productmonitoring.model.CreateMonitoringGroupRequest;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemDto;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemSnapshotDto;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupDto;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringJobExecutionDto;
import com.sellerintelligence.modules.productmonitoring.model.PageResponse;
import com.sellerintelligence.modules.productmonitoring.model.TriggerSnapshotRequest;
import com.sellerintelligence.modules.productmonitoring.model.UpdateMonitoringGroupRequest;
import com.sellerintelligence.modules.productmonitoring.service.MonitoredItemService;
import com.sellerintelligence.modules.productmonitoring.service.MonitoredItemSnapshotService;
import com.sellerintelligence.modules.productmonitoring.service.MonitoringGroupService;
import com.sellerintelligence.modules.productmonitoring.service.MonitoringJobExecutionService;
import com.sellerintelligence.modules.productmonitoring.service.MonitoringSchedulerPolicyValidator;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST para gestão de Grupos de Monitoramento, Itens Monitorados
 * e Snapshots Históricos com isolamento estrito por tenant.
 */
@RestController
@RequestMapping("/api/v1/monitoring")
public class ProductMonitoringController {

    private final MonitoringGroupService groupService;
    private final MonitoredItemService itemService;
    private final MonitoredItemSnapshotService snapshotService;
    private final MonitoringJobExecutionService jobExecutionService;
    private final MonitoringSchedulerPolicyValidator policyValidator;

    public ProductMonitoringController(
            MonitoringGroupService groupService,
            MonitoredItemService itemService,
            MonitoredItemSnapshotService snapshotService,
            MonitoringJobExecutionService jobExecutionService,
            MonitoringSchedulerPolicyValidator policyValidator
    ) {
        this.groupService = groupService;
        this.itemService = itemService;
        this.snapshotService = snapshotService;
        this.jobExecutionService = jobExecutionService;
        this.policyValidator = policyValidator;
    }

    // --- GRUPOS DE MONITORAMENTO ---

    @PostMapping("/groups")
    public ResponseEntity<MonitoringGroupDto> createGroup(
            @Valid @RequestBody CreateMonitoringGroupRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        MonitoringGroupDto created = groupService.createGroup(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/groups")
    public ResponseEntity<PageResponse<MonitoringGroupDto>> listGroups(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<MonitoringGroupDto> results = groupService.listGroups(
                tenantId, search, PageRequest.of(safePage, safeSize)
        );
        return ResponseEntity.ok(PageResponse.of(results));
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<MonitoringGroupDto> getGroup(
            @PathVariable("id") UUID id
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        MonitoringGroupDto dto = groupService.getGroup(tenantId, id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<MonitoringGroupDto> updateGroup(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateMonitoringGroupRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        MonitoringGroupDto updated = groupService.updateGroup(tenantId, id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable("id") UUID id
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        groupService.deleteGroup(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // --- ITENS MONITORADOS ---

    @PostMapping("/groups/{groupId}/items")
    public ResponseEntity<MonitoredItemDto> addItemToGroup(
            @PathVariable("groupId") UUID groupId,
            @Valid @RequestBody AddMonitoredItemRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        MonitoredItemDto created = itemService.addItemToGroup(tenantId, groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/groups/{groupId}/items")
    public ResponseEntity<PageResponse<MonitoredItemDto>> listItemsInGroup(
            @PathVariable("groupId") UUID groupId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<MonitoredItemDto> results = itemService.listItemsInGroup(
                tenantId, groupId, PageRequest.of(safePage, safeSize)
        );
        return ResponseEntity.ok(PageResponse.of(results));
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<MonitoredItemDto> getItem(
            @PathVariable("itemId") UUID itemId
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        MonitoredItemDto dto = itemService.getItem(tenantId, itemId);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/groups/{groupId}/items/{itemId}")
    public ResponseEntity<Void> removeItemFromGroup(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("itemId") UUID itemId
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        itemService.removeItemFromGroup(tenantId, groupId, itemId);
        return ResponseEntity.noContent().build();
    }

    // --- SNAPSHOTS HISTÓRICOS & EXECUÇÃO DE JOBS ---

    @GetMapping("/items/{itemId}/snapshots")
    public ResponseEntity<List<MonitoredItemSnapshotDto>> getItemSnapshots(
            @PathVariable("itemId") UUID itemId
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        List<MonitoredItemSnapshotDto> snapshots = snapshotService.getItemSnapshots(tenantId, itemId);
        return ResponseEntity.ok(snapshots);
    }

    @PostMapping("/items/{itemId}/trigger-snapshot")
    public ResponseEntity<MonitoringJobExecutionService.ExecutionResult> triggerSnapshot(
            @PathVariable("itemId") UUID itemId,
            @RequestBody(required = false) TriggerSnapshotRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        var result = jobExecutionService.executeItemSnapshot(tenantId, itemId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/executions")
    public ResponseEntity<PageResponse<MonitoringJobExecutionDto>> listExecutions(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<MonitoringJobExecutionDto> results = jobExecutionService.listExecutions(
                tenantId, PageRequest.of(safePage, safeSize)
        );
        return ResponseEntity.ok(PageResponse.of(results));
    }

    @PostMapping("/admin/kill-switch")
    public ResponseEntity<Map<String, Object>> setKillSwitch(
            @RequestBody Map<String, Object> body
    ) {
        String marketplace = (String) body.get("marketplace");
        Boolean active = (Boolean) body.get("active");
        if (marketplace != null && active != null) {
            policyValidator.setKillSwitch(marketplace, active);
            return ResponseEntity.ok(Map.of("marketplace", marketplace, "active", active));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "marketplace e active são obrigatórios"));
    }
}
