package com.sellerintelligence.modules.marketintelligence.service;

import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.catalog.Product;
import com.sellerintelligence.modules.catalog.ProductRepository;
import com.sellerintelligence.modules.catalog.ProductVariant;
import com.sellerintelligence.modules.catalog.ProductVariantRepository;
import com.sellerintelligence.modules.marketintelligence.MarketObservation;
import com.sellerintelligence.modules.marketintelligence.MarketObservationRepository;
import com.sellerintelligence.modules.marketintelligence.ProductMatch;
import com.sellerintelligence.modules.marketintelligence.ProductMatchRepository;
import com.sellerintelligence.modules.marketintelligence.model.MatchScoreBreakdown;
import com.sellerintelligence.modules.marketintelligence.model.MatchStatus;
import com.sellerintelligence.modules.marketintelligence.model.ProductMatchCandidateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Motor de correspondência determinístico e explicável entre observações de mercado e produtos canônicos.
 * Implementa normalização avançada de termos, comparação de GTIN/EAN, similaridade de Jaccard e matching explicável.
 * Cumpre a regra estrita de aceite: "matching não une automaticamente itens abaixo do limiar".
 */
@Service
public class ProductMatchingService {

    private static final Logger log = LoggerFactory.getLogger(ProductMatchingService.class);
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("0.7500");
    private static final BigDecimal POTENTIAL_THRESHOLD = new BigDecimal("0.5000");

    private static final Set<String> STOPWORDS = Set.of(
            "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas",
            "com", "para", "por", "e", "ou", "a", "o", "os", "as", "um",
            "uma", "uns", "umas", "sem", "sob", "sobre", "kit", "combo"
    );

    private final MarketObservationRepository observationRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductMatchRepository productMatchRepository;

    public ProductMatchingService(
            MarketObservationRepository observationRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductMatchRepository productMatchRepository
    ) {
        this.observationRepository = observationRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productMatchRepository = productMatchRepository;
    }

    @Transactional
    public List<ProductMatchCandidateResponse> matchObservation(
            UUID tenantId,
            UUID observationId,
            BigDecimal requestedThreshold
    ) {
        BigDecimal threshold = (requestedThreshold != null) ? requestedThreshold : DEFAULT_THRESHOLD;

        MarketObservation observation = observationRepository.findByIdAndTenantId(observationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Observação de mercado não encontrada para o id: " + observationId));

        List<Product> tenantProducts = productRepository.findAllByTenantId(tenantId);
        List<ProductMatchCandidateResponse> candidates = new ArrayList<>();

        for (Product product : tenantProducts) {
            List<ProductVariant> variants = productVariantRepository.findAllByTenantIdAndProductId(tenantId, product.getId());
            MatchScoreBreakdown breakdown = calculateScore(observation, product, variants, threshold);

            // Persiste ou atualiza o resultado de correspondência
            saveOrUpdateMatch(tenantId, product.getId(), observation.getId(), breakdown, threshold);

            candidates.add(new ProductMatchCandidateResponse(
                    product.getId(),
                    product.getTitle(),
                    product.getCanonicalSku(),
                    product.getBrand(),
                    breakdown
            ));
        }

        // Ordena por score geral decrescente
        candidates.sort(Comparator.comparing(
                (ProductMatchCandidateResponse c) -> c.scoreBreakdown().overallScore()
        ).reversed());

        return candidates;
    }

    public MatchScoreBreakdown calculateScore(
            MarketObservation observation,
            Product product,
            List<ProductVariant> variants,
            BigDecimal threshold
    ) {
        // 1. Similaridade de Título (Jaccard sobre tokens normalizados)
        Set<String> obsTokens = extractTokens(observation.getTitle());
        Set<String> prodTokens = extractTokens(product.getTitle());
        double titleSim = calculateJaccard(obsTokens, prodTokens);
        BigDecimal titleScore = BigDecimal.valueOf(titleSim).setScale(4, RoundingMode.HALF_UP);

        // 2. GTIN / EAN Match
        String obsGtin = (observation.getGtin() != null && !observation.getGtin().isBlank())
                ? observation.getGtin().trim()
                : null;

        Set<String> productGtins = variants.stream()
                .map(ProductVariant::getGtinBarcode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(g -> !g.isBlank())
                .collect(Collectors.toSet());

        BigDecimal gtinScore = null;
        boolean gtinMatch = false;
        boolean gtinConflict = false;

        if (obsGtin != null && !productGtins.isEmpty()) {
            if (productGtins.contains(obsGtin)) {
                gtinScore = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
                gtinMatch = true;
            } else {
                gtinScore = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
                gtinConflict = true;
            }
        }

        // 3. Marca Match
        BigDecimal brandScore = null;
        String obsBrand = normalizeSimple(observation.getBrand());
        String prodBrand = normalizeSimple(product.getBrand());

        if (!obsBrand.isEmpty() && !prodBrand.isEmpty()) {
            if (obsBrand.equals(prodBrand)) {
                brandScore = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
            } else {
                brandScore = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            }
        }

        // 4. Cálculo Ponderado do Overall Score
        double overall;
        StringBuilder explanation = new StringBuilder();

        if (gtinMatch) {
            // Se GTIN coincide perfeitamente: peso muito alto no GTIN (50%), título (35%) e marca (15%)
            double brandWeight = (brandScore != null) ? 0.15 : 0.0;
            double titleWeight = (brandScore != null) ? 0.35 : 0.50;
            double gtinWeight = 0.50;

            double brandVal = (brandScore != null) ? brandScore.doubleValue() : 0.0;
            overall = (1.0 * gtinWeight) + (titleSim * titleWeight) + (brandVal * brandWeight);

            explanation.append("GTIN idêntico (").append(obsGtin).append(") (+50%), ");
            explanation.append("similaridade de título de ").append(Math.round(titleSim * 100)).append("% (+")
                    .append(Math.round(titleSim * titleWeight * 100)).append("%), ");
            if (brandScore != null) {
                explanation.append("marca '").append(product.getBrand()).append("' ")
                        .append(brandScore.compareTo(BigDecimal.ONE) == 0 ? "coincidente (+15%)" : "divergente (0%)").append(". ");
            }
        } else if (gtinConflict) {
            // Conflito de GTIN (ambos têm GTINs diferentes): penalização severa
            overall = titleSim * 0.40;
            explanation.append("Conflito de GTIN (observado: ").append(obsGtin)
                    .append(" vs catálogo: ").append(productGtins).append(") (penalizado para 0%), ");
            explanation.append("similaridade de título de ").append(Math.round(titleSim * 100)).append("%. ");
        } else {
            // Sem GTIN disponível em um ou ambos: avaliação puramente textual e de marca
            if (brandScore != null) {
                double brandVal = brandScore.doubleValue();
                overall = (titleSim * 0.70) + (brandVal * 0.30);
                explanation.append("GTIN não informado. Similaridade de título de ").append(Math.round(titleSim * 100))
                        .append("% (+").append(Math.round(titleSim * 70)).append("%), ");
                explanation.append("marca '").append(product.getBrand()).append("' ")
                        .append(brandScore.compareTo(BigDecimal.ONE) == 0 ? "coincidente (+30%)" : "divergente (0%)").append(". ");
            } else {
                overall = titleSim;
                explanation.append("GTIN e marca não informados em ambos. Similaridade textual de ")
                        .append(Math.round(titleSim * 100)).append("% (+").append(Math.round(titleSim * 100)).append("%). ");
            }
        }

        // Garante que o score fique no intervalo [0.0000, 1.0000]
        overall = Math.max(0.0, Math.min(1.0, overall));
        BigDecimal finalOverallScore = BigDecimal.valueOf(overall).setScale(4, RoundingMode.HALF_UP);

        // 5. Determinação do Status de Matching conforme Threshold
        MatchStatus status;
        if (finalOverallScore.compareTo(threshold) >= 0) {
            status = MatchStatus.MATCHED;
            explanation.append("Score global ").append(finalOverallScore)
                    .append(" atingiu o limiar de ").append(threshold)
                    .append(": item classificado como MATCHED.");
        } else if (finalOverallScore.compareTo(POTENTIAL_THRESHOLD) >= 0) {
            status = MatchStatus.POTENTIAL_MATCH;
            explanation.append("Score global ").append(finalOverallScore)
                    .append(" abaixo do limiar de ").append(threshold)
                    .append(" mas com similaridade relevante: classificado como POTENTIAL_MATCH (NÃO unido automaticamente, pendente de revisão manual).");
        } else {
            status = MatchStatus.NO_MATCH;
            explanation.append("Score global ").append(finalOverallScore)
                    .append(" insuficiente: classificado como NO_MATCH (NÃO unido automaticamente).");
        }

        return new MatchScoreBreakdown(
                finalOverallScore,
                gtinScore,
                titleScore,
                brandScore,
                null,
                status,
                explanation.toString(),
                threshold
        );
    }

    private void saveOrUpdateMatch(
            UUID tenantId,
            UUID productId,
            UUID observationId,
            MatchScoreBreakdown breakdown,
            BigDecimal threshold
    ) {
        Optional<ProductMatch> existing = productMatchRepository
                .findByTenantIdAndCanonicalProductIdAndObservationId(tenantId, productId, observationId);

        if (existing.isPresent()) {
            ProductMatch match = existing.get();
            match.updateMatch(
                    breakdown.overallScore(),
                    breakdown.gtinScore(),
                    breakdown.titleScore(),
                    breakdown.brandScore(),
                    breakdown.priceScore(),
                    breakdown.status(),
                    breakdown.explanation(),
                    threshold,
                    Instant.now()
            );
            productMatchRepository.save(match);
        } else {
            ProductMatch match = new ProductMatch(
                    UUID.randomUUID(),
                    tenantId,
                    productId,
                    observationId,
                    breakdown.overallScore(),
                    breakdown.gtinScore(),
                    breakdown.titleScore(),
                    breakdown.brandScore(),
                    breakdown.priceScore(),
                    breakdown.status(),
                    breakdown.explanation(),
                    threshold,
                    Instant.now()
            );
            productMatchRepository.save(match);
        }
    }

    private double calculateJaccard(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() && set2.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    public Set<String> extractTokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ");

        return Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() > 1)
                .filter(token -> !STOPWORDS.contains(token))
                .collect(Collectors.toSet());
    }

    private String normalizeSimple(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }
}
