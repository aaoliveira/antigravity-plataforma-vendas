package br.com.sellerintelligence.tenancy.domain;

public enum Role {
    OWNER,
    ADMIN,
    MANAGER,
    ANALYST,
    FINANCE,
    OPERATOR,
    VIEWER;

    public boolean canManageMembers() {
        return this == OWNER || this == ADMIN;
    }

    public boolean canAssignRole(Role targetRole) {
        if (this == OWNER) {
            return true;
        }
        if (this == ADMIN) {
            return targetRole != OWNER;
        }
        return false;
    }

    public boolean canManageOrganization() {
        return this == OWNER;
    }
}
