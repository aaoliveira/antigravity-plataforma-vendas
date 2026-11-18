package com.sellerintelligence.modules.productmonitoring.service;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.DuplicateResourceException;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoringGroupRecord;
import com.sellerintelligence.modules.productmonitoring.model.CreateMonitoringGroupRequest;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupDto;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupStatus;
import com.sellerintelligence.modules.productmonitoring.model.UpdateMonitoringGroupRequest;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemRepository;
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
public class MonitoringGroupService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringGroupService.class);

    private final MonitoringGroupRepository groupRepository;
    private final MonitoredItemRepository itemRepository;
    private final AuditPublisher auditPublisher;

    public MonitoringGroupService(
            MonitoringGroupRepository groupRepository,
            MonitoredItemRepository itemRepository,
            AuditPublisher auditPublisher
    ) {
        this.groupRepository = groupRepository;
        this.itemRepository = itemRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public MonitoringGroupDto createGroup(UUID tenantId, CreateMonitoringGroupRequest request) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(request, "request cannot be null");

        String normalizedName = request.name().trim();
        if (groupRepository.existsByTenantIdAndNameIgnoreCaseAndStatusNot(tenantId, normalizedName, MonitoringGroupStatus.DELETED)) {
            throw new DuplicateResourceException("Já existe um grupo ativo ou pausado com o nome: " + normalizedName);
        }

        MonitoringGroupRecord record = new MonitoringGroupRecord(
                UUID.randomUUID(),
                tenantId,
                normalizedName,
                request.description(),
                request.color(),
                request.minIntervalMinutes(),
                request.retentionDays(),
                MonitoringGroupStatus.ACTIVE,
                null,
                Instant.now(),
                Instant.now()
        );

        MonitoringGroupRecord saved = groupRepository.save(record);
        log.info("Grupo de monitoramento criado com sucesso: id={}, tenant={}, name={}", saved.getId(), tenantId, saved.getName());

        auditPublisher.publish(
                "MONITORING_GROUP_CREATED",
                null,
                "system",
                tenantId,
                null,
                null,
                null,
                "Grupo de monitoramento criado: " + saved.getName()
        );

        return toDto(saved, 0);
    }

    @Transactional(readOnly = true)
    public MonitoringGroupDto getGroup(UUID tenantId, UUID groupId) {
        MonitoringGroupRecord record = findActiveGroupOrThrow(tenantId, groupId);
        int totalItems = itemRepository.countByTenantIdAndGroupIdAndStatusNot(tenantId, groupId, com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus.DELETED);
        return toDto(record, totalItems);
    }

    @Transactional(readOnly = true)
    public Page<MonitoringGroupDto> listGroups(UUID tenantId, String search, Pageable pageable) {
        Page<MonitoringGroupRecord> page;
        if (search != null && !search.isBlank()) {
            page = groupRepository.searchByTenantIdAndName(tenantId, search.trim(), pageable);
        } else {
            page = groupRepository.findAllByTenantIdAndStatusNot(tenantId, MonitoringGroupStatus.DELETED, pageable);
        }

        return page.map(record -> {
            int totalItems = itemRepository.countByTenantIdAndGroupIdAndStatusNot(
                    tenantId, record.getId(), com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus.DELETED
            );
            return toDto(record, totalItems);
        });
    }

    @Transactional
    public MonitoringGroupDto updateGroup(UUID tenantId, UUID groupId, UpdateMonitoringGroupRequest request) {
        MonitoringGroupRecord record = findActiveGroupOrThrow(tenantId, groupId);

        if (request.name() != null && !request.name().isBlank()) {
            String newName = request.name().trim();
            if (!newName.equalsIgnoreCase(record.getName()) &&
                    groupRepository.existsByTenantIdAndNameIgnoreCaseAndStatusNot(tenantId, newName, MonitoringGroupStatus.DELETED)) {
                throw new DuplicateResourceException("Já existe um grupo com o nome: " + newName);
            }
            record.setName(newName);
        }

        if (request.description() != null) {
            record.setDescription(request.description().trim());
        }
        if (request.color() != null && !request.color().isBlank()) {
            record.setColor(request.color().trim());
        }
        if (request.minIntervalMinutes() != null) {
            record.setMinIntervalMinutes(request.minIntervalMinutes());
        }
        if (request.retentionDays() != null) {
            record.setRetentionDays(request.retentionDays());
        }
        if (request.status() != null) {
            record.setStatus(request.status());
        }

        MonitoringGroupRecord updated = groupRepository.save(record);
        log.info("Grupo de monitoramento atualizado: id={}, tenant={}", updated.getId(), tenantId);

        auditPublisher.publish(
                "MONITORING_GROUP_UPDATED",
                null,
                "system",
                tenantId,
                null,
                null,
                null,
                "Grupo de monitoramento atualizado: " + updated.getName()
        );

        int totalItems = itemRepository.countByTenantIdAndGroupIdAndStatusNot(
                tenantId, groupId, com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus.DELETED
        );
        return toDto(updated, totalItems);
    }

    @Transactional
    public void deleteGroup(UUID tenantId, UUID groupId) {
        MonitoringGroupRecord record = findActiveGroupOrThrow(tenantId, groupId);

        record.setStatus(MonitoringGroupStatus.DELETED);
        record.setDeletedAt(Instant.now());
        groupRepository.save(record);

        // Marca itens associados como DELETED para interromper agendamentos
        var items = itemRepository.findAllByTenantIdAndGroupIdAndStatusNot(
                tenantId, groupId, com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus.DELETED
        );
        for (var item : items) {
            item.setStatus(com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus.DELETED);
            item.setDeletedAt(Instant.now());
            itemRepository.save(item);
        }

        log.info("Grupo de monitoramento removido logicamente: id={}, tenant={}, itensRemovidos={}", groupId, tenantId, items.size());

        auditPublisher.publish(
                "MONITORING_GROUP_DELETED",
                null,
                "system",
                tenantId,
                null,
                null,
                null,
                "Grupo de monitoramento removido logicamente: " + record.getName()
        );
    }

    private MonitoringGroupRecord findActiveGroupOrThrow(UUID tenantId, UUID groupId) {
        return groupRepository.findByIdAndTenantId(groupId, tenantId)
                .filter(g -> g.getStatus() != MonitoringGroupStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de monitoramento não encontrado ou indisponível: " + groupId));
    }

    private MonitoringGroupDto toDto(MonitoringGroupRecord r, int totalItems) {
        return new MonitoringGroupDto(
                r.getId(),
                r.getName(),
                r.getDescription(),
                r.getColor(),
                r.getMinIntervalMinutes(),
                r.getRetentionDays(),
                r.getStatus(),
                totalItems,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
