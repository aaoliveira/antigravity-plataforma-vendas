package br.com.sellerintelligence.sync.infrastructure.rabbitmq;

import java.io.Serializable;
import java.util.UUID;

public record SyncJobMessage(
        UUID jobId,
        UUID tenantId,
        int attempt
) implements Serializable {}
