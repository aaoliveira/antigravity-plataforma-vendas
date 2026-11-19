package com.sellerintelligence.modules.aiassistant.draft.model;

import com.sellerintelligence.modules.aiassistant.draft.entity.DraftListingVersion;

import java.time.Instant;
import java.util.UUID;

public record DraftListingVersionDto(
        UUID id,
        UUID draftListingId,
        int versionNumber,
        String promptTemplateId,
        String promptTemplateVersion,
        String aiModelName,
        String aiProvider,
        String tone,
        String targetMarketplace,
        String inputAttributesJson,
        String generatedTitle,
        String generatedDescription,
        String reviewedTitle,
        String reviewedDescription,
        String effectiveTitle,
        String effectiveDescription,
        String traceableAttributesJson,
        String unprovenClaimsJson,
        int titleCharCount,
        int descriptionCharCount,
        boolean characterLimitValid,
        boolean userEdited,
        String reviewNotes,
        Instant createdAt
) {
    public static DraftListingVersionDto from(DraftListingVersion entity) {
        String effectiveTitle = entity.getReviewedTitle() != null && !entity.getReviewedTitle().isBlank()
                ? entity.getReviewedTitle()
                : entity.getGeneratedTitle();
        String effectiveDescription = entity.getReviewedDescription() != null && !entity.getReviewedDescription().isBlank()
                ? entity.getReviewedDescription()
                : entity.getGeneratedDescription();

        return new DraftListingVersionDto(
                entity.getId(),
                entity.getDraftListingId(),
                entity.getVersionNumber(),
                entity.getPromptTemplateId(),
                entity.getPromptTemplateVersion(),
                entity.getAiModelName(),
                entity.getAiProvider(),
                entity.getTone(),
                entity.getTargetMarketplace(),
                entity.getInputAttributesJson(),
                entity.getGeneratedTitle(),
                entity.getGeneratedDescription(),
                entity.getReviewedTitle(),
                entity.getReviewedDescription(),
                effectiveTitle,
                effectiveDescription,
                entity.getTraceableAttributesJson(),
                entity.getUnprovenClaimsJson(),
                entity.getTitleCharCount(),
                entity.getDescriptionCharCount(),
                entity.getCharacterLimitValid(),
                entity.getUserEdited(),
                entity.getReviewNotes(),
                entity.getCreatedAt()
        );
    }
}
