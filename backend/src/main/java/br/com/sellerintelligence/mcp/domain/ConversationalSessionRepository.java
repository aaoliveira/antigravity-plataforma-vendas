package br.com.sellerintelligence.mcp.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationalSessionRepository extends JpaRepository<ConversationalSession, UUID> {

    List<ConversationalSession> findByTenantIdAndUserIdOrderByUpdatedAtDesc(UUID tenantId, UUID userId);

    Optional<ConversationalSession> findByIdAndTenantId(UUID id, UUID tenantId);
}
