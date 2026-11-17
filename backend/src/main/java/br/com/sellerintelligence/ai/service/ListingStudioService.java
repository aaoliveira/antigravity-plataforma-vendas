package br.com.sellerintelligence.ai.service;

import br.com.sellerintelligence.ai.domain.*;
import br.com.sellerintelligence.ai.dto.*;
import br.com.sellerintelligence.ai.engine.AiPromptSanitizer;
import br.com.sellerintelligence.ai.engine.ListingAuditEngine;
import br.com.sellerintelligence.ai.provider.AiProvider;
import br.com.sellerintelligence.ai.provider.AiServiceUnavailableException;
import br.com.sellerintelligence.ai.provider.ListingAiOutput;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.sync.domain.Listing;
import br.com.sellerintelligence.sync.domain.ListingRepository;
import br.com.sellerintelligence.sync.domain.Product;
import br.com.sellerintelligence.sync.domain.ProductRepository;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class ListingStudioService {

    private static final String PROMPT_TITLE_VERSION = "v1.0.0";
    private static final String PROMPT_DESC_VERSION = "v1.0.0";
    private static final String DISCLAIMER_TEXT = "Sugestão gerada por Inteligência Artificial. Deve ser revisada por humano antes de qualquer uso. Não altera o anúncio no marketplace.";

    private final ListingRepository listingRepository;
    private final ProductRepository productRepository;
    private final ListingVersionRepository versionRepository;
    private final ListingQualityAuditRepository auditRepository;
    private final AiGenerationLogRepository logRepository;
    private final ListingAuditEngine auditEngine;
    private final AiPromptSanitizer promptSanitizer;
    private final AiProvider aiProvider;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public ListingStudioService(
            ListingRepository listingRepository,
            ProductRepository productRepository,
            ListingVersionRepository versionRepository,
            ListingQualityAuditRepository auditRepository,
            AiGenerationLogRepository logRepository,
            ListingAuditEngine auditEngine,
            AiPromptSanitizer promptSanitizer,
            AiProvider aiProvider,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper
    ) {
        this.listingRepository = listingRepository;
        this.productRepository = productRepository;
        this.versionRepository = versionRepository;
        this.auditRepository = auditRepository;
        this.logRepository = logRepository;
        this.auditEngine = auditEngine;
        this.promptSanitizer = promptSanitizer;
        this.aiProvider = aiProvider;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    public QualityAuditResultDto auditListing(UUID listingId) {
        UUID tenantId = TenantContext.getTenantId();
        Listing listing = findListingForTenant(tenantId, listingId);
        Product product = listing.getProductId() != null ? productRepository.findById(listing.getProductId()).orElse(null) : null;

        Map<String, String> attributes = extractAttributesFromRawPayload(listing.getRawPayload());
        if (product != null) {
            if (product.getBrand() != null) attributes.put("marca", product.getBrand());
            if (product.getCategory() != null) attributes.put("categoria", product.getCategory());
        }

        QualityAuditResultDto result = auditEngine.auditListing(
                listing,
                product,
                listing.getRawPayload(), // ou texto da descrição se disponível
                attributes,
                listing.getThumbnailUrl() != null ? 1 : 0
        );

        try {
            ListingQualityAudit audit = new ListingQualityAudit(
                    tenantId,
                    listingId,
                    result.overallScore(),
                    result.titleScore(),
                    result.descriptionScore(),
                    result.attributesScore(),
                    result.imagesScore(),
                    objectMapper.writeValueAsString(result.criteriaBreakdown()),
                    objectMapper.writeValueAsString(result.actionableRecommendations()),
                    objectMapper.writeValueAsString(result.missingRequiredAttributes())
            );
            auditRepository.save(audit);
        } catch (Exception e) {
            // Log e segue
        }

        return result;
    }

    @Transactional(readOnly = true)
    public QualityAuditResultDto getLatestAudit(UUID listingId) {
        UUID tenantId = TenantContext.getTenantId();
        findListingForTenant(tenantId, listingId); // valida posse

        Optional<ListingQualityAudit> latest = auditRepository.findLatestByTenantIdAndListingId(tenantId, listingId);
        if (latest.isPresent()) {
            ListingQualityAudit a = latest.get();
            try {
                List<CriterionEvaluationDto> breakdown = objectMapper.readValue(a.getCriteriaBreakdownJson(), new TypeReference<>() {});
                List<String> recs = objectMapper.readValue(a.getActionableRecommendationsJson(), new TypeReference<>() {});
                List<String> missing = objectMapper.readValue(a.getMissingRequiredAttributesJson(), new TypeReference<>() {});
                return new QualityAuditResultDto(
                        a.getListingId(),
                        a.getOverallScore(),
                        a.getTitleScore(),
                        a.getDescriptionScore(),
                        a.getAttributesScore(),
                        a.getImagesScore(),
                        breakdown,
                        recs,
                        missing,
                        a.getCreatedAt()
                );
            } catch (Exception ex) {
                // Fallback para novo cálculo
            }
        }
        return auditListing(listingId);
    }

    public ListingGenerationResponse generateContent(UUID listingId, ListingGenerationRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getCurrentUserId().map(UUID::toString).orElse("system-user");
        Listing listing = findListingForTenant(tenantId, listingId);
        Product product = listing.getProductId() != null ? productRepository.findById(listing.getProductId()).orElse(null) : null;

        String promptType = request.promptType() != null ? request.promptType().toUpperCase() : "TITLE";
        String promptVersion = "TITLE".equalsIgnoreCase(promptType) ? PROMPT_TITLE_VERSION : PROMPT_DESC_VERSION;
        String promptFile = "TITLE".equalsIgnoreCase(promptType) ? "classpath:prompts/listing-title-v1.st" : "classpath:prompts/listing-description-v1.st";

        String systemPrompt = loadPromptResource(promptFile);

        // Prepara atributos confirmados
        Map<String, String> confirmedAttributes = new LinkedHashMap<>();
        if (product != null) {
            if (product.getBrand() != null) confirmedAttributes.put("marca", product.getBrand());
            if (product.getCategory() != null) confirmedAttributes.put("categoria", product.getCategory());
            if (product.getSku() != null) confirmedAttributes.put("sku", product.getSku());
        }
        if (request.additionalConfirmedAttributes() != null) {
            confirmedAttributes.putAll(request.additionalConfirmedAttributes());
        }

        // Sanitização contra Prompt Injection
        String untrustedXml = promptSanitizer.buildUntrustedContextXml(
                listing.getTitle(),
                listing.getRawPayload(),
                product != null ? product.getBrand() : "",
                product != null ? product.getCategory() : "",
                confirmedAttributes
        );

        long startTime = System.currentTimeMillis();
        try {
            ListingAiOutput output = aiProvider.generateListingContent(promptType, systemPrompt, untrustedXml, null);
            long latency = System.currentTimeMillis() - startTime;

            // Rastreabilidade do modelo/provedor sem segredos
            AiGenerationLog log = new AiGenerationLog(
                    tenantId,
                    userId,
                    listingId,
                    promptType,
                    promptVersion,
                    output.modelUsed(),
                    latency,
                    "SUCCESS",
                    null
            );
            logRepository.save(log);

            String suggestedTitle = "TITLE".equalsIgnoreCase(promptType) ? output.textResult() : null;
            String suggestedDesc = "DESCRIPTION".equalsIgnoreCase(promptType) ? output.textResult() : null;

            return new ListingGenerationResponse(
                    listingId,
                    promptType,
                    promptVersion,
                    output.modelUsed(),
                    suggestedTitle,
                    suggestedDesc,
                    output.confirmedAttributesUsed(),
                    output.missingAttributesIdentified(),
                    output.rationale(),
                    "AI_GENERATED",
                    DISCLAIMER_TEXT
            );
        } catch (AiServiceUnavailableException ex) {
            long latency = System.currentTimeMillis() - startTime;
            AiGenerationLog errorLog = new AiGenerationLog(
                    tenantId,
                    userId,
                    listingId,
                    promptType,
                    promptVersion,
                    aiProvider.getProviderName(),
                    latency,
                    "ERROR",
                    "AI_SERVICE_UNAVAILABLE"
            );
            logRepository.save(errorLog);
            throw ex;
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - startTime;
            AiGenerationLog errorLog = new AiGenerationLog(
                    tenantId,
                    userId,
                    listingId,
                    promptType,
                    promptVersion,
                    aiProvider.getProviderName(),
                    latency,
                    "ERROR",
                    "INTERNAL_ERROR"
            );
            logRepository.save(errorLog);
            throw new AiServiceUnavailableException("Falha no provedor de IA ao gerar conteúdo: " + ex.getMessage(), ex);
        }
    }

    public ListingVersionDto saveVersion(UUID listingId, SaveListingVersionRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getCurrentUserId().map(UUID::toString).orElse("user");
        Listing listing = findListingForTenant(tenantId, listingId);

        Integer nextVersion = versionRepository.findMaxVersionNumber(tenantId, listingId) + 1;

        ListingVersion version = new ListingVersion(
                tenantId,
                listingId,
                listing.getProductId(),
                nextVersion,
                request.title() != null ? request.title().trim() : listing.getTitle(),
                request.description(),
                request.attributesJson(),
                request.status() != null ? request.status() : "DRAFT",
                "MANUAL_EDIT",
                PROMPT_TITLE_VERSION,
                "user-edit",
                userId
        );

        ListingVersion saved = versionRepository.save(version);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ListingVersionDto> listVersions(UUID listingId) {
        UUID tenantId = TenantContext.getTenantId();
        findListingForTenant(tenantId, listingId);
        return versionRepository.findByTenantIdAndListingIdOrderByVersionDesc(tenantId, listingId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ListingVersionDto acceptVersion(UUID listingId, UUID versionId) {
        UUID tenantId = TenantContext.getTenantId();
        findListingForTenant(tenantId, listingId);

        ListingVersion target = versionRepository.findByTenantIdAndId(tenantId, versionId)
                .orElseThrow(() -> new BusinessException("VERSION_NOT_FOUND", "Versão não encontrada: " + versionId, HttpStatus.NOT_FOUND));

        target.setStatus("ACCEPTED");
        ListingVersion saved = versionRepository.save(target);
        return toDto(saved);
    }

    private Listing findListingForTenant(UUID tenantId, UUID listingId) {
        return listingRepository.findByTenantIdAndId(tenantId, listingId)
                .orElseThrow(() -> new BusinessException("LISTING_NOT_FOUND", "Anúncio não encontrado para o tenant ativo: " + listingId, HttpStatus.NOT_FOUND));
    }

    private String loadPromptResource(String location) {
        try {
            Resource res = resourceLoader.getResource(location);
            try (InputStream in = res.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            return "Você é um especialista em e-commerce. Use estritamente atributos confirmados.";
        }
    }

    private Map<String, String> extractAttributesFromRawPayload(String rawPayload) {
        Map<String, String> map = new HashMap<>();
        if (rawPayload == null || rawPayload.isBlank()) return map;
        try {
            return objectMapper.readValue(rawPayload, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return map;
        }
    }

    private ListingVersionDto toDto(ListingVersion v) {
        return new ListingVersionDto(
                v.getId(),
                v.getListingId(),
                v.getVersionNumber(),
                v.getTitle(),
                v.getDescription(),
                v.getAttributesJson(),
                v.getStatus(),
                v.getSource(),
                v.getPromptVersion(),
                v.getModelName(),
                v.getCreatedBy(),
                v.getCreatedAt()
        );
    }
}
