package br.com.sellerintelligence.ai.dto;

public record CriterionEvaluationDto(
        String id,
        String name,
        int weight,
        int pointsEarned,
        int maxPoints,
        String status, // GOOD, NEEDS_IMPROVEMENT, CRITICAL
        String evidence,
        String recommendation
) {}
