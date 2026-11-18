package com.sellerintelligence.modules.productmonitoring.service;

import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemRecord;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoringGroupRecord;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoringJobExecutionRecord;
import com.sellerintelligence.modules.productmonitoring.model.JobExecutionStatus;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemSnapshotDto;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupStatus;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringJobExecutionDto;
import com.sellerintelligence.modules.productmonitoring.model.TriggerSnapshotRequest;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoringGroupRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoringJobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Serviço orquestrador de execução de jobs de monitoramento.
 * Garante o cumprimento estrito de:
 * 1. "Agendamento respeita quotas, janela mínima, kill switch e capacidade validada."
 * 2. "Não buscar dado em fonte proibida."
 * 3. Auditoria de cancelamentos e execuções.
 */
@Service
public class MonitoringJobExecutionService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringJobExecutionService.class);

    private final MonitoredItemRepository itemRepository;
    private final MonitoringGroupRepository groupRepository;
    private final MonitoredItemSnapshotService snapshotService;
    private final MonitoringSchedulerPolicyValidator policyValidator;
    private final MonitoringJobExecutionRepository executionRepository;

    public record ExecutionResult(
            JobExecutionStatus status,
            String skipReason,
            MonitoredItemSnapshotDto snapshot
    ) {
    }

    public MonitoringJobExecutionService(
            MonitoredItemRepository itemRepository,
            MonitoringGroupRepository groupRepository,
            MonitoredItemSnapshotService snapshotService,
            MonitoringSchedulerPolicyValidator policyValidator,
            MonitoringJobExecutionRepository executionRepository
    ) {
        this.itemRepository = itemRepository;
        this.groupRepository = groupRepository;
        this.snapshotService = snapshotService;
        this.policyValidator = policyValidator;
        this.executionRepository = executionRepository;
    }

    @Transactional
    public ExecutionResult executeItemSnapshot(UUID tenantId, UUID itemId, TriggerSnapshotRequest request) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(itemId, "itemId cannot be null");

        long startMs = System.currentTimeMillis();

        MonitoredItemRecord item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .filter(i -> i.getStatus() != MonitoredItemStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Item monitorado não encontrado: " + itemId));

        MonitoringGroupRecord group = groupRepository.findByIdAndTenantId(item.getGroupId(), tenantId)
                .filter(g -> g.getStatus() != MonitoringGroupStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de monitoramento associado não encontrado: " + item.getGroupId()));

        // 1. Validação de Políticas, Kill Switch, Quota e Janela Mínima
        var validation = policyValidator.validatePreExecution(tenantId, item, group, Instant.now());
        if (!validation.isAllowed()) {
            long durationMs = System.currentTimeMillis() - startMs;
            MonitoringJobExecutionRecord executionRecord = new MonitoringJobExecutionRecord(
                    UUID.randomUUID(),
                    tenantId,
                    item.getId(),
                    group.getId(),
                    "ITEM_SNAPSHOT",
                    item.getMarketplace(),
                    item.getSourcePolicyId(),
                    validation.status(),
                    validation.reason(),
                    Instant.now(),
                    durationMs
            );
            executionRepository.save(executionRecord);

            log.warn("Execução de snapshot não realizada: status={}, motivo='{}', item={}",
                    validation.status(), validation.reason(), itemId);

            return new ExecutionResult(validation.status(), validation.reason(), null);
        }

        // 2. Execução Efetiva do Snapshot
        BigDecimal price = (request != null && request.price() != null)
                ? request.price()
                : (item.getCurrentPrice() != null ? item.getCurrentPrice() : item.getInitialPrice());

        BigDecimal shippingCost = request != null ? request.shippingCost() : null;
        Integer availableQty = request != null ? request.availableQuantity() : null;
        String status = (request != null && request.status() != null) ? request.status() : "ACTIVE";

        MonitoredItemSnapshotDto snapshot = snapshotService.recordSnapshot(
                tenantId,
                item.getId(),
                price,
                shippingCost,
                availableQty,
                status,
                Instant.now()
        );

        long durationMs = System.currentTimeMillis() - startMs;
        MonitoringJobExecutionRecord executionRecord = new MonitoringJobExecutionRecord(
                UUID.randomUUID(),
                tenantId,
                item.getId(),
                group.getId(),
                "ITEM_SNAPSHOT",
                item.getMarketplace(),
                item.getSourcePolicyId(),
                JobExecutionStatus.SUCCESS,
                null,
                Instant.now(),
                durationMs
        );
        executionRepository.save(executionRecord);

        log.info("Job de snapshot executado com sucesso: item={}, duration={}ms", itemId, durationMs);
        return new ExecutionResult(JobExecutionStatus.SUCCESS, null, snapshot);
    }

    @Transactional(readOnly = true)
    public Page<MonitoringJobExecutionDto> listExecutions(UUID tenantId, Pageable pageable) {
        Page<MonitoringJobExecutionRecord> records = executionRepository.findAllByTenantIdOrderByExecutedAtDesc(tenantId, pageable);
        return records.map(r -> new MonitoringJobExecutionDto(
                r.getId(),
                r.getItemId(),
                r.getGroupId(),
                r.getJobType(),
                r.getMarketplace(),
                r.getSourcePolicyId(),
                r.getExecutionStatus(),
                r.getSkipReason(),
                r.getExecutedAt(),
                r.getDurationMs()
        ));
    }
}
