package br.com.sellerintelligence.discovery.service;

import br.com.sellerintelligence.discovery.domain.DiscoveryVisualSearch;
import br.com.sellerintelligence.discovery.domain.DiscoveryVisualSearchRepository;
import br.com.sellerintelligence.discovery.security.SecureFileUploadValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class VisualSearchService {

    private static final Logger log = LoggerFactory.getLogger(VisualSearchService.class);

    private final DiscoveryVisualSearchRepository visualSearchRepository;
    private final SecureFileUploadValidator fileUploadValidator;

    public VisualSearchService(DiscoveryVisualSearchRepository visualSearchRepository,
                               SecureFileUploadValidator fileUploadValidator) {
        this.visualSearchRepository = visualSearchRepository;
        this.fileUploadValidator = fileUploadValidator;
    }

    @Transactional
    public VisualSearchResult performVisualSearch(UUID tenantId, byte[] imageBytes, String originalFilename) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(imageBytes, "imageBytes cannot be null");

        fileUploadValidator.validateSize(imageBytes.length);
        byte[] header = Arrays.copyOf(imageBytes, Math.min(imageBytes.length, 16));
        String mimeType = fileUploadValidator.detectAndValidateMimeType(header);

        if (!mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("Busca visual suporta exclusivamente arquivos de imagem (PNG ou JPEG).");
        }

        String storedPath = "visual-searches/" + tenantId + "/" + UUID.randomUUID() + (mimeType.equals("image/png") ? ".png" : ".jpg");

        // Simula busca vetorial de produtos visualmente semelhantes
        List<VisualCandidate> candidates = List.of(
                new VisualCandidate("MLB28190", "MERCADO_LIVRE", "Produto Similar Identificado por Imagem A", new BigDecimal("149.90"), new BigDecimal("0.9100"), "https://mercadolivre.com.br/item/MLB28190"),
                new VisualCandidate("SP482910", "SHOPEE", "Produto Similar Identificado por Imagem B", new BigDecimal("135.00"), new BigDecimal("0.8450"), "https://shopee.com.br/product/SP482910")
        );

        DiscoveryVisualSearch search = new DiscoveryVisualSearch(
                tenantId,
                storedPath,
                mimeType,
                "Busca por imagem: " + fileUploadValidator.sanitizeFilename(originalFilename),
                candidates.size()
        );
        visualSearchRepository.save(search);
        log.info("Recorded visual search: id={}, tenantId={}, candidates={}", search.getId(), tenantId, candidates.size());

        return new VisualSearchResult(search.getId(), mimeType, candidates);
    }

    public record VisualCandidate(
            String externalId,
            String marketplace,
            String title,
            BigDecimal price,
            BigDecimal visualSimilarityScore,
            String url
    ) {}

    public record VisualSearchResult(
            UUID searchId,
            String mimeType,
            List<VisualCandidate> candidates
    ) {}
}
