package br.com.sellerintelligence.action.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "action_tasks")
public class ActionTask {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status;

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "evidence_summary", columnDefinition = "TEXT")
    private String evidenceSummary;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ActionTask() {}

    public ActionTask(UUID tenantId, UUID alertId, String title, String description,
                      TaskPriority priority, UUID assignedToUserId, Instant dueDate,
                      String evidenceSummary, UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.alertId = alertId;
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.priority = priority != null ? priority : TaskPriority.MEDIUM;
        this.status = TaskStatus.TODO;
        this.assignedToUserId = assignedToUserId;
        this.dueDate = dueDate;
        this.evidenceSummary = evidenceSummary;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void assignTo(UUID newAssigneeUserId) {
        this.assignedToUserId = newAssigneeUserId;
        this.updatedAt = Instant.now();
    }

    public void updateStatus(TaskStatus newStatus, String notes) {
        this.status = Objects.requireNonNull(newStatus, "newStatus cannot be null");
        if (notes != null && !notes.isBlank()) {
            this.completionNotes = notes;
        }
        if (newStatus == TaskStatus.DONE || newStatus == TaskStatus.CANCELLED) {
            this.completedAt = Instant.now();
        } else {
            this.completedAt = null;
        }
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getAlertId() { return alertId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskPriority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public UUID getAssignedToUserId() { return assignedToUserId; }
    public Instant getDueDate() { return dueDate; }
    public String getEvidenceSummary() { return evidenceSummary; }
    public String getCompletionNotes() { return completionNotes; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
