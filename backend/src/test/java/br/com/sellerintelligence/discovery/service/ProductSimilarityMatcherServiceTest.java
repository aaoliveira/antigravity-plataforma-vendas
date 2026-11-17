package br.com.sellerintelligence.discovery.service;

import br.com.sellerintelligence.discovery.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSimilarityMatcherServiceTest {

    @Mock
    private DiscoveryProductMatchRepository matchRepository;

    @Mock
    private DiscoveryCatalogItemRepository itemRepository;

    private ProductSimilarityMatcherService matcherService;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID catalogId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        matcherService = new ProductSimilarityMatcherService(matchRepository, itemRepository);
    }

    @Test
    @DisplayName("Should generate candidate matches with EAN exact match and calculate estimated margin")
    void shouldGenerateMatchesAndCalculateMargin() {
        DiscoveryCatalogItem item = new DiscoveryCatalogItem(
                catalogId, tenantId, "Teclado Mecanico Gamer RGB", "KB-10", "7891234567895",
                "Desc", new BigDecimal("100.00"), null, null,
                ExtractionMethod.PDF_TEXT, new BigDecimal("0.9500"), "{}"
        );
        when(itemRepository.findByIdAndTenantId(item.getId(), tenantId)).thenReturn(Optional.of(item));
        when(matchRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ProductSimilarityMatcherService.CandidateProductProposal> candidates = List.of(
                new ProductSimilarityMatcherService.CandidateProductProposal(
                        "MERCADO_LIVRE", "MLB9988", "Teclado Mecanico RGB",
                        new BigDecimal("200.00"), "7891234567895", "https://ml.com/MLB9988"
                ),
                new ProductSimilarityMatcherService.CandidateProductProposal(
                        "SHOPEE", "SP1122", "Teclado Gamer Outro",
                        new BigDecimal("180.00"), null, "https://shopee.com/SP1122"
                )
        );

        List<DiscoveryProductMatch> matches = matcherService.findAndGenerateMatches(tenantId, item.getId(), candidates);

        assertThat(matches).hasSize(2);

        // 1. Exact EAN Match
        DiscoveryProductMatch exact = matches.get(0);
        assertThat(exact.getMatchSource()).isEqualTo(MatchSource.EAN_EXACT);
        assertThat(exact.getSimilarityScore()).isEqualByComparingTo("0.9800");
        assertThat(exact.getStatus()).isEqualTo(MatchStatus.CANDIDATE);
        // Margin: (200 - 100) / 200 = 0.5000 (50%)
        assertThat(exact.getEstimatedGrossMargin()).isEqualByComparingTo("0.5000");

        // 2. Semantic text similarity
        DiscoveryProductMatch semantic = matches.get(1);
        assertThat(semantic.getMatchSource()).isEqualTo(MatchSource.SEMANTIC_VECTOR);
        assertThat(semantic.getStatus()).isEqualTo(MatchStatus.CANDIDATE);
        assertThat(semantic.getSimilarityScore()).isGreaterThan(new BigDecimal("0.1000"));
    }

    @Test
    @DisplayName("Should confirm candidate match on explicit seller confirmation")
    void shouldConfirmCandidateMatch() {
        DiscoveryProductMatch match = new DiscoveryProductMatch(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "MLB123",
                "Produto Correspondente", new BigDecimal("150.00"), "https://ml.com",
                new BigDecimal("0.9000"), MatchSource.SEMANTIC_VECTOR, new BigDecimal("0.3300")
        );

        when(matchRepository.findByIdAndTenantId(match.getId(), tenantId)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(DiscoveryProductMatch.class))).thenAnswer(inv -> inv.getArgument(0));

        DiscoveryProductMatch confirmed = matcherService.confirmMatch(tenantId, match.getId());

        assertThat(confirmed.getStatus()).isEqualTo(MatchStatus.CONFIRMED);
        verify(matchRepository, times(1)).save(match);
    }
}
