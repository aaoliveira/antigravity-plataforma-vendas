package br.com.sellerintelligence.mcp.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationalMessageRepository extends JpaRepository<ConversationalMessage, UUID> {

    List<ConversationalMessage> findBySessionIdAndTenantIdOrderByCreatedAtAsc(UUID sessionId, UUID tenantId);
}
