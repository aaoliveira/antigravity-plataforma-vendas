package com.sellerintelligence.modules.productmonitoring;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.BusinessRuleException;
import com.sellerintelligence.core.exception.SourceUnavailableException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    private MonitoredItemRecord createDefaultItem(Instant createdAt) {
        return new MonitoredItemRecord(
                itemId, tenantId, groupId, "MERCADO_LIVRE", "POL-1", "MLB-101",
                "Smartphone", new BigDecimal("3499.00"), new BigDecimal("3499.00"), "BRL",
                "https://mercadolivre.com.br/item/101", "Oficial Store", "NEW",
                MonitoredItemStatus.ACTIVE, 60, null, Instant.now(),
                null, null, createdAt, createdAt
        );
    }

    @Test
    @DisplayName("Deve registrar primeiro snapshot efetivo após inclusão e marcar changeType como INITIAL")
    void shouldRecordFirstEffectiveSnapshot() {
        Instant itemCreation = Instant.now().minus(1, ChronoUnit.HOURS);
        MonitoredItemRecord item = createDefaultItem(itemCreation);

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(snapshotRepository.findByTenantIdAndIdempotencyKey(eq(tenantId), any())).thenReturn(Optional.empty());
        when(snapshotRepository.findTopByTenantIdAndItemIdAndObservedAtLessThanEqualOrderByObservedAtDesc(eq(tenantId), eq(itemId), any()))
                .thenReturn(Optional.empty());
        when(snapshotRepository.save(any(MonitoredItemSnapshotRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant snapshotObserved = Instant.now();
        MonitoredItemSnapshotDto snapshot = snapshotService.recordSnapshot(
                tenantId, itemId, new BigDecimal("3299.00"), new BigDecimal("0.00"), 10, "ACTIVE", snapshotObserved
        );

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.price()).isEqualByComparingTo("3299.00");
        assertThat(snapshot.shippingStatus()).isEqualTo("FREE");
        assertThat(snapshot.isEffectiveSnapshot()).isTrue();
        assertThat(snapshot.changeType()).isEqualTo("INITIAL");
        assertThat(snapshot.hasPriceChange()).isFalse();
        assertThat(snapshot.priceChangeAmount()).isEqualByComparingTo("0.0000");
        assertThat(snapshot.previousSnapshotId()).isNull();

        assertThat(item.getFirstSnapshotAt()).isEqualTo(snapshotObserved);
        assertThat(item.getCurrentPrice()).isEqualByComparingTo("3299.00");
        assertThat(item.getLastSnapshotAt()).isEqualTo(snapshotObserved);

        verify(itemRepository).save(item);
        verify(snapshotRepository).save(any(MonitoredItemSnapshotRecord.class));
    }

    @Test
    @DisplayName("Idempotência: Deve retornar snapshot existente sem duplicar gravação quando chave idêntica for enviada")
    void shouldReturnExistingSnapshotOnDuplicateIdempotencyKey() {
        Instant itemCreation = Instant.now().minus(2, ChronoUnit.HOURS);
        MonitoredItemRecord item = createDefaultItem(itemCreation);

        String customKey = "batch-20261118-001";
        MonitoredItemSnapshotRecord existingRecord = new MonitoredItemSnapshotRecord(
                UUID.randomUUID(), tenantId, itemId, groupId, "MERCADO_LIVRE", "MLB-101",
                new BigDecimal("2999.00"), "BRL", BigDecimal.ZERO, "FREE", 15, "ACTIVE",
                "OBSERVED", Instant.now(), Instant.now(), Instant.now().plus(60, ChronoUnit.MINUTES),
                true, customKey, "https://mercadolivre.com.br/item/101", "OFFICIAL_API", "SUCCESS",
                null, false, BigDecimal.ZERO, BigDecimal.ZERO, false, false, "INITIAL", Instant.now()
        );

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(snapshotRepository.findByTenantIdAndIdempotencyKey(tenantId, customKey))
                .thenReturn(Optional.of(existingRecord));

        MonitoredItemSnapshotDto result = snapshotService.recordSnapshot(
                tenantId, itemId, new BigDecimal("2999.00"), BigDecimal.ZERO, 15, "ACTIVE",
                Instant.now(), customKey, null, null, null, false
        );

        assertThat(result.id()).isEqualTo(existingRecord.getId());
        assertThat(result.price()).isEqualByComparingTo("2999.00");
        assertThat(result.idempotencyKey()).isEqualTo(customKey);

        verify(snapshotRepository, never()).save(any());
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Change Detection: Deve detectar aumento de preço (PRICE_INCREASE) relativo ao snapshot anterior")
    void shouldDetectPriceIncreaseRelativeToPreviousSnapshot() {
        Instant itemCreation = Instant.now().minus(2, ChronoUnit.HOURS);
        MonitoredItemRecord item = createDefaultItem(itemCreation);

        UUID prevId = UUID.randomUUID();
        MonitoredItemSnapshotRecord prevSnapshot = new MonitoredItemSnapshotRecord(
                prevId, tenantId, itemId, groupId, "MERCADO_LIVRE", "MLB-101",
                new BigDecimal("100.00"), "BRL", BigDecimal.ZERO, "FREE", 5, "ACTIVE",
                "OBSERVED", Instant.now().minus(60, ChronoUnit.MINUTES),
                Instant.now().minus(60, ChronoUnit.MINUTES), true, Instant.now()
        );

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(snapshotRepository.findByTenantIdAndIdempotencyKey(eq(tenantId), any())).thenReturn(Optional.empty());
        when(snapshotRepository.findTopByTenantIdAndItemIdAndObservedAtLessThanEqualOrderByObservedAtDesc(eq(tenantId), eq(itemId), any()))
                .thenReturn(Optional.of(prevSnapshot));
        when(snapshotRepository.save(any(MonitoredItemSnapshotRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MonitoredItemSnapshotDto snapshot = snapshotService.recordSnapshot(
                tenantId, itemId, new BigDecimal("125.00"), BigDecimal.ZERO, 5, "ACTIVE", Instant.now()
        );

        assertThat(snapshot.changeType()).isEqualTo("PRICE_INCREASE");
        assertThat(snapshot.hasPriceChange()).isTrue();
        assertThat(snapshot.priceChangeAmount()).isEqualByComparingTo("25.0000");
        assertThat(snapshot.priceChangePercent()).isEqualByComparingTo("25.0000");
        assertThat(snapshot.previousSnapshotId()).isEqualTo(prevId);
    }

    @Test
    @DisplayName("Change Detection: Deve detectar redução de preço (PRICE_DECREASE) relativo ao snapshot anterior")
    void shouldDetectPriceDecreaseRelativeToPreviousSnapshot() {
        Instant itemCreation = Instant.now().minus(2, ChronoUnit.HOURS);
        MonitoredItemRecord item = createDefaultItem(itemCreation);

        UUID prevId = UUID.randomUUID();
        MonitoredItemSnapshotRecord prevSnapshot = new MonitoredItemSnapshotRecord(
                prevId, tenantId, itemId, groupId, "MERCADO_LIVRE", "MLB-101",
                new BigDecimal("200.00"), "BRL", null, "UNKNOWN", 10, "ACTIVE",
                "OBSERVED", Instant.now().minus(30, ChronoUnit.MINUTES),
                Instant.now().minus(30, ChronoUnit.MINUTES), true, Instant.now()
        );

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(snapshotRepository.findByTenantIdAndIdempotencyKey(eq(tenantId), any())).thenReturn(Optional.empty());
        when(snapshotRepository.findTopByTenantIdAndItemIdAndObservedAtLessThanEqualOrderByObservedAtDesc(eq(tenantId), eq(itemId), any()))
                .thenReturn(Optional.of(prevSnapshot));
        when(snapshotRepository.save(any(MonitoredItemSnapshotRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MonitoredItemSnapshotDto snapshot = snapshotService.recordSnapshot(
                tenantId, itemId, new BigDecimal("150.00"), null, 10, "ACTIVE", Instant.now()
        );

        assertThat(snapshot.changeType()).isEqualTo("PRICE_DECREASE");
        assertThat(snapshot.hasPriceChange()).isTrue();
        assertThat(snapshot.priceChangeAmount()).isEqualByComparingTo("-50.0000");
        assertThat(snapshot.priceChangePercent()).isEqualByComparingTo("-25.0000");
        assertThat(snapshot.previousSnapshotId()).isEqualTo(prevId);
    }

    @Test
    @DisplayName("Change Detection: Deve detectar mudança de estoque (AVAILABILITY_CHANGE) quando preço não muda")
    void shouldDetectAvailabilityChangeWhenPriceUnchanged() {
        Instant itemCreation = Instant.now().minus(2, ChronoUnit.HOURS);
        MonitoredItemRecord item = createDefaultItem(itemCreation);

        UUID prevId = UUID.randomUUID();
        MonitoredItemSnapshotRecord prevSnapshot = new MonitoredItemSnapshotRecord(
                prevId, tenantId, itemId, groupId, "MERCADO_LIVRE", "MLB-101",
                new BigDecimal("100.00"), "BRL", BigDecimal.ZERO, "FREE", 10, "ACTIVE",
                "OBSERVED", Instant.now().minus(30, ChronoUnit.MINUTES),
                Instant.now().minus(30, ChronoUnit.MINUTES), true, Instant.now()
        );

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(snapshotRepository.findByTenantIdAndIdempotencyKey(eq(tenantId), any())).thenReturn(Optional.empty());
        when(snapshotRepository.findTopByTenantIdAndItemIdAndObservedAtLessThanEqualOrderByObservedAtDesc(eq(tenantId), eq(itemId), any()))
                .thenReturn(Optional.of(prevSnapshot));
        when(snapshotRepository.save(any(MonitoredItemSnapshotRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MonitoredItemSnapshotDto snapshot = snapshotService.recordSnapshot(
                tenantId, itemId, new BigDecimal("100.00"), BigDecimal.ZERO, 2, "ACTIVE", Instant.now()
        );

        assertThat(snapshot.changeType()).isEqualTo("AVAILABILITY_CHANGE");
        assertThat(snapshot.hasPriceChange()).isFalse();
        assertThat(snapshot.hasAvailabilityChange()).isTrue();
        assertThat(snapshot.priceChangeAmount()).isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("Change Detection: Deve detectar NO_CHANGE quando todos os atributos e preços forem idênticos")
    void shouldDetectNoChangeWhenAllAttributesIdentical() {
        Instant itemCreation = Instant.now().minus(2, ChronoUnit.HOURS);
        MonitoredItemRecord item = createDefaultItem(itemCreation);

        UUID prevId = UUID.randomUUID();
        MonitoredItemSnapshotRecord prevSnapshot = new MonitoredItemSnapshotRecord(
                prevId, tenantId, itemId, groupId, "MERCADO_LIVRE", "MLB-101",
                new BigDecimal("100.00"), "BRL", BigDecimal.ZERO, "FREE", 5, "ACTIVE",
                "OBSERVED", Instant.now().minus(30, ChronoUnit.MINUTES),
                Instant.now().minus(30, ChronoUnit.MINUTES), true, Instant.now()
        );

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(snapshotRepository.findByTenantIdAndIdempotencyKey(eq(tenantId), any())).thenReturn(Optional.empty());
        when(snapshotRepository.findTopByTenantIdAndItemIdAndObservedAtLessThanEqualOrderByObservedAtDesc(eq(tenantId), eq(itemId), any()))
                .thenReturn(Optional.of(prevSnapshot));
        when(snapshotRepository.save(any(MonitoredItemSnapshotRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MonitoredItemSnapshotDto snapshot = snapshotService.recordSnapshot(
                tenantId, itemId, new BigDecimal("100.00"), BigDecimal.ZERO, 5, "ACTIVE", Instant.now()
        );

        assertThat(snapshot.changeType()).isEqualTo("NO_CHANGE");
        assertThat(snapshot.hasPriceChange()).isFalse();
        assertThat(snapshot.hasAvailabilityChange()).isFalse();
        assertThat(snapshot.hasAttributeChange()).isFalse();
    }

    @Test
    @DisplayName("Fail-Closed: Deve lançar SourceUnavailableException e NÃO salvar snapshot se fonte estiver indisponível")
    void shouldThrowSourceUnavailableExceptionAndNotSaveSnapshot() {
        Instant itemCreation = Instant.now().minus(2, ChronoUnit.HOURS);
        MonitoredItemRecord item = createDefaultItem(itemCreation);

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> snapshotService.recordSnapshot(
                tenantId, itemId, new BigDecimal("100.00"), null, 5, "ACTIVE",
                Instant.now(), null, null, null, null, true
        )).isInstanceOf(SourceUnavailableException.class)
          .hasMessageContaining("Fonte de dados externa indisponível");

        verify(snapshotRepository, never()).save(any());
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Metadados e Validade: Deve preencher valid_until, source_url e source_type corretamente")
    void shouldPopulateValidityAndSourceMetadata() {
        Instant itemCreation = Instant.now().minus(2, ChronoUnit.HOURS);
        MonitoredItemRecord item = createDefaultItem(itemCreation);

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(snapshotRepository.findByTenantIdAndIdempotencyKey(eq(tenantId), any())).thenReturn(Optional.empty());
        when(snapshotRepository.findTopByTenantIdAndItemIdAndObservedAtLessThanEqualOrderByObservedAtDesc(eq(tenantId), eq(itemId), any()))
                .thenReturn(Optional.empty());

        ArgumentCaptor<MonitoredItemSnapshotRecord> captor = ArgumentCaptor.forClass(MonitoredItemSnapshotRecord.class);
        when(snapshotRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        Instant observed = Instant.now();
        Instant customValidUntil = observed.plus(120, ChronoUnit.MINUTES);

        MonitoredItemSnapshotDto dto = snapshotService.recordSnapshot(
                tenantId, itemId, new BigDecimal("450.00"), new BigDecimal("25.00"), 3, "ACTIVE",
                observed, "custom-key-123", "https://api.external.com/items/101", "LICENSED_CATALOG",
                customValidUntil, false
        );

        MonitoredItemSnapshotRecord saved = captor.getValue();
        assertThat(saved.getValidUntil()).isEqualTo(customValidUntil);
        assertThat(saved.getSourceUrl()).isEqualTo("https://api.external.com/items/101");
        assertThat(saved.getSourceType()).isEqualTo("LICENSED_CATALOG");
        assertThat(saved.getCollectionStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getIdempotencyKey()).isEqualTo("custom-key-123");
        assertThat(dto.sourceType()).isEqualTo("LICENSED_CATALOG");
    }

    @Test
    @DisplayName("Regra Anti-Retroatividade: deve rejeitar snapshot com data anterior à inclusão do item")
    void shouldRejectRetroactiveSnapshotBeforeItemCreation() {
        Instant itemCreation = Instant.now().minusSeconds(100);
        MonitoredItemRecord item = createDefaultItem(itemCreation);

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));

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
        MonitoredItemRecord item = createDefaultItem(inclusion);

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
