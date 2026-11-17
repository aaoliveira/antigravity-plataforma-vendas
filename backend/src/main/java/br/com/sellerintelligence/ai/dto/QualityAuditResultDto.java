package br.com.sellerintelligence.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QualityAuditResultDto(
        UUID listingId,
        int overallScore,
        int titleScore,
        int descriptionScore,
        int attributesScore,
        int imagesScore,
        List<CriterionEvaluationDto> criteriaBreakdown,
        List<String> actionableRecommendations,
        List<String> missingRequiredAttributes,
        Instant auditedAt
) {}
