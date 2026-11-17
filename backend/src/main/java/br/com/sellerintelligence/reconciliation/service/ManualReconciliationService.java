package br.com.sellerintelligence.reconciliation.service;

import br.com.sellerintelligence.reconciliation.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ManualReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ManualReconciliationService.class);

    private final ReconciliationSettlementItemRepository itemRepository;
    private final ReconciliationDisputeRepository disputeRepository;
    private final ReconciliationAuditLogRepository auditLogRepository;

    public ManualReconciliationService(ReconciliationSettlementItemRepository itemRepository,
                                       ReconciliationDisputeRepository disputeRepository,
                                       ReconciliationAuditLogRepository auditLogRepository) {
        this.itemRepository = itemRepository;
        this.disputeRepository = disputeRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public ReconciliationSettlementItem manuallyAssociateOrder(UUID tenantId, UUID itemId, UUID orderId,
                                                              String justification, UUID userId) {
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("Justificativa é obrigatória para associação manual de conciliação.");
        }

        ReconciliationSettlementItem item = getItem(tenantId, itemId);
        String previousState = item.getReconciliationStatus().name();

        item.manualOverride(orderId, justification);
        ReconciliationSettlementItem saved = itemRepository.save(item);

        ReconciliationAuditLog auditLog = new ReconciliationAuditLog(
                tenantId, itemId, userId, ReconciliationAuditAction.MANUAL_MATCH,
                previousState, saved.getReconciliationStatus().name(), justification
        );
        auditLogRepository.save(auditLog);

        log.info("Manual association applied: itemId={}, orderId={}, tenantId={}", itemId, orderId, tenantId);
        return saved;
    }

    @Transactional
    public ReconciliationSettlementItem ignoreDiscrepancy(UUID tenantId, UUID itemId,
                                                         String justification, UUID userId) {
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("Justificativa é obrigatória para abonar divergência.");
        }

        ReconciliationSettlementItem item = getItem(tenantId, itemId);
        String previousState = item.getReconciliationStatus().name();

        item.ignoreDiscrepancy(justification);
        ReconciliationSettlementItem saved = itemRepository.save(item);

        ReconciliationAuditLog auditLog = new ReconciliationAuditLog(
                tenantId, itemId, userId, ReconciliationAuditAction.DISCREPANCY_IGNORED,
                previousState, saved.getReconciliationStatus().name(), justification
        );
        auditLogRepository.save(auditLog);

        log.info("Discrepancy ignored by user: itemId={}, tenantId={}", itemId, tenantId);
        return saved;
    }

    @Transactional
    public ReconciliationDispute logDispute(UUID tenantId, UUID itemId, String protocolNumber,
                                            String marketplace, BigDecimal disputedAmount,
                                            DisputeChannel channel, String notes, UUID userId) {
        if (protocolNumber == null || protocolNumber.isBlank()) {
            throw new IllegalArgumentException("Número de protocolo de contestação é obrigatório.");
        }
        if (disputedAmount == null || disputedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor contestado deve ser maior que zero.");
        }

        ReconciliationSettlementItem item = getItem(tenantId, itemId);

        ReconciliationDispute dispute = new ReconciliationDispute(
                itemId, tenantId, protocolNumber, marketplace, disputedAmount, channel, notes
        );
        ReconciliationDispute savedDispute = disputeRepository.save(dispute);

        ReconciliationAuditLog auditLog = new ReconciliationAuditLog(
                tenantId, itemId, userId, ReconciliationAuditAction.DISPUTE_LOGGED,
                item.getReconciliationStatus().name(), "DISPUTE_OPEN",
                "Protocolo de contestação registrado: " + protocolNumber
        );
        auditLogRepository.save(auditLog);

        log.info("Dispute protocol logged: disputeId={}, protocol={}, tenantId={}",
                savedDispute.getId(), protocolNumber, tenantId);
        return savedDispute;
    }

    @Transactional(readOnly = true)
    public List<ReconciliationDispute> listDisputes(UUID tenantId) {
        return disputeRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationAuditLog> listAuditLogs(UUID tenantId, UUID itemId) {
        return auditLogRepository.findBySettlementItemIdOrderByCreatedAtDesc(itemId);
    }

    private ReconciliationSettlementItem getItem(UUID tenantId, UUID itemId) {
        return itemRepository.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Item de conciliação não encontrado: " + itemId));
    }
}
