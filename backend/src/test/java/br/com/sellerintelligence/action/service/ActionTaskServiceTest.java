package br.com.sellerintelligence.action.service;

import br.com.sellerintelligence.action.domain.*;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.MembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionTaskServiceTest {

    @Mock
    private ActionTaskRepository taskRepository;

    @Mock
    private ActionTaskAuditRepository auditRepository;

    @Mock
    private ActionAlertRepository alertRepository;

    @Mock
    private MembershipRepository membershipRepository;

    private ActionTaskService taskService;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();
    private final UUID assignedUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        taskService = new ActionTaskService(taskRepository, auditRepository, alertRepository, membershipRepository);
    }

    @Test
    @DisplayName("Should create task from alert, inherit evidence summary and link to alert")
    void shouldCreateTaskFromAlert() {
        ActionAlert alert = new ActionAlert(
                tenantId, UUID.randomUUID(), "Risco de Margem Baixa",
                "SKU-456 com margem de 4.2%", ActionAlertSeverity.CRITICAL,
                ActionAlertDomain.FINANCIAL, AlertScopeType.STORE, "store-1",
                "CALCULATED", "{\"sku\":\"SKU-456\",\"margin\":4.2}", "dedup-abc"
        );
        when(alertRepository.findByIdAndTenantId(alert.getId(), tenantId)).thenReturn(Optional.of(alert));
        when(membershipRepository.existsByOrganizationIdAndUserIdAndStatus(tenantId, assignedUserId, MembershipStatus.ACTIVE))
                .thenReturn(true);
        when(taskRepository.save(any(ActionTask.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant dueDate = Instant.now().plus(2, ChronoUnit.DAYS);
        ActionTask task = taskService.createTaskFromAlert(
                tenantId, alert.getId(), actorUserId, TaskPriority.HIGH,
                assignedUserId, dueDate, "Revisar custo do SKU-456", "Contatar fornecedor"
        );

        assertThat(task).isNotNull();
        assertThat(task.getTitle()).isEqualTo("Revisar custo do SKU-456");
        assertThat(task.getAlertId()).isEqualTo(alert.getId());
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getAssignedToUserId()).isEqualTo(assignedUserId);
        assertThat(task.getEvidenceSummary()).contains("Alerta [" + alert.getId() + "]");

        verify(taskRepository, times(1)).save(any(ActionTask.class));
        verify(auditRepository, times(1)).save(any(ActionTaskAudit.class));
        verify(alertRepository, times(1)).save(alert);
        assertThat(alert.getStatus()).isEqualTo(ActionAlertStatus.READ);
    }

    @Test
    @DisplayName("Should block assigning task to user outside organization tenant (Anti-IDOR)")
    void shouldBlockAssigningToUserOutsideTenant() {
        UUID externalUserId = UUID.randomUUID();
        when(membershipRepository.existsByOrganizationIdAndUserIdAndStatus(tenantId, externalUserId, MembershipStatus.ACTIVE))
                .thenReturn(false);

        assertThatThrownBy(() -> taskService.createTask(
                tenantId, actorUserId, "Tarefa Inválida", "Tentativa de atribuição cross-tenant",
                TaskPriority.LOW, externalUserId, null, "evidência"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("não pertence à organização tenant");

        verify(taskRepository, never()).save(any(ActionTask.class));
        verify(auditRepository, never()).save(any(ActionTaskAudit.class));
    }

    @Test
    @DisplayName("Should assign task to valid tenant member and record audit trail")
    void shouldAssignTaskToValidMember() {
        ActionTask task = new ActionTask(
                tenantId, null, "Auditar tributos", "Conferir ICMS-ST",
                TaskPriority.MEDIUM, null, null, null, actorUserId
        );
        when(taskRepository.findByIdAndTenantId(task.getId(), tenantId)).thenReturn(Optional.of(task));
        when(membershipRepository.existsByOrganizationIdAndUserIdAndStatus(tenantId, assignedUserId, MembershipStatus.ACTIVE))
                .thenReturn(true);
        when(taskRepository.save(any(ActionTask.class))).thenAnswer(inv -> inv.getArgument(0));

        ActionTask updated = taskService.assignTask(tenantId, task.getId(), actorUserId, assignedUserId);

        assertThat(updated.getAssignedToUserId()).isEqualTo(assignedUserId);
        verify(taskRepository, times(1)).save(task);
        verify(auditRepository, times(1)).save(argThat(audit ->
                audit.getActionType() == TaskAuditAction.ASSIGNED &&
                audit.getActorUserId().equals(actorUserId)
        ));
    }

    @Test
    @DisplayName("Should complete task, record completion audit and resolve associated alert")
    void shouldCompleteTaskAndResolveAlert() {
        UUID alertId = UUID.randomUUID();
        ActionTask task = new ActionTask(
                tenantId, alertId, "Ajustar Preço Mínimo", "Margem reestabelecida",
                TaskPriority.HIGH, assignedUserId, null, "evidência", actorUserId
        );
        ActionAlert alert = new ActionAlert(
                tenantId, null, "Margem Crítica", "Margem de 2%",
                ActionAlertSeverity.CRITICAL, ActionAlertDomain.FINANCIAL,
                AlertScopeType.ORGANIZATION, null, "CALCULATED", "{}", "dedup-xyz"
        );
        when(taskRepository.findByIdAndTenantId(task.getId(), tenantId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(ActionTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(alertRepository.findByIdAndTenantId(alertId, tenantId)).thenReturn(Optional.of(alert));

        ActionTask completed = taskService.updateTaskStatus(tenantId, task.getId(), actorUserId, TaskStatus.DONE, "Preço atualizado no ERP");

        assertThat(completed.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getCompletionNotes()).isEqualTo("Preço atualizado no ERP");

        verify(auditRepository, times(1)).save(argThat(audit ->
                audit.getActionType() == TaskAuditAction.COMPLETED &&
                audit.getNewState().equals("DONE")
        ));
        verify(alertRepository, times(1)).save(alert);
        assertThat(alert.getStatus()).isEqualTo(ActionAlertStatus.RESOLVED);
    }
}
