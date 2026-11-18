package com.sellerintelligence.modules.productmonitoring.service;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.BusinessRuleException;
import com.sellerintelligence.core.exception.DuplicateResourceException;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemRecord;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoringGroupRecord;
import com.sellerintelligence.modules.productmonitoring.model.AddMonitoredItemRequest;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemDto;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupStatus;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemSnapshotRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoringGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class MonitoredItemService {

    private static final Logger log = LoggerFactory.getLogger(MonitoredItemService.class);

    private final MonitoredItemRepository itemRepository;
    private final MonitoringGroupRepository groupRepository;
    private final MonitoredItemSnapshotRepository snapshotRepository;
    private final MonitoringSchedulerPolicyValidator policyValidator;
    private final AuditPublisher auditPublisher;

    public MonitoredItemService(
            MonitoredItemRepository itemRepository,
            MonitoringGroupRepository groupRepository,
            MonitoredItemSnapshotRepository snapshotRepository,
            MonitoringSchedulerPolicyValidator policyValidator,
            AuditPublisher auditPublisher
    ) {
        this.itemRepository = itemRepository;
        this.groupRepository = groupRepository;
        this.snapshotRepository = snapshotRepository;
        this.policyValidator = policyValidator;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public MonitoredItemDto addItemToGroup(UUID tenantId, UUID groupId, AddMonitoredItemRequest request) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(groupId, "groupId cannot be null");
        Objects.requireNonNull(request, "request cannot be null");

        MonitoringGroupRecord group = groupRepository.findByIdAndTenantId(groupId, tenantId)
                .filter(g -> g.getStatus() != MonitoringGroupStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de monitoramento não encontrado: " + groupId));

        // 1. Verificação de Fonte Proibida
        if (policyValidator.isForbiddenSource(request.sourcePolicyId())) {
            throw new BusinessRuleException(
                    "Operação negada: a fonte vinculada '" + request.sourcePolicyId() + "' é proibida ou não homologada."
            );
        }

        // 2. Controle Estrito de Duplicidade no Grupo
        String normalizedListingId = request.externalListingId().trim();
        String normalizedMarketplace = request.marketplace().trim().toUpperCase();

        if (itemRepository.existsByTenantIdAndGroupIdAndMarketplaceAndExternalListingIdAndStatusNot(
                tenantId, groupId, normalizedMarketplace, normalizedListingId, MonitoredItemStatus.DELETED)) {
            throw new DuplicateResourceException(
                    String.format("O anúncio '%s' do marketplace '%s' já está adicionado ao grupo '%s'.",
                            normalizedListingId, normalizedMarketplace, group.getName())
            );
        }

        int interval = Math.max(
                request.monitoringIntervalMinutes() != null ? request.monitoringIntervalMinutes() : group.getMinIntervalMinutes(),
                group.getMinIntervalMinutes()
        );

        Instant now = Instant.now();
        MonitoredItemRecord record = new MonitoredItemRecord(
                UUID.randomUUID(),
                tenantId,
                groupId,
                normalizedMarketplace,
                request.sourcePolicyId().trim(),
                normalizedListingId,
                request.title().trim(),
                request.initialPrice(),
                request.initialPrice(),
                request.currency(),
                request.sourceUrl(),
                request.sellerName(),
                request.itemCondition(),
                MonitoredItemStatus.ACTIVE,
                interval,
                null,
                now,
                null, // firstSnapshotAt começa nulo!
                null,
                now,
                now
        );

        MonitoredItemRecord saved = itemRepository.save(record);
        log.info("Item adicionado ao grupo de monitoramento: id={}, tenant={}, group={}, listingId={}",
                saved.getId(), tenantId, groupId, normalizedListingId);

        auditPublisher.publish(
                "MONITORED_ITEM_ADDED",
                null,
                "system",
                tenantId,
                null,
                null,
                null,
                String.format("Item adicionado ao grupo '%s': %s (%s)", group.getName(), saved.getTitle(), normalizedListingId)
        );

        return toDto(saved, 0);
    }

    @Transactional(readOnly = true)
    public MonitoredItemDto getItem(UUID tenantId, UUID itemId) {
        MonitoredItemRecord record = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .filter(i -> i.getStatus() != MonitoredItemStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Item monitorado não encontrado: " + itemId));

        int totalSnapshots = snapshotRepository.countByTenantIdAndItemId(tenantId, itemId);
        return toDto(record, totalSnapshots);
    }

    @Transactional(readOnly = true)
    public Page<MonitoredItemDto> listItemsInGroup(UUID tenantId, UUID groupId, Pageable pageable) {
        // Valida grupo
        groupRepository.findByIdAndTenantId(groupId, tenantId)
                .filter(g -> g.getStatus() != MonitoringGroupStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de monitoramento não encontrado: " + groupId));

        Page<MonitoredItemRecord> page = itemRepository.findAllByTenantIdAndGroupIdAndStatusNot(
                tenantId, groupId, MonitoredItemStatus.DELETED, pageable
        );

        return page.map(record -> {
            int totalSnapshots = snapshotRepository.countByTenantIdAndItemId(tenantId, record.getId());
            return toDto(record, totalSnapshots);
        });
    }

    @Transactional
    public void removeItemFromGroup(UUID tenantId, UUID groupId, UUID itemId) {
        MonitoredItemRecord record = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .filter(i -> i.getGroupId().equals(groupId))
                .filter(i -> i.getStatus() != MonitoredItemStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Item monitorado não encontrado no grupo especificado: " + itemId));

        record.setStatus(MonitoredItemStatus.DELETED);
        record.setDeletedAt(Instant.now());
        itemRepository.save(record);

        log.info("Item monitorado removido logicamente: id={}, group={}, tenant={}", itemId, groupId, tenantId);

        auditPublisher.publish(
                "MONITORED_ITEM_REMOVED",
                null,
                "system",
                tenantId,
                null,
                null,
                null,
                String.format("Item removido do monitoramento: %s (%s)", record.getTitle(), record.getExternalListingId())
        );
    }

    private MonitoredItemDto toDto(MonitoredItemRecord r, int totalSnapshots) {
        return new MonitoredItemDto(
                r.getId(),
                r.getGroupId(),
                r.getMarketplace(),
                r.getSourcePolicyId(),
                r.getExternalListingId(),
                r.getTitle(),
                r.getInitialPrice(),
                r.getCurrentPrice(),
                r.getCurrency(),
                r.getSourceUrl(),
                r.getSellerName(),
                r.getItemCondition(),
                r.getStatus(),
                r.getMonitoringIntervalMinutes(),
                r.getLastSnapshotAt(),
                r.getNextRunAt(),
                r.getFirstSnapshotAt(),
                totalSnapshots,
                r.getCreatedAt()
        );
    }
}
