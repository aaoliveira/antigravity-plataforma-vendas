package br.com.sellerintelligence.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record ListingVersionDto(
        UUID id,
        UUID listingId,
        int versionNumber,
        String title,
        String description,
        String attributesJson,
        String status,
        String source,
        String promptVersion,
        String modelName,
        String createdBy,
        Instant createdAt
) {}
