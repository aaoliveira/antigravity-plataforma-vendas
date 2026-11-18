package com.sellerintelligence.modules.productmonitoring.service;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Serviço responsável por aplicar as políticas de retenção e expurgo histórico de snapshots.
 * Cumpre a regra de aceite: "exclusão segue retenção definida."
 */
@Service
public class MonitoringRetentionService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringRetentionService.class);

    private final MonitoredItemSnapshotRepository snapshotRepository;
    private final AuditPublisher auditPublisher;

    public MonitoringRetentionService(
            MonitoredItemSnapshotRepository snapshotRepository,
            AuditPublisher auditPublisher
    ) {
        this.snapshotRepository = snapshotRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public int purgeExpiredSnapshots(UUID tenantId, int retentionDays) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        int effectiveDays = Math.max(retentionDays, 7);

        Instant cutoff = Instant.now().minus(Duration.ofDays(effectiveDays));
        int deletedCount = snapshotRepository.deleteSnapshotsOlderThan(tenantId, cutoff);

        log.info("Expurgo de snapshots expirados concluído: tenant={}, retenção={} dias, deletados={}",
                tenantId, effectiveDays, deletedCount);

        auditPublisher.publish(
                "MONITORING_SNAPSHOTS_PURGED",
                null,
                "system",
                tenantId,
                null,
                null,
                null,
                String.format("Expurgo de retenção: %d snapshots anteriores a %s removidos.", deletedCount, cutoff)
        );

        return deletedCount;
    }
}
