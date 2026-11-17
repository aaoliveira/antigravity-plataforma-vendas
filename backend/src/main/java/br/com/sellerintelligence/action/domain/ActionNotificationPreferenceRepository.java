package br.com.sellerintelligence.action.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionNotificationPreferenceRepository extends JpaRepository<ActionNotificationPreference, UUID> {

    List<ActionNotificationPreference> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    Optional<ActionNotificationPreference> findByTenantIdAndUserIdAndChannel(UUID tenantId, UUID userId, NotificationChannel channel);

    List<ActionNotificationPreference> findByTenantId(UUID tenantId);
}
