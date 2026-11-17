package br.com.sellerintelligence.mcp.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface McpToolExecutionLogRepository extends JpaRepository<McpToolExecutionLog, UUID> {

    List<McpToolExecutionLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<McpToolExecutionLog> findByTenantIdAndToolNameOrderByCreatedAtDesc(UUID tenantId, String toolName);
}
