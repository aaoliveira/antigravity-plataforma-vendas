package br.com.sellerintelligence.ai.dto;

public record SaveListingVersionRequest(
        String title,
        String description,
        String attributesJson,
        String status // DRAFT, ACCEPTED, REJECTED
) {}
