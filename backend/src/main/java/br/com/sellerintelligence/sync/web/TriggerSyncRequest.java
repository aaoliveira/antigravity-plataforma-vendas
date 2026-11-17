package br.com.sellerintelligence.sync.web;

import br.com.sellerintelligence.sync.domain.SyncResourceType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TriggerSyncRequest(
        @NotNull(message = "storeId is required")
        UUID storeId,

        @NotNull(message = "resourceType is required")
        SyncResourceType resourceType
) {}
