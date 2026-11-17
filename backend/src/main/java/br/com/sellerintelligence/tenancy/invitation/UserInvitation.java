package br.com.sellerintelligence.tenancy.invitation;

import br.com.sellerintelligence.tenancy.domain.Role;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_invitations")
public class UserInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "allowed_company_ids")
    private String allowedCompanyIds;

    @Column(name = "allowed_store_ids")
    private String allowedStoreIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "invited_by_user_id", nullable = false)
    private UUID invitedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UserInvitation() {
    }

    public UserInvitation(UUID tenantId, String email, Role role, String token,
                          String allowedCompanyIds, String allowedStoreIds,
                          OffsetDateTime expiresAt, UUID invitedByUserId) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.token = Objects.requireNonNull(token, "token must not be null");
        this.allowedCompanyIds = allowedCompanyIds;
        this.allowedStoreIds = allowedStoreIds;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.invitedByUserId = Objects.requireNonNull(invitedByUserId, "invitedByUserId must not be null");
        this.status = InvitationStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(this.expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAllowedCompanyIds() {
        return allowedCompanyIds;
    }

    public void setAllowedCompanyIds(String allowedCompanyIds) {
        this.allowedCompanyIds = allowedCompanyIds;
    }

    public String getAllowedStoreIds() {
        return allowedStoreIds;
    }

    public void setAllowedStoreIds(String allowedStoreIds) {
        this.allowedStoreIds = allowedStoreIds;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(OffsetDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public UUID getInvitedByUserId() {
        return invitedByUserId;
    }

    public void setInvitedByUserId(UUID invitedByUserId) {
        this.invitedByUserId = invitedByUserId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
