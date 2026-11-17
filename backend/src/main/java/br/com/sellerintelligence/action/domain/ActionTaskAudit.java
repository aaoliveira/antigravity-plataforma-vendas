package br.com.sellerintelligence.action.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "action_task_audits")
public class ActionTaskAudit {

    @Id
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 64)
    private TaskAuditAction actionType;

    @Column(name = "previous_state", length = 64)
    private String previousState;

    @Column(name = "new_state", nullable = false, length = 64)
    private String newState;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ActionTaskAudit() {}

    public ActionTaskAudit(UUID taskId, UUID actorUserId, TaskAuditAction actionType,
                           String previousState, String newState, String notes) {
        this.id = UUID.randomUUID();
        this.taskId = Objects.requireNonNull(taskId, "taskId cannot be null");
        this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId cannot be null");
        this.actionType = Objects.requireNonNull(actionType, "actionType cannot be null");
        this.previousState = previousState;
        this.newState = Objects.requireNonNull(newState, "newState cannot be null");
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTaskId() { return taskId; }
    public UUID getActorUserId() { return actorUserId; }
    public TaskAuditAction getActionType() { return actionType; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}
