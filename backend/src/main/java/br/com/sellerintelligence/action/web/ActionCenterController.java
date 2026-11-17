package br.com.sellerintelligence.action.web;

import br.com.sellerintelligence.action.domain.*;
import br.com.sellerintelligence.action.service.*;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/action")
public class ActionCenterController {

    private final ActionAlertRuleService ruleService;
    private final ActionAlertService alertService;
    private final ActionTaskService taskService;
    private final ActionNotificationService notificationService;

    public ActionCenterController(ActionAlertRuleService ruleService,
                                  ActionAlertService alertService,
                                  ActionTaskService taskService,
                                  ActionNotificationService notificationService) {
        this.ruleService = ruleService;
        this.alertService = alertService;
        this.taskService = taskService;
        this.notificationService = notificationService;
    }

    // 1. Regras de Alerta
    @PostMapping("/rules")
    public ResponseEntity<ActionAlertRule> createRule(@Valid @RequestBody CreateAlertRuleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        ActionAlertRule rule = ruleService.createRule(
                tenantId, request.name(), request.ruleType(), request.scopeType(),
                request.scopeId(), request.thresholdValue(), request.thresholdOperator(),
                request.timeWindowMinutes(), request.cooldownMinutes(), request.severity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @GetMapping("/rules")
    public ResponseEntity<List<ActionAlertRule>> listRules() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ruleService.getRules(tenantId));
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<ActionAlertRule> getRule(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ruleService.getRule(tenantId, id));
    }

    @PostMapping("/rules/{id}/simulate")
    public ResponseEntity<RuleSimulationResult> simulateRule(@PathVariable UUID id,
                                                            @Valid @RequestBody SimulateRuleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RuleSimulationResult result = ruleService.simulateRule(
                tenantId, id, request.observedValue(), request.targetContext(), request.persistAlert()
        );
        return ResponseEntity.ok(result);
    }

    // 2. Inbox de Alertas Operacionais
    @GetMapping("/alerts")
    public ResponseEntity<List<ActionAlert>> listAlerts(
            @RequestParam(required = false) ActionAlertStatus status,
            @RequestParam(required = false) ActionAlertDomain domain) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(alertService.getAlerts(tenantId, status, domain));
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<ActionAlert> getAlert(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(alertService.getAlert(tenantId, id));
    }

    @PatchMapping("/alerts/{id}/status")
    public ResponseEntity<ActionAlert> updateAlertStatus(@PathVariable UUID id,
                                                        @RequestParam ActionAlertStatus status) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(alertService.updateStatus(tenantId, id, status));
    }

    // 3. Tarefas Operacionais (Action Center)
    @GetMapping("/tasks")
    public ResponseEntity<List<ActionTask>> listTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) UUID assignedToUserId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(taskService.getTasks(tenantId, status, assignedToUserId));
    }

    @PostMapping("/tasks")
    public ResponseEntity<ActionTask> createTask(@Valid @RequestBody CreateTaskRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getCurrentUserId().orElse(UUID.randomUUID());
        ActionTask task = taskService.createTask(
                tenantId, userId, request.title(), request.description(),
                request.priority(), request.assignedToUserId(), request.dueDate(), request.evidenceSummary()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PostMapping("/tasks/from-alert/{alertId}")
    public ResponseEntity<ActionTask> createTaskFromAlert(@PathVariable UUID alertId,
                                                          @RequestBody(required = false) CreateTaskFromAlertRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getCurrentUserId().orElse(UUID.randomUUID());

        ActionTask task = taskService.createTaskFromAlert(
                tenantId,
                alertId,
                userId,
                request != null ? request.priority() : TaskPriority.MEDIUM,
                request != null ? request.assignedToUserId() : null,
                request != null ? request.dueDate() : null,
                request != null ? request.customTitle() : null,
                request != null ? request.customNotes() : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<ActionTask> getTask(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(taskService.getTask(tenantId, id));
    }

    @PatchMapping("/tasks/{id}/status")
    public ResponseEntity<ActionTask> updateTaskStatus(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateTaskStatusRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getCurrentUserId().orElse(UUID.randomUUID());
        return ResponseEntity.ok(taskService.updateTaskStatus(tenantId, id, userId, request.status(), request.notes()));
    }

    @PatchMapping("/tasks/{id}/assign")
    public ResponseEntity<ActionTask> assignTask(@PathVariable UUID id,
                                                 @Valid @RequestBody AssignTaskRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getCurrentUserId().orElse(UUID.randomUUID());
        return ResponseEntity.ok(taskService.assignTask(tenantId, id, userId, request.assignedToUserId()));
    }

    @GetMapping("/tasks/{id}/audits")
    public ResponseEntity<List<ActionTaskAudit>> getTaskAudits(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(taskService.getTaskAudits(tenantId, id));
    }

    // 4. Governança de Notificações e Opt-in
    @GetMapping("/notifications/preferences")
    public ResponseEntity<List<ActionNotificationPreference>> getPreferences() {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getCurrentUserId().orElse(UUID.randomUUID());
        return ResponseEntity.ok(notificationService.getPreferences(tenantId, userId));
    }

    @PostMapping("/notifications/preferences")
    public ResponseEntity<ActionNotificationPreference> setPreference(@Valid @RequestBody SetNotificationPreferenceRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getCurrentUserId().orElse(UUID.randomUUID());
        ActionNotificationPreference pref = notificationService.setPreference(
                tenantId, userId, request.channel(), request.optedIn(),
                request.targetAddress(), request.frequencyLimitPerHour()
        );
        return ResponseEntity.ok(pref);
    }

    @GetMapping("/notifications/deliveries")
    public ResponseEntity<List<ActionNotificationDelivery>> listDeliveries() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(notificationService.getDeliveriesForTenant(tenantId));
    }
}
