package com.sellerintelligence.modules.aiassistant.draft.model;

/**
 * Tons de redação autorizados para geração assistida de anúncios (P020-BE).
 */
public enum DraftTone {
    INFORMATIVE_OBJECTIVE("Informativo e Objetivo", "Tom neutro, conciso, focado em especificações reais e benefícios diretos"),
    TECHNICAL("Técnico e Detalhado", "Focado em terminologia técnica, especificações de engenharia e medidas confirmadas"),
    COMMERCIAL_POLITE("Comercial e Cortês", "Persuasivo moderado, profissional e elegante, sem superlativos proibidos");

    private final String displayName;
    private final String description;

    DraftTone(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
