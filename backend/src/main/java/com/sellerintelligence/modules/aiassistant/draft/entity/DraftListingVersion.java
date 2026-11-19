package com.sellerintelligence.modules.aiassistant.draft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Versão imutável de rascunho de anúncio gerado ou editado (P020-BE).
 * Preserva histórico integral, rastreabilidade de claims, prompt utilizado e contagem de caracteres.
 */
@Entity
@Table(name = "draft_listing_versions")
public class DraftListingVersion {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "draft_listing_id", nullable = false, updatable = false)
    private UUID draftListingId;

    @Column(name = "version_number", nullable = false, updatable = false)
    private Integer versionNumber;

    @Column(name = "prompt_template_id", nullable = false, length = 100)
    private String promptTemplateId;

    @Column(name = "prompt_template_version", nullable = false, length = 50)
    private String promptTemplateVersion;

    @Column(name = "ai_model_name", nullable = false, length = 100)
    private String aiModelName;

    @Column(name = "ai_provider", nullable = false, length = 50)
    private String aiProvider;

    @Column(name = "tone", nullable = false, length = 50)
    private String tone;

    @Column(name = "target_marketplace", nullable = false, length = 50)
    private String targetMarketplace;

    @Column(name = "input_attributes_json", nullable = false, columnDefinition = "TEXT")
    private String inputAttributesJson;

    @Column(name = "generated_title", nullable = false, length = 255)
    private String generatedTitle;

    @Column(name = "generated_description", nullable = false, columnDefinition = "TEXT")
    private String generatedDescription;

    @Column(name = "reviewed_title", length = 255)
    private String reviewedTitle;

    @Column(name = "reviewed_description", columnDefinition = "TEXT")
    private String reviewedDescription;

    @Column(name = "traceable_attributes_json", nullable = false, columnDefinition = "TEXT")
    private String traceableAttributesJson;

    @Column(name = "unproven_claims_json", columnDefinition = "TEXT")
    private String unprovenClaimsJson;

    @Column(name = "title_char_count", nullable = false)
    private Integer titleCharCount;

    @Column(name = "description_char_count", nullable = false)
    private Integer descriptionCharCount;

    @Column(name = "character_limit_valid", nullable = false)
    private Boolean characterLimitValid;

    @Column(name = "user_edited", nullable = false)
    private Boolean userEdited;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DraftListingVersion() {
        // JPA requirement
    }

    public DraftListingVersion(
            UUID id,
            UUID tenantId,
            UUID draftListingId,
            Integer versionNumber,
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
            String traceableAttributesJson,
            String unprovenClaimsJson,
            Integer titleCharCount,
            Integer descriptionCharCount,
            Boolean characterLimitValid,
            Boolean userEdited,
            String reviewNotes
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.draftListingId = Objects.requireNonNull(draftListingId, "draftListingId não pode ser nulo");
        this.versionNumber = Objects.requireNonNull(versionNumber, "versionNumber não pode ser nulo");
        this.promptTemplateId = Objects.requireNonNull(promptTemplateId, "promptTemplateId não pode ser nulo");
        this.promptTemplateVersion = Objects.requireNonNull(promptTemplateVersion, "promptTemplateVersion não pode ser nulo");
        this.aiModelName = Objects.requireNonNull(aiModelName, "aiModelName não pode ser nulo");
        this.aiProvider = Objects.requireNonNull(aiProvider, "aiProvider não pode ser nulo");
        this.tone = Objects.requireNonNull(tone, "tone não pode ser nulo");
        this.targetMarketplace = Objects.requireNonNull(targetMarketplace, "targetMarketplace não pode ser nulo");
        this.inputAttributesJson = Objects.requireNonNull(inputAttributesJson, "inputAttributesJson não pode ser nulo");
        this.generatedTitle = Objects.requireNonNull(generatedTitle, "generatedTitle não pode ser nulo");
        this.generatedDescription = Objects.requireNonNull(generatedDescription, "generatedDescription não pode ser nulo");
        this.reviewedTitle = reviewedTitle;
        this.reviewedDescription = reviewedDescription;
        this.traceableAttributesJson = Objects.requireNonNull(traceableAttributesJson, "traceableAttributesJson não pode ser nulo");
        this.unprovenClaimsJson = unprovenClaimsJson;
        this.titleCharCount = titleCharCount != null ? titleCharCount : 0;
        this.descriptionCharCount = descriptionCharCount != null ? descriptionCharCount : 0;
        this.characterLimitValid = characterLimitValid != null ? characterLimitValid : false;
        this.userEdited = userEdited != null ? userEdited : false;
        this.reviewNotes = reviewNotes;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getDraftListingId() {
        return draftListingId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public String getPromptTemplateVersion() {
        return promptTemplateVersion;
    }

    public String getAiModelName() {
        return aiModelName;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getTone() {
        return tone;
    }

    public String getTargetMarketplace() {
        return targetMarketplace;
    }

    public String getInputAttributesJson() {
        return inputAttributesJson;
    }

    public String getGeneratedTitle() {
        return generatedTitle;
    }

    public String getGeneratedDescription() {
        return generatedDescription;
    }

    public String getReviewedTitle() {
        return reviewedTitle;
    }

    public void setReviewedTitle(String reviewedTitle) {
        this.reviewedTitle = reviewedTitle;
    }

    public String getReviewedDescription() {
        return reviewedDescription;
    }

    public void setReviewedDescription(String reviewedDescription) {
        this.reviewedDescription = reviewedDescription;
    }

    public String getTraceableAttributesJson() {
        return traceableAttributesJson;
    }

    public String getUnprovenClaimsJson() {
        return unprovenClaimsJson;
    }

    public Integer getTitleCharCount() {
        return titleCharCount;
    }

    public void setTitleCharCount(Integer titleCharCount) {
        this.titleCharCount = titleCharCount;
    }

    public Integer getDescriptionCharCount() {
        return descriptionCharCount;
    }

    public void setDescriptionCharCount(Integer descriptionCharCount) {
        this.descriptionCharCount = descriptionCharCount;
    }

    public Boolean getCharacterLimitValid() {
        return characterLimitValid;
    }

    public void setCharacterLimitValid(Boolean characterLimitValid) {
        this.characterLimitValid = characterLimitValid;
    }

    public Boolean getUserEdited() {
        return userEdited;
    }

    public void setUserEdited(Boolean userEdited) {
        this.userEdited = userEdited;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
