package br.com.sellerintelligence.sync.web;

import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.sync.domain.SyncJob;
import br.com.sellerintelligence.sync.domain.SyncTriggerType;
import br.com.sellerintelligence.sync.service.SyncJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync/jobs")
public class SyncJobController {

    private final SyncJobService syncJobService;

    public SyncJobController(SyncJobService syncJobService) {
        this.syncJobService = syncJobService;
    }

    @PostMapping
    public ResponseEntity<SyncJobResponse> triggerSync(@Valid @RequestBody TriggerSyncRequest request) {
        SyncJob job = syncJobService.triggerSync(request.storeId(), request.resourceType(), SyncTriggerType.MANUAL);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SyncJobResponse.from(job));
    }

    @GetMapping
    public ResponseEntity<List<SyncJobResponse>> listJobs() {
        UUID tenantId = TenantContext.getTenantId();
        List<SyncJobResponse> responses = syncJobService.listJobs(tenantId).stream()
                .map(SyncJobResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<SyncJobResponse> getJob(@PathVariable UUID jobId) {
        UUID tenantId = TenantContext.getTenantId();
        SyncJob job = syncJobService.getJob(jobId, tenantId);
        return ResponseEntity.ok(SyncJobResponse.from(job));
    }

    @PostMapping("/{jobId}/retry")
    public ResponseEntity<SyncJobResponse> retryJob(@PathVariable UUID jobId) {
        UUID tenantId = TenantContext.getTenantId();
        SyncJob job = syncJobService.retryJob(jobId, tenantId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SyncJobResponse.from(job));
    }
}
