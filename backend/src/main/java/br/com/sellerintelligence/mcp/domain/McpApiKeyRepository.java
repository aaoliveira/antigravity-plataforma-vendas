package br.com.sellerintelligence.mcp.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface McpApiKeyRepository extends JpaRepository<McpApiKey, UUID> {

    Optional<McpApiKey> findByKeyHash(String keyHash);

    List<McpApiKey> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<McpApiKey> findByIdAndTenantId(UUID id, UUID tenantId);
}
