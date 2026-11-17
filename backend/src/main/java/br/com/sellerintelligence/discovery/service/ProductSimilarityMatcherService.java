package br.com.sellerintelligence.discovery.service;

import br.com.sellerintelligence.discovery.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ProductSimilarityMatcherService {

    private static final Logger log = LoggerFactory.getLogger(ProductSimilarityMatcherService.class);

    private final DiscoveryProductMatchRepository matchRepository;
    private final DiscoveryCatalogItemRepository itemRepository;

    public ProductSimilarityMatcherService(DiscoveryProductMatchRepository matchRepository,
                                           DiscoveryCatalogItemRepository itemRepository) {
        this.matchRepository = matchRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public List<DiscoveryProductMatch> findAndGenerateMatches(UUID tenantId, UUID catalogItemId,
                                                             List<CandidateProductProposal> externalCandidates) {
        DiscoveryCatalogItem item = itemRepository.findByIdAndTenantId(catalogItemId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Item de catálogo não encontrado: " + catalogItemId));

        List<DiscoveryProductMatch> matches = new ArrayList<>();

        for (CandidateProductProposal candidate : externalCandidates) {
            BigDecimal similarity;
            MatchSource source;

            // 1. Verificação EAN exato
            if (item.getEan() != null && item.getEan().equalsIgnoreCase(candidate.ean())) {
                similarity = new BigDecimal("0.9800");
                source = MatchSource.EAN_EXACT;
            } else {
                // 2. Similaridade de texto / vetorial
                double score = computeTextSimilarity(item.getName(), candidate.title());
                similarity = BigDecimal.valueOf(Math.min(0.9500, Math.max(0.1000, score))).setScale(4, RoundingMode.HALF_EVEN);
                source = MatchSource.SEMANTIC_VECTOR;
            }

            // 3. Estimativa de Margem Bruta
            BigDecimal estimatedMargin = null;
            if (item.getCostPrice() != null && candidate.price() != null && candidate.price().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal revenue = candidate.price();
                BigDecimal cost = item.getCostPrice();
                estimatedMargin = revenue.subtract(cost).divide(revenue, 4, RoundingMode.HALF_EVEN);
            }

            DiscoveryProductMatch match = new DiscoveryProductMatch(
                    item.getId(),
                    tenantId,
                    candidate.marketplace(),
                    candidate.externalId(),
                    candidate.title(),
                    candidate.price(),
                    candidate.url(),
                    similarity,
                    source,
                    estimatedMargin
            );
            matches.add(match);
        }

        List<DiscoveryProductMatch> saved = matchRepository.saveAll(matches);
        log.info("Generated {} match candidates for catalogItemId={}", saved.size(), catalogItemId);
        return saved;
    }

    @Transactional
    public DiscoveryProductMatch confirmMatch(UUID tenantId, UUID matchId) {
        DiscoveryProductMatch match = getMatch(tenantId, matchId);
        match.confirm();
        log.info("Match confirmed by seller: matchId={}, tenantId={}", matchId, tenantId);
        return matchRepository.save(match);
    }

    @Transactional
    public DiscoveryProductMatch rejectMatch(UUID tenantId, UUID matchId) {
        DiscoveryProductMatch match = getMatch(tenantId, matchId);
        match.reject();
        log.info("Match rejected by seller: matchId={}, tenantId={}", matchId, tenantId);
        return matchRepository.save(match);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryProductMatch> getMatchesForItem(UUID tenantId, UUID catalogItemId) {
        return matchRepository.findByCatalogItemIdAndTenantId(catalogItemId, tenantId);
    }

    @Transactional(readOnly = true)
    public DiscoveryProductMatch getMatch(UUID tenantId, UUID matchId) {
        return matchRepository.findByIdAndTenantId(matchId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Candidato de similaridade não encontrado: " + matchId));
    }

    private double computeTextSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        Set<String> tokens1 = new HashSet<>(Arrays.asList(s1.toLowerCase().split("\\s+")));
        Set<String> tokens2 = new HashSet<>(Arrays.asList(s2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    public record CandidateProductProposal(
            String marketplace,
            String externalId,
            String title,
            BigDecimal price,
            String ean,
            String url
    ) {}
}
