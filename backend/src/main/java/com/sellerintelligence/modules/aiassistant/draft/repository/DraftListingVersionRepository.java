package com.sellerintelligence.modules.aiassistant.draft.repository;

import com.sellerintelligence.modules.aiassistant.draft.entity.DraftListingVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DraftListingVersionRepository extends JpaRepository<DraftListingVersion, UUID> {

    Optional<DraftListingVersion> findByTenantIdAndDraftListingIdAndVersionNumber(
            UUID tenantId,
            UUID draftListingId,
            Integer versionNumber
    );

    List<DraftListingVersion> findByTenantIdAndDraftListingIdOrderByVersionNumberDesc(
            UUID tenantId,
            UUID draftListingId
    );
}
