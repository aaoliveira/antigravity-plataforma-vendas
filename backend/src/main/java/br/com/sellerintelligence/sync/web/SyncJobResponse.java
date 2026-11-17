package br.com.sellerintelligence.sync.web;

import br.com.sellerintelligence.sync.domain.SyncJob;
import br.com.sellerintelligence.sync.domain.SyncResourceType;
import br.com.sellerintelligence.sync.domain.SyncStatus;
import br.com.sellerintelligence.sync.domain.SyncTriggerType;
import br.com.sellerintelligence.tenancy.store.Marketplace;

import java.time.Instant;
import java.util.UUID;

public record SyncJobResponse(
        UUID id,
        UUID storeId,
        Marketplace marketplace,
        SyncResourceType resourceType,
        SyncTriggerType triggerType,
        SyncStatus status,
        int itemsProcessed,
        int itemsSucceeded,
        int itemsFailed,
        String errorSummary,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {
    public static SyncJobResponse from(SyncJob job) {
        return new SyncJobResponse(
                job.getId(),
                job.getStoreId(),
                job.getMarketplace(),
                job.getResourceType(),
                job.getTriggerType(),
                job.getStatus(),
                job.getItemsProcessed(),
                job.getItemsSucceeded(),
                job.getItemsFailed(),
                job.getErrorSummary(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }
}
