package br.com.sellerintelligence.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {

    @Query("SELECT l FROM AiGenerationLog l WHERE l.tenantId = :tenantId ORDER BY l.createdAt DESC")
    List<AiGenerationLog> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") UUID tenantId);
}
