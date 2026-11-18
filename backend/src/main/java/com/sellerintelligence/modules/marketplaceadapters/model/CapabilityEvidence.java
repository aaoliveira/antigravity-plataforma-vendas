package com.sellerintelligence.modules.marketplaceadapters.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Cadastro imutável de evidência documental de uma capacidade de marketplace.
 * Garante que nenhuma capacidade seja executada sem fundamentação em documentação oficial.
 */
public record CapabilityEvidence(
        String officialUrl,
        LocalDate verificationDate,
        List<String> requiredScopes,
        String rateLimitsAndQuotas,
        String restrictions,
        String documentationVersion,
        String responsibleReviewer
) {

    public CapabilityEvidence {
        Objects.requireNonNull(officialUrl, "officialUrl é obrigatória");
        Objects.requireNonNull(verificationDate, "verificationDate é obrigatória");
        Objects.requireNonNull(rateLimitsAndQuotas, "rateLimitsAndQuotas é obrigatória");
        Objects.requireNonNull(responsibleReviewer, "responsibleReviewer é obrigatório");

        if (officialUrl.isBlank()) {
            throw new IllegalArgumentException("officialUrl não pode ser vazia");
        }
        if (requiredScopes == null) {
            requiredScopes = Collections.emptyList();
        } else {
            requiredScopes = List.copyOf(requiredScopes);
        }
        if (restrictions == null) {
            restrictions = "Nenhuma restrição específica";
        }
        if (documentationVersion == null) {
            documentationVersion = "Vigente";
        }
    }
}
