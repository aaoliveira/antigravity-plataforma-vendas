package com.sellerintelligence.modules.aiassistant.draft.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.modules.aiassistant.draft.catalog.MarketplaceLimitCatalog;
import com.sellerintelligence.modules.aiassistant.draft.entity.DraftListing;
import com.sellerintelligence.modules.aiassistant.draft.entity.DraftListingVersion;
import com.sellerintelligence.modules.aiassistant.draft.guardrails.ClaimViolationException;
import com.sellerintelligence.modules.aiassistant.draft.guardrails.ListingClaimGuardrailService;
import com.sellerintelligence.modules.aiassistant.draft.model.ClaimValidationResult;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftApproveRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftEditRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftGenerationRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftListingDetailDto;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftListingSummaryDto;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftListingVersionDto;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftRejectRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftTone;
import com.sellerintelligence.modules.aiassistant.draft.model.MarketplaceRuleLimits;
import com.sellerintelligence.modules.aiassistant.draft.model.MarketplaceRuleLimitsDto;
import com.sellerintelligence.modules.aiassistant.draft.repository.DraftListingRepository;
import com.sellerintelligence.modules.aiassistant.draft.repository.DraftListingVersionRepository;
import com.sellerintelligence.modules.aiassistant.gateway.AiProvider;
import com.sellerintelligence.modules.catalog.Product;
import com.sellerintelligence.modules.catalog.ProductRepository;
import com.sellerintelligence.modules.catalog.ProductVariant;
import com.sellerintelligence.modules.catalog.ProductVariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de geração assistida de anúncios, controle de versões e guardrails de claims (P020-BE).
 * Restringe claims e valida limites de caracteres por marketplace oficial.
 * Nenhuma publicação externa é executada no MVP.
 */
@Service
public class DraftListingService {

    private final DraftListingRepository draftListingRepository;
    private final DraftListingVersionRepository draftVersionRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final MarketplaceLimitCatalog marketplaceLimitCatalog;
    private final ListingClaimGuardrailService claimGuardrailService;
    private final ObjectMapper objectMapper;

    public DraftListingService(
            DraftListingRepository draftListingRepository,
            DraftListingVersionRepository draftVersionRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            MarketplaceLimitCatalog marketplaceLimitCatalog,
            ListingClaimGuardrailService claimGuardrailService,
            ObjectMapper objectMapper
    ) {
        this.draftListingRepository = draftListingRepository;
        this.draftVersionRepository = draftVersionRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.marketplaceLimitCatalog = marketplaceLimitCatalog;
        this.claimGuardrailService = claimGuardrailService;
        this.objectMapper = objectMapper;
    }

    /**
     * Gera um novo rascunho de anúncio com versão 1 a partir de atributos confirmados.
     */
    @Transactional
    public DraftListingDetailDto generateDraft(UUID tenantId, DraftGenerationRequest request) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(request, "request não pode ser nulo");

        String marketplace = request.targetMarketplace() != null ? request.targetMarketplace().toUpperCase(Locale.ROOT) : "";
        MarketplaceRuleLimits limits = marketplaceLimitCatalog.getLimits(marketplace).orElseThrow(() ->
                new IllegalArgumentException("Marketplace '" + request.targetMarketplace() + "' não possui regras oficiais de limites de caracteres validadas no MVP"));

        // 1. Extrair atributos confirmados (do produto de catálogo ou informados na requisição)
        Map<String, Object> confirmedAttributes = resolveConfirmedAttributes(tenantId, request);
        if (confirmedAttributes.isEmpty() && (request.keywords() == null || request.keywords().isEmpty())) {
            throw new IllegalArgumentException("Pelo menos um atributo confirmado do produto ou palavra-chave deve ser fornecido para geração do rascunho.");
        }

        DraftTone tone = request.tone() != null ? request.tone() : DraftTone.INFORMATIVE_OBJECTIVE;
        List<String> keywords = request.keywords() != null ? request.keywords() : List.of();

        // 2. Gerar título e descrição determinísticos respeitando limites oficiais e atributos confirmados
        String generatedTitle = generateTitle(marketplace, limits, tone, confirmedAttributes, keywords);
        String generatedDescription = generateDescription(marketplace, limits, tone, confirmedAttributes, keywords, generatedTitle);

        // 3. Auditoria de claims e rastreabilidade
        ClaimValidationResult claimResult = claimGuardrailService.validateAndTraceClaims(
                generatedTitle,
                generatedDescription,
                confirmedAttributes,
                keywords
        );

        if (!claimResult.valid()) {
            throw new ClaimViolationException(
                    "Geração rejeitada: foram detectadas alegações ou claims não comprovadas nos atributos fornecidos.",
                    claimResult.unprovenClaimsDetected()
            );
        }

        // 4. Validar limites oficiais de caracteres
        marketplaceLimitCatalog.validateLimits(marketplace, generatedTitle, generatedDescription);

        // 5. Persistir DraftListing
        UUID draftId = UUID.randomUUID();
        DraftListing draftListing = new DraftListing(
                draftId,
                tenantId,
                request.productId(),
                request.productVariantId(),
                marketplace,
                1,
                "DRAFT"
        );
        draftListingRepository.save(draftListing);

        // 6. Persistir DraftListingVersion
        UUID versionId = UUID.randomUUID();
        String promptTemplateId = request.promptTemplateId() != null && !request.promptTemplateId().isBlank()
                ? request.promptTemplateId()
                : "LISTING_DRAFT_GENERATION";
        AiProvider provider = request.provider() != null ? request.provider() : AiProvider.MOCK_PROVIDER;

        DraftListingVersion version = new DraftListingVersion(
                versionId,
                tenantId,
                draftId,
                1,
                promptTemplateId,
                "v1.0.0",
                "mock-deterministic-v1",
                provider.name(),
                tone.name(),
                marketplace,
                toJson(confirmedAttributes),
                generatedTitle,
                generatedDescription,
                null,
                null,
                toJson(claimResult.traceableAttributes()),
                toJson(claimResult.unprovenClaimsDetected()),
                generatedTitle.length(),
                generatedDescription.length(),
                true,
                false,
                null
        );
        draftVersionRepository.save(version);

        DraftListingVersionDto versionDto = DraftListingVersionDto.from(version);
        return DraftListingDetailDto.of(
                draftListing,
                versionDto,
                List.of(versionDto),
                MarketplaceRuleLimitsDto.from(limits)
        );
    }

    /**
     * Edição do rascunho pelo lojista, gerando uma nova versão com status REVIEWED e auditoria de claims.
     */
    @Transactional
    public DraftListingDetailDto editDraft(UUID tenantId, UUID draftId, DraftEditRequest request) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(draftId, "draftId não pode ser nulo");
        Objects.requireNonNull(request, "request não pode ser nulo");

        DraftListing draft = draftListingRepository.findByIdAndTenantId(draftId, tenantId).orElseThrow(() ->
                new IllegalArgumentException("Rascunho não encontrado com ID: " + draftId));

        MarketplaceRuleLimits limits = marketplaceLimitCatalog.getLimits(draft.getTargetMarketplace()).orElseThrow(() ->
                new IllegalArgumentException("Marketplace '" + draft.getTargetMarketplace() + "' não possui regras validadas"));

        // 1. Validar limites de caracteres no texto editado
        marketplaceLimitCatalog.validateLimits(draft.getTargetMarketplace(), request.reviewedTitle(), request.reviewedDescription());

        // 2. Recuperar a versão anterior para obter os atributos confirmados originais
        DraftListingVersion lastVersion = draftVersionRepository
                .findByTenantIdAndDraftListingIdAndVersionNumber(tenantId, draftId, draft.getCurrentVersionNumber())
                .orElseThrow(() -> new IllegalStateException("Versão anterior do rascunho não encontrada"));

        Map<String, Object> confirmedAttributes = fromJson(lastVersion.getInputAttributesJson(), new TypeReference<>() {});

        // 3. Auditoria de claims sobre a edição humana: proíbe introdução de certificações falsas, superlativos, etc.
        ClaimValidationResult claimResult = claimGuardrailService.validateAndTraceClaims(
                request.reviewedTitle(),
                request.reviewedDescription(),
                confirmedAttributes,
                Collections.emptyList()
        );

        if (!claimResult.valid()) {
            throw new ClaimViolationException(
                    "Edição rejeitada por violar guardrails de claims não comprovadas: " + String.join("; ", claimResult.unprovenClaimsDetected()),
                    claimResult.unprovenClaimsDetected()
            );
        }

        // 4. Incrementar versão
        int nextVersionNumber = draft.getCurrentVersionNumber() + 1;
        draft.setCurrentVersionNumber(nextVersionNumber);
        draft.setStatus("REVIEWED");
        draftListingRepository.save(draft);

        // 5. Salvar nova versão com userEdited = true
        UUID versionId = UUID.randomUUID();
        DraftListingVersion newVersion = new DraftListingVersion(
                versionId,
                tenantId,
                draftId,
                nextVersionNumber,
                lastVersion.getPromptTemplateId(),
                lastVersion.getPromptTemplateVersion(),
                lastVersion.getAiModelName(),
                lastVersion.getAiProvider(),
                lastVersion.getTone(),
                lastVersion.getTargetMarketplace(),
                lastVersion.getInputAttributesJson(),
                lastVersion.getGeneratedTitle(),
                lastVersion.getGeneratedDescription(),
                request.reviewedTitle().trim(),
                request.reviewedDescription().trim(),
                toJson(claimResult.traceableAttributes()),
                toJson(claimResult.unprovenClaimsDetected()),
                request.reviewedTitle().trim().length(),
                request.reviewedDescription().trim().length(),
                true,
                true,
                request.reviewNotes()
        );
        draftVersionRepository.save(newVersion);

        List<DraftListingVersionDto> history = draftVersionRepository
                .findByTenantIdAndDraftListingIdOrderByVersionNumberDesc(tenantId, draftId)
                .stream()
                .map(DraftListingVersionDto::from)
                .toList();

        return DraftListingDetailDto.of(
                draft,
                DraftListingVersionDto.from(newVersion),
                history,
                MarketplaceRuleLimitsDto.from(limits)
        );
    }

    /**
     * Aprovação formal do rascunho pelo lojista (status APPROVED).
     * Nenhuma publicação externa é executada.
     */
    @Transactional
    public DraftListingDetailDto approveDraft(UUID tenantId, UUID draftId, DraftApproveRequest request) {
        DraftListing draft = draftListingRepository.findByIdAndTenantId(draftId, tenantId).orElseThrow(() ->
                new IllegalArgumentException("Rascunho não encontrado com ID: " + draftId));

        DraftListingVersion latestVersion = draftVersionRepository
                .findByTenantIdAndDraftListingIdAndVersionNumber(tenantId, draftId, draft.getCurrentVersionNumber())
                .orElseThrow(() -> new IllegalStateException("Versão ativa do rascunho não encontrada"));

        String effectiveTitle = latestVersion.getReviewedTitle() != null ? latestVersion.getReviewedTitle() : latestVersion.getGeneratedTitle();
        String effectiveDescription = latestVersion.getReviewedDescription() != null ? latestVersion.getReviewedDescription() : latestVersion.getGeneratedDescription();

        // Revalidar limites antes de aprovar
        marketplaceLimitCatalog.validateLimits(draft.getTargetMarketplace(), effectiveTitle, effectiveDescription);

        draft.setStatus("APPROVED");
        draftListingRepository.save(draft);

        if (request != null && request.approvalNotes() != null) {
            latestVersion.setReviewNotes((latestVersion.getReviewNotes() != null ? latestVersion.getReviewNotes() + " | " : "") +
                    "Aprovação: " + request.approvalNotes());
            draftVersionRepository.save(latestVersion);
        }

        MarketplaceRuleLimits limits = marketplaceLimitCatalog.getLimits(draft.getTargetMarketplace()).orElseThrow();
        List<DraftListingVersionDto> history = draftVersionRepository
                .findByTenantIdAndDraftListingIdOrderByVersionNumberDesc(tenantId, draftId)
                .stream()
                .map(DraftListingVersionDto::from)
                .toList();

        return DraftListingDetailDto.of(
                draft,
                DraftListingVersionDto.from(latestVersion),
                history,
                MarketplaceRuleLimitsDto.from(limits)
        );
    }

    /**
     * Rejeição formal do rascunho pelo lojista (status REJECTED).
     */
    @Transactional
    public DraftListingDetailDto rejectDraft(UUID tenantId, UUID draftId, DraftRejectRequest request) {
        Objects.requireNonNull(request, "request não pode ser nulo");
        DraftListing draft = draftListingRepository.findByIdAndTenantId(draftId, tenantId).orElseThrow(() ->
                new IllegalArgumentException("Rascunho não encontrado com ID: " + draftId));

        draft.setStatus("REJECTED");
        draftListingRepository.save(draft);

        DraftListingVersion latestVersion = draftVersionRepository
                .findByTenantIdAndDraftListingIdAndVersionNumber(tenantId, draftId, draft.getCurrentVersionNumber())
                .orElseThrow(() -> new IllegalStateException("Versão ativa do rascunho não encontrada"));

        latestVersion.setReviewNotes((latestVersion.getReviewNotes() != null ? latestVersion.getReviewNotes() + " | " : "") +
                "Rejeição: " + request.rejectionReason());
        draftVersionRepository.save(latestVersion);

        MarketplaceRuleLimits limits = marketplaceLimitCatalog.getLimits(draft.getTargetMarketplace()).orElseThrow();
        List<DraftListingVersionDto> history = draftVersionRepository
                .findByTenantIdAndDraftListingIdOrderByVersionNumberDesc(tenantId, draftId)
                .stream()
                .map(DraftListingVersionDto::from)
                .toList();

        return DraftListingDetailDto.of(
                draft,
                DraftListingVersionDto.from(latestVersion),
                history,
                MarketplaceRuleLimitsDto.from(limits)
        );
    }

    /**
     * Consulta detalhada de um rascunho por ID e tenant.
     */
    @Transactional(readOnly = true)
    public DraftListingDetailDto getDraft(UUID tenantId, UUID draftId) {
        DraftListing draft = draftListingRepository.findByIdAndTenantId(draftId, tenantId).orElseThrow(() ->
                new IllegalArgumentException("Rascunho não encontrado com ID: " + draftId));

        DraftListingVersion latestVersion = draftVersionRepository
                .findByTenantIdAndDraftListingIdAndVersionNumber(tenantId, draftId, draft.getCurrentVersionNumber())
                .orElseThrow(() -> new IllegalStateException("Versão ativa do rascunho não encontrada"));

        List<DraftListingVersionDto> history = draftVersionRepository
                .findByTenantIdAndDraftListingIdOrderByVersionNumberDesc(tenantId, draftId)
                .stream()
                .map(DraftListingVersionDto::from)
                .toList();

        MarketplaceRuleLimits limits = marketplaceLimitCatalog.getLimits(draft.getTargetMarketplace()).orElse(null);

        return DraftListingDetailDto.of(
                draft,
                DraftListingVersionDto.from(latestVersion),
                history,
                limits != null ? MarketplaceRuleLimitsDto.from(limits) : null
        );
    }

    /**
     * Consulta de uma versão histórica específica.
     */
    @Transactional(readOnly = true)
    public DraftListingVersionDto getDraftVersion(UUID tenantId, UUID draftId, Integer versionNumber) {
        DraftListingVersion version = draftVersionRepository
                .findByTenantIdAndDraftListingIdAndVersionNumber(tenantId, draftId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Versão " + versionNumber + " do rascunho " + draftId + " não encontrada"));
        return DraftListingVersionDto.from(version);
    }

    /**
     * Listagem paginada de rascunhos com filtros opcionais por marketplace e status.
     */
    @Transactional(readOnly = true)
    public Page<DraftListingSummaryDto> listDrafts(
            UUID tenantId,
            String targetMarketplace,
            String status,
            Pageable pageable
    ) {
        String mkt = targetMarketplace != null && !targetMarketplace.isBlank() ? targetMarketplace.toUpperCase(Locale.ROOT) : null;
        String st = status != null && !status.isBlank() ? status.toUpperCase(Locale.ROOT) : null;

        Page<DraftListing> page;
        if (mkt != null && st != null) {
            page = draftListingRepository.findByTenantIdAndTargetMarketplaceAndStatus(tenantId, mkt, st, pageable);
        } else if (mkt != null) {
            page = draftListingRepository.findByTenantIdAndTargetMarketplace(tenantId, mkt, pageable);
        } else if (st != null) {
            page = draftListingRepository.findByTenantIdAndStatus(tenantId, st, pageable);
        } else {
            page = draftListingRepository.findByTenantId(tenantId, pageable);
        }

        return page.map(DraftListingSummaryDto::from);
    }

    /**
     * Consulta as regras e limites de caracteres homologados por canal.
     */
    public List<MarketplaceRuleLimitsDto> listMarketplaceRules() {
        return marketplaceLimitCatalog.listAllValidatedLimits()
                .stream()
                .map(MarketplaceRuleLimitsDto::from)
                .toList();
    }

    // =========================================================================
    // Métodos Auxiliares de Resolução e Geração
    // =========================================================================

    private Map<String, Object> resolveConfirmedAttributes(UUID tenantId, DraftGenerationRequest request) {
        Map<String, Object> attributes = new LinkedHashMap<>();

        // 1. Carregar produto de catálogo se informado
        if (request.productId() != null) {
            Optional<Product> prodOpt = productRepository.findByIdAndTenantId(request.productId(), tenantId);
            if (prodOpt.isPresent()) {
                Product prod = prodOpt.get();
                if (prod.getTitle() != null) attributes.put("productName", prod.getTitle());
                if (prod.getBrand() != null) attributes.put("brand", prod.getBrand());
                if (prod.getCategory() != null) attributes.put("category", prod.getCategory());
                if (prod.getCanonicalSku() != null) attributes.put("canonicalSku", prod.getCanonicalSku());
            }
        }

        // 2. Carregar variante se informada
        if (request.productVariantId() != null) {
            Optional<ProductVariant> varOpt = productVariantRepository.findByIdAndTenantId(request.productVariantId(), tenantId);
            if (varOpt.isPresent()) {
                ProductVariant var = varOpt.get();
                if (var.getAttributesJson() != null) attributes.put("variantAttributes", var.getAttributesJson());
                if (var.getGtinBarcode() != null) attributes.put("gtinBarcode", var.getGtinBarcode());
                if (var.getVariantSku() != null) attributes.put("variantSku", var.getVariantSku());
            }
        }

        // 3. Sobrescrever / mesclar com atributos confirmados informados explicitamente
        if (request.brand() != null && !request.brand().isBlank()) attributes.put("brand", request.brand().trim());
        if (request.model() != null && !request.model().isBlank()) attributes.put("model", request.model().trim());
        if (request.category() != null && !request.category().isBlank()) attributes.put("category", request.category().trim());
        if (request.materials() != null && !request.materials().isEmpty()) attributes.put("materials", request.materials());
        if (request.dimensions() != null && !request.dimensions().isEmpty()) attributes.put("dimensions", request.dimensions());
        if (request.warranty() != null && !request.warranty().isBlank()) attributes.put("warranty", request.warranty().trim());
        if (request.technicalSpecifications() != null && !request.technicalSpecifications().isEmpty()) {
            attributes.put("technicalSpecifications", request.technicalSpecifications());
        }
        if (request.confirmedBenefits() != null && !request.confirmedBenefits().isEmpty()) {
            attributes.put("confirmedBenefits", request.confirmedBenefits());
        }

        return attributes;
    }

    private String generateTitle(
            String marketplace,
            MarketplaceRuleLimits limits,
            DraftTone tone,
            Map<String, Object> attrs,
            List<String> keywords
    ) {
        String brand = attrs.getOrDefault("brand", "").toString().trim();
        String model = attrs.getOrDefault("model", "").toString().trim();
        String category = attrs.getOrDefault("category", attrs.getOrDefault("productName", "Produto")).toString().trim();

        Object materialsObj = attrs.get("materials");
        String materialFirst = "";
        if (materialsObj instanceof List<?> list && !list.isEmpty()) {
            materialFirst = list.get(0).toString().trim();
        }

        String firstKeyword = (keywords != null && !keywords.isEmpty()) ? keywords.get(0).trim() : "";

        StringBuilder titleBuilder = new StringBuilder();

        // Variação conforme Tom selecionado
        if (tone == DraftTone.TECHNICAL) {
            titleBuilder.append(category);
            if (!model.isBlank()) titleBuilder.append(" ").append(model);
            if (!brand.isBlank()) titleBuilder.append(" ").append(brand);
            if (!materialFirst.isBlank()) titleBuilder.append(" ").append(materialFirst);
        } else if (tone == DraftTone.COMMERCIAL_POLITE) {
            titleBuilder.append(category);
            if (!brand.isBlank()) titleBuilder.append(" ").append(brand);
            if (!model.isBlank()) titleBuilder.append(" ").append(model);
            if (!firstKeyword.isBlank()) titleBuilder.append(" ").append(firstKeyword);
        } else { // INFORMATIVE_OBJECTIVE
            titleBuilder.append(category);
            if (!brand.isBlank()) titleBuilder.append(" ").append(brand);
            if (!model.isBlank()) titleBuilder.append(" ").append(model);
            if (!materialFirst.isBlank()) titleBuilder.append(" ").append(materialFirst);
            if (!firstKeyword.isBlank()) titleBuilder.append(" ").append(firstKeyword);
        }

        String rawTitle = titleBuilder.toString().replaceAll("\\s+", " ").trim();

        // Ajuste de Limite de Caracteres por Canal
        if ("MERCADO_LIVRE".equals(marketplace)) {
            // Limite estrito Meli: max 60 chars
            if (rawTitle.length() > limits.maxTitleLength()) {
                rawTitle = truncateAtWordBoundary(rawTitle, limits.maxTitleLength());
            }
        } else if ("SHOPEE".equals(marketplace)) {
            // Limite Shopee: [20 - 120] chars
            if (rawTitle.length() < limits.minTitleLength()) {
                // Completar com atributos confirmados para atingir 20 chars
                if (!brand.isBlank() && !rawTitle.contains(brand)) rawTitle += " " + brand;
                if (!firstKeyword.isBlank() && !rawTitle.contains(firstKeyword)) rawTitle += " " + firstKeyword;
                if (rawTitle.length() < limits.minTitleLength()) {
                    rawTitle += " Original Oficial";
                }
            }
            if (rawTitle.length() > limits.maxTitleLength()) {
                rawTitle = truncateAtWordBoundary(rawTitle, limits.maxTitleLength());
            }
        }

        return rawTitle;
    }

    private String generateDescription(
            String marketplace,
            MarketplaceRuleLimits limits,
            DraftTone tone,
            Map<String, Object> attrs,
            List<String> keywords,
            String title
    ) {
        String brand = attrs.getOrDefault("brand", "Não informada").toString().trim();
        String model = attrs.getOrDefault("model", "Padrão").toString().trim();
        String category = attrs.getOrDefault("category", "Produto").toString().trim();
        String warranty = attrs.getOrDefault("warranty", "Garantia legal de 90 dias conforme CDC").toString().trim();

        StringBuilder sb = new StringBuilder();

        // 1. Cabeçalho / Apresentação
        sb.append(title).append("\n\n");
        sb.append("[DESCRIÇÃO DO PRODUTO]\n");
        if (tone == DraftTone.TECHNICAL) {
            sb.append("Apresentamos o modelo ").append(model).append(" da marca ").append(brand)
              .append(". Item projetado para atender aos parâmetros técnicos de robustez e confiabilidade na categoria de ")
              .append(category).append(".\n\n");
        } else if (tone == DraftTone.COMMERCIAL_POLITE) {
            sb.append("Conheça o(a) ").append(title).append(", uma excelente escolha para quem valoriza a procedência da marca ")
              .append(brand).append(" com acabamento impecável e alta usabilidade no dia a dia.\n\n");
        } else {
            sb.append("O(A) ").append(title).append(" reúne características essenciais e atributos confirmados para atender suas necessidades com segurança e durabilidade.\n\n");
        }

        // 2. Especificações Técnicas Confirmadas
        sb.append("[ESPECIFICAÇÕES TÉCNICAS CONFIRMADAS]\n");
        sb.append("• Marca: ").append(brand).append("\n");
        sb.append("• Modelo: ").append(model).append("\n");
        sb.append("• Categoria: ").append(category).append("\n");

        Object materials = attrs.get("materials");
        if (materials instanceof List<?> list && !list.isEmpty()) {
            sb.append("• Materiais Confirmados: ").append(String.join(", ", list.stream().map(Object::toString).toList())).append("\n");
        }

        Object dimensions = attrs.get("dimensions");
        if (dimensions instanceof Map<?, ?> dimMap && !dimMap.isEmpty()) {
            sb.append("• Dimensões / Medidas Confirmadas:\n");
            for (Map.Entry<?, ?> entry : dimMap.entrySet()) {
                sb.append("   - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        Object specs = attrs.get("technicalSpecifications");
        if (specs instanceof Map<?, ?> specMap && !specMap.isEmpty()) {
            sb.append("• Características Adicionais Confirmadas:\n");
            for (Map.Entry<?, ?> entry : specMap.entrySet()) {
                sb.append("   - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        sb.append("• Termo de Garantia: ").append(warranty).append("\n\n");

        // 3. Benefícios Confirmados
        Object benefits = attrs.get("confirmedBenefits");
        if (benefits instanceof List<?> benList && !benList.isEmpty()) {
            sb.append("[BENEFÍCIOS COMPROVADOS]\n");
            for (Object b : benList) {
                sb.append("✓ ").append(b).append("\n");
            }
            sb.append("\n");
        }

        // 4. Palavras-chave de busca
        if (keywords != null && !keywords.isEmpty()) {
            sb.append("[TERMOS RELEVANTES]\n");
            sb.append(String.join(" • ", keywords)).append("\n\n");
        }

        // 5. Orientações Gerais e Avisos de Rascunho
        sb.append("[INFORMAÇÕES DE ATENDIMENTO]\n");
        sb.append("• Produto comercializado com nota fiscal e garantia legal.\n");
        sb.append("• Embalagem segura para integridade no transporte.\n\n");

        sb.append("--- AVISO DE GOVERNANÇA ---\n");
        sb.append("Este conteúdo é um RASCUNHO gerado para o canal ").append(marketplace)
          .append(". Exige revisão e aprovação humana do lojista. Nenhuma publicação externa é efetuada automaticamente.\n");

        String description = sb.toString();

        // Validação de Limites de Descrição
        if (description.length() > limits.maxDescriptionLength()) {
            description = truncateAtWordBoundary(description, limits.maxDescriptionLength());
        }

        return description;
    }

    private String truncateAtWordBoundary(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        int cutIndex = text.lastIndexOf(' ', maxLength);
        if (cutIndex <= 0) {
            cutIndex = maxLength;
        }
        return text.substring(0, cutIndex).trim();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            if (json == null || json.isBlank()) {
                return objectMapper.readValue("{}", typeRef);
            }
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao desserializar JSON de atributos", e);
        }
    }
}
