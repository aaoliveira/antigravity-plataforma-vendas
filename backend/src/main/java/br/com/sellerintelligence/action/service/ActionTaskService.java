package br.com.sellerintelligence.action.service;

import br.com.sellerintelligence.action.domain.*;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.MembershipStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
public class ActionTaskService {

    private static final Logger log = LoggerFactory.getLogger(ActionTaskService.class);

    private final ActionTaskRepository taskRepository;
    private final ActionTaskAuditRepository auditRepository;
    private final ActionAlertRepository alertRepository;
    private final MembershipRepository membershipRepository;

    public ActionTaskService(ActionTaskRepository taskRepository,
                             ActionTaskAuditRepository auditRepository,
                             ActionAlertRepository alertRepository,
                             MembershipRepository membershipRepository) {
        this.taskRepository = taskRepository;
        this.auditRepository = auditRepository;
        this.alertRepository = alertRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public ActionTask createTaskFromAlert(UUID tenantId, UUID alertId, UUID actorUserId,
                                          TaskPriority priority, UUID assignedToUserId,
                                          Instant dueDate, String customTitle, String customNotes) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(alertId, "alertId cannot be null");
        Objects.requireNonNull(actorUserId, "actorUserId cannot be null");

        ActionAlert alert = alertRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Alerta não encontrado para criação de tarefa: " + alertId));

        validateAssignee(tenantId, assignedToUserId);

        String title = (customTitle != null && !customTitle.isBlank())
                ? customTitle
                : ("Resolver: " + alert.getTitle());

        String description = alert.getDescription() + (customNotes != null && !customNotes.isBlank() ? "\nObservação: " + customNotes : "");
        String evidenceSummary = String.format("Origem: Alerta [%s] | Severidade: %s | Domínio: %s | Proveniência: %s | Evidência: %s",
                alert.getId(), alert.getSeverity(), alert.getDomainType(), alert.getProvenance(), alert.getEvidenceContext());

        ActionTask task = new ActionTask(
                tenantId,
                alert.getId(),
                title,
                description,
                priority != null ? priority : TaskPriority.MEDIUM,
                assignedToUserId,
                dueDate,
                evidenceSummary,
                actorUserId
        );

        ActionTask saved = taskRepository.save(task);

        // Record audit trail
        ActionTaskAudit audit = new ActionTaskAudit(
                saved.getId(),
                actorUserId,
                TaskAuditAction.CREATED,
                null,
                TaskStatus.TODO.name(),
                "Tarefa criada a partir do alerta " + alertId
        );
        auditRepository.save(audit);

        // Update alert status to READ if it was NEW
        if (alert.getStatus() == ActionAlertStatus.NEW) {
            alert.updateStatus(ActionAlertStatus.READ);
            alertRepository.save(alert);
        }

        log.info("Created action task {} from alert {} for tenant {}", saved.getId(), alertId, tenantId);
        return saved;
    }

    @Transactional
    public ActionTask createTask(UUID tenantId, UUID creatorUserId, String title, String description,
                                 TaskPriority priority, UUID assignedToUserId, Instant dueDate,
                                 String evidenceSummary) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(creatorUserId, "creatorUserId cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(description, "description cannot be null");

        validateAssignee(tenantId, assignedToUserId);

        ActionTask task = new ActionTask(
                tenantId,
                null,
                title,
                description,
                priority != null ? priority : TaskPriority.MEDIUM,
                assignedToUserId,
                dueDate,
                evidenceSummary,
                creatorUserId
        );
        ActionTask saved = taskRepository.save(task);

        ActionTaskAudit audit = new ActionTaskAudit(
                saved.getId(),
                creatorUserId,
                TaskAuditAction.CREATED,
                null,
                TaskStatus.TODO.name(),
                "Tarefa operacional criada manualmente."
        );
        auditRepository.save(audit);

        log.info("Created manual action task {} for tenant {}", saved.getId(), tenantId);
        return saved;
    }

    @Transactional
    public ActionTask assignTask(UUID tenantId, UUID taskId, UUID actorUserId, UUID newAssigneeUserId) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(taskId, "taskId cannot be null");
        Objects.requireNonNull(actorUserId, "actorUserId cannot be null");

        ActionTask task = getTask(tenantId, taskId);
        validateAssignee(tenantId, newAssigneeUserId);

        UUID previousAssignee = task.getAssignedToUserId();
        task.assignTo(newAssigneeUserId);
        ActionTask updated = taskRepository.save(task);

        ActionTaskAudit audit = new ActionTaskAudit(
                taskId,
                actorUserId,
                TaskAuditAction.ASSIGNED,
                previousAssignee != null ? previousAssignee.toString() : "UNASSIGNED",
                newAssigneeUserId != null ? newAssigneeUserId.toString() : "UNASSIGNED",
                "Responsável pela tarefa alterado."
        );
        auditRepository.save(audit);

        log.info("Assigned task {} to user {} by actor {}", taskId, newAssigneeUserId, actorUserId);
        return updated;
    }

    @Transactional
    public ActionTask updateTaskStatus(UUID tenantId, UUID taskId, UUID actorUserId,
                                       TaskStatus newStatus, String notes) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(taskId, "taskId cannot be null");
        Objects.requireNonNull(actorUserId, "actorUserId cannot be null");
        Objects.requireNonNull(newStatus, "newStatus cannot be null");

        ActionTask task = getTask(tenantId, taskId);
        TaskStatus previousStatus = task.getStatus();

        task.updateStatus(newStatus, notes);
        ActionTask updated = taskRepository.save(task);

        TaskAuditAction action = switch (newStatus) {
            case DONE -> TaskAuditAction.COMPLETED;
            case CANCELLED -> TaskAuditAction.CANCELLED;
            default -> TaskAuditAction.STATUS_CHANGED;
        };

        ActionTaskAudit audit = new ActionTaskAudit(
                taskId,
                actorUserId,
                action,
                previousStatus.name(),
                newStatus.name(),
                notes != null ? notes : ("Status alterado para " + newStatus)
        );
        auditRepository.save(audit);

        // If task was completed and originated from an alert, resolve the alert
        if (newStatus == TaskStatus.DONE && task.getAlertId() != null) {
            alertRepository.findByIdAndTenantId(task.getAlertId(), tenantId)
                    .ifPresent(alert -> {
                        alert.updateStatus(ActionAlertStatus.RESOLVED);
                        alertRepository.save(alert);
                    });
        }

        log.info("Updated task {} status from {} to {}", taskId, previousStatus, newStatus);
        return updated;
    }

    @Transactional(readOnly = true)
    public ActionTask getTask(UUID tenantId, UUID taskId) {
        return taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tarefa operacional não encontrada: " + taskId));
    }

    @Transactional(readOnly = true)
    public List<ActionTask> getTasks(UUID tenantId, TaskStatus status, UUID assignedToUserId) {
        if (status != null) {
            return taskRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
        }
        if (assignedToUserId != null) {
            return taskRepository.findByTenantIdAndAssignedToUserIdOrderByCreatedAtDesc(tenantId, assignedToUserId);
        }
        return taskRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<ActionTaskAudit> getTaskAudits(UUID tenantId, UUID taskId) {
        // verify task belongs to tenant
        getTask(tenantId, taskId);
        return auditRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    private void validateAssignee(UUID tenantId, UUID assignedToUserId) {
        if (assignedToUserId == null) {
            return;
        }
        boolean isMember = membershipRepository.existsByOrganizationIdAndUserIdAndStatus(
                tenantId, assignedToUserId, MembershipStatus.ACTIVE
        );
        if (!isMember) {
            throw new IllegalArgumentException(
                    "Usuário atribuído (" + assignedToUserId + ") não pertence à organização tenant (" + tenantId + ")."
            );
        }
    }
}
