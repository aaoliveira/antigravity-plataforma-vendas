package com.sellerintelligence.modules.productmonitoring;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.BusinessRuleException;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemRecord;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemSnapshotRecord;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemSnapshotDto;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemSnapshotRepository;
import com.sellerintelligence.modules.productmonitoring.service.MonitoredItemSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoredItemSnapshotServiceTest {

    @Mock
    private MonitoredItemRepository itemRepository;

    @Mock
    private MonitoredItemSnapshotRepository snapshotRepository;

    @Mock
    private AuditPublisher auditPublisher;

    private MonitoredItemSnapshotService snapshotService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        snapshotService = new MonitoredItemSnapshotService(itemRepository, snapshotRepository, auditPublisher);
    }

    @Test
    @DisplayName("Deve registrar primeiro snapshot efetivo após inclusão e atualizar firstSnapshotAt do item")
    void shouldRecordFirstEffectiveSnapshot() {
        Instant itemCreation = Instant.now().minusSeconds(3600); // Criado há 1 hora
        MonitoredItemRecord item = new MonitoredItemRecord(
                itemId, tenantId, groupId, "MERCADO_LIVRE", "POL-1", "MLB-101",
                "Smartphone", new BigDecimal("3499.00"), new BigDecimal("3499.00"), "BRL",
                null, null, "NEW", MonitoredItemStatus.ACTIVE, 60, null, Instant.now(),
                null, null, itemCreation, itemCreation
        );

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(snapshotRepository.save(any(MonitoredItemSnapshotRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant snapshotObserved = Instant.now();
        MonitoredItemSnapshotDto snapshot = snapshotService.recordSnapshot(
                tenantId, itemId, new BigDecimal("3299.00"), new BigDecimal("0.00"), 10, "ACTIVE", snapshotObserved
        );

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.price()).isEqualByComparingTo("3299.00");
        assertThat(snapshot.shippingStatus()).isEqualTo("FREE");
        assertThat(snapshot.isEffectiveSnapshot()).isTrue();

        // O item agora tem firstSnapshotAt populado
        assertThat(item.getFirstSnapshotAt()).isEqualTo(snapshotObserved);
        assertThat(item.getCurrentPrice()).isEqualByComparingTo("3299.00");
        assertThat(item.getLastSnapshotAt()).isEqualTo(snapshotObserved);

        verify(itemRepository).save(item);
    }

    @Test
    @DisplayName("Regra Anti-Retroatividade: deve rejeitar snapshot com data anterior à inclusão do item")
    void shouldRejectRetroactiveSnapshotBeforeItemCreation() {
        Instant itemCreation = Instant.now().minusSeconds(100);
        MonitoredItemRecord item = new MonitoredItemRecord(
                itemId, tenantId, groupId, "MERCADO_LIVRE", "POL-1", "MLB-101",
                "Smartphone", new BigDecimal("3499.00"), new BigDecimal("3499.00"), "BRL",
                null, null, "NEW", MonitoredItemStatus.ACTIVE, 60, null, Instant.now(),
                null, null, itemCreation, itemCreation
        );

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));

        // Tenta registrar snapshot forjado de 10 dias atrás
        Instant retroactiveDate = Instant.now().minusSeconds(10 * 24 * 3600);

        assertThatThrownBy(() -> snapshotService.recordSnapshot(
                tenantId, itemId, new BigDecimal("2999.00"), null, 5, "ACTIVE", retroactiveDate
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("não pode ser anterior à data de inclusão do item");
    }

    @Test
    @DisplayName("Deve consultar snapshots filtrando estritamente a partir da data de inclusão (sem histórico retroativo)")
    void shouldRetrieveSnapshotsSinceInclusion() {
        Instant inclusion = Instant.now().minusSeconds(3600);
        MonitoredItemRecord item = new MonitoredItemRecord(
                itemId, tenantId, groupId, "MERCADO_LIVRE", "POL-1", "MLB-101",
                "Smartphone", new BigDecimal("3499.00"), new BigDecimal("3499.00"), "BRL",
                null, null, "NEW", MonitoredItemStatus.ACTIVE, 60, null, Instant.now(),
                inclusion, null, inclusion, inclusion
        );

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));

        MonitoredItemSnapshotRecord s1 = new MonitoredItemSnapshotRecord(
                UUID.randomUUID(), tenantId, itemId, groupId, "MERCADO_LIVRE", "MLB-101",
                new BigDecimal("3499.00"), "BRL", null, "UNKNOWN", 5, "ACTIVE",
                "OBSERVED", Instant.now(), inclusion.plusSeconds(60), true, Instant.now()
        );

        when(snapshotRepository.findSnapshotsSinceInclusion(any(), any(), any()))
                .thenReturn(List.of(s1));

        List<MonitoredItemSnapshotDto> snapshots = snapshotService.getItemSnapshots(tenantId, itemId);

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).price()).isEqualByComparingTo("3499.00");
    }
}
