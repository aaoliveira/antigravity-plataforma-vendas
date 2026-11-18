package com.sellerintelligence.modules.productmonitoring.service;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.BusinessRuleException;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemRecord;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemSnapshotRecord;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemSnapshotDto;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Serviço responsável pela gravação e consulta de snapshots históricos.
 * Cumpre a regra de aceite fundamental:
 * "Histórico começa no primeiro snapshot efetivo após inclusão."
 * "Histórico não retroativo."
 */
@Service
public class MonitoredItemSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(MonitoredItemSnapshotService.class);

    private final MonitoredItemRepository itemRepository;
    private final MonitoredItemSnapshotRepository snapshotRepository;
    private final AuditPublisher auditPublisher;

    public MonitoredItemSnapshotService(
            MonitoredItemRepository itemRepository,
            MonitoredItemSnapshotRepository snapshotRepository,
            AuditPublisher auditPublisher
    ) {
        this.itemRepository = itemRepository;
        this.snapshotRepository = snapshotRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public MonitoredItemSnapshotDto recordSnapshot(
            UUID tenantId,
            UUID itemId,
            BigDecimal price,
            BigDecimal shippingCost,
            Integer availableQuantity,
            String itemStatus,
            Instant observedAt
    ) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(itemId, "itemId cannot be null");
        Objects.requireNonNull(price, "price cannot be null");

        MonitoredItemRecord item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .filter(i -> i.getStatus() != MonitoredItemStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Item monitorado não encontrado: " + itemId));

        Instant actualObservedAt = observedAt != null ? observedAt : Instant.now();

        // REGRA DE OURO ANTI-ALUCINAÇÃO: Não retroagir snapshot para antes da data de inclusão do item
        // Permite pequena margem de clock skew de até 5 segundos
        if (actualObservedAt.isBefore(item.getCreatedAt().minusSeconds(5))) {
            throw new BusinessRuleException(
                    "Violação de integridade histórica: o snapshot (" + actualObservedAt +
                    ") não pode ser anterior à data de inclusão do item no monitoramento (" + item.getCreatedAt() + ")."
            );
        }

        String shippingStatus = shippingCost != null ? (shippingCost.compareTo(BigDecimal.ZERO) == 0 ? "FREE" : "KNOWN") : "UNKNOWN";

        MonitoredItemSnapshotRecord snapshot = new MonitoredItemSnapshotRecord(
                UUID.randomUUID(),
                tenantId,
                item.getId(),
                item.getGroupId(),
                item.getMarketplace(),
                item.getExternalListingId(),
                price,
                item.getCurrency(),
                shippingCost,
                shippingStatus,
                availableQuantity,
                itemStatus != null ? itemStatus : "ACTIVE",
                "OBSERVED",
                Instant.now(),
                actualObservedAt,
                true,
                Instant.now()
        );

        MonitoredItemSnapshotRecord saved = snapshotRepository.save(snapshot);

        // Atualiza metadados do item monitorado
        if (item.getFirstSnapshotAt() == null) {
            item.setFirstSnapshotAt(actualObservedAt);
            log.info("Primeiro snapshot efetivo registrado para o item: id={}, observedAt={}", itemId, actualObservedAt);
        }

        item.setCurrentPrice(price);
        item.setLastSnapshotAt(actualObservedAt);
        item.setNextRunAt(actualObservedAt.plus(Duration.ofMinutes(item.getMonitoringIntervalMinutes())));
        itemRepository.save(item);

        auditPublisher.publish(
                "MONITORED_ITEM_SNAPSHOT_RECORDED",
                null,
                "system",
                tenantId,
                null,
                null,
                null,
                String.format("Snapshot registrado para item %s: preço=%s, status=%s", item.getTitle(), price, itemStatus)
        );

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MonitoredItemSnapshotDto> getItemSnapshots(UUID tenantId, UUID itemId) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(itemId, "itemId cannot be null");

        MonitoredItemRecord item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .filter(i -> i.getStatus() != MonitoredItemStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Item monitorado não encontrado: " + itemId));

        // Filtra estritamente snapshots ocorridos a partir da data de inclusão do item (ou firstSnapshotAt)
        Instant inclusionInstant = item.getFirstSnapshotAt() != null ? item.getFirstSnapshotAt() : item.getCreatedAt();
        List<MonitoredItemSnapshotRecord> snapshots = snapshotRepository.findSnapshotsSinceInclusion(
                tenantId, itemId, inclusionInstant.minusSeconds(1)
        );

        return snapshots.stream().map(this::toDto).toList();
    }

    private MonitoredItemSnapshotDto toDto(MonitoredItemSnapshotRecord r) {
        return new MonitoredItemSnapshotDto(
                r.getId(),
                r.getItemId(),
                r.getGroupId(),
                r.getMarketplace(),
                r.getExternalListingId(),
                r.getPrice(),
                r.getCurrency(),
                r.getShippingCost(),
                r.getShippingStatus(),
                r.getAvailableQuantity(),
                r.getStatus(),
                r.getProvenanceType(),
                r.getCollectedAt(),
                r.getObservedAt(),
                r.isEffectiveSnapshot()
        );
    }
}
