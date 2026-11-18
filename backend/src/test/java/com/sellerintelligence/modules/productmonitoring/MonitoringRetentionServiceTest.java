package com.sellerintelligence.modules.productmonitoring;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemSnapshotRepository;
import com.sellerintelligence.modules.productmonitoring.service.MonitoringRetentionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringRetentionServiceTest {

    @Mock
    private MonitoredItemSnapshotRepository snapshotRepository;

    @Mock
    private AuditPublisher auditPublisher;

    private MonitoringRetentionService retentionService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        retentionService = new MonitoringRetentionService(snapshotRepository, auditPublisher);
    }

    @Test
    @DisplayName("Deve expurgar snapshots mais antigos que a retenção definida e registrar evento auditável")
    void shouldPurgeExpiredSnapshots() {
        when(snapshotRepository.deleteSnapshotsOlderThan(eq(tenantId), any(Instant.class))).thenReturn(42);

        int deleted = retentionService.purgeExpiredSnapshots(tenantId, 90);

        assertThat(deleted).isEqualTo(42);
        verify(snapshotRepository).deleteSnapshotsOlderThan(eq(tenantId), any(Instant.class));
        verify(auditPublisher).publish(
                eq("MONITORING_SNAPSHOTS_PURGED"),
                any(),
                eq("system"),
                eq(tenantId),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Deve forçar piso mínimo de 7 dias de retenção para segurança legal de auditoria")
    void shouldEnforceMinimumRetentionDays() {
        when(snapshotRepository.deleteSnapshotsOlderThan(eq(tenantId), any(Instant.class))).thenReturn(0);

        int deleted = retentionService.purgeExpiredSnapshots(tenantId, 2); // Solicita 2 dias

        assertThat(deleted).isEqualTo(0);
        verify(snapshotRepository).deleteSnapshotsOlderThan(eq(tenantId), any(Instant.class));
    }
}
