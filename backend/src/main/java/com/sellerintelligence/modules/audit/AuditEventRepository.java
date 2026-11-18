package com.sellerintelligence.modules.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findByEventType(String eventType);

    List<AuditEvent> findByTenantId(UUID tenantId);

    List<AuditEvent> findByPrincipalEmail(String principalEmail);
}
