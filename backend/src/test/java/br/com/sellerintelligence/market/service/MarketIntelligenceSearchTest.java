package br.com.sellerintelligence.market.service;

import br.com.sellerintelligence.market.domain.MarketSearchSnapshot;
import br.com.sellerintelligence.market.domain.MarketSearchSnapshotRepository;
import br.com.sellerintelligence.market.provider.MarketProductDto;
import br.com.sellerintelligence.market.provider.MarketSearchQuery;
import br.com.sellerintelligence.market.provider.MarketSearchResult;
import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketIntelligenceSearchTest {

    @Autowired
    private MarketIntelligenceService marketService;

    @Autowired
    private MarketSearchSnapshotRepository snapshotRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        Organization org = new Organization("Market Test Org", "market-test-org");
        org = organizationRepository.save(org);
        tenantId = org.getId();
    }

    @Test
    @DisplayName("Should execute keyword search on Mercado Livre and return observed results with snapshots")
    void shouldExecuteKeywordSearchWithObservedResults() {
        MarketSearchQuery query = new MarketSearchQuery(
                "teclado mecanico",
                "MLB1055",
                new BigDecimal("50.00"),
                new BigDecimal("500.00"),
                "new",
                null,
                "price_asc",
                0,
                10
        );

        MarketSearchResult result = marketService.executeSearch(tenantId, Marketplace.MERCADO_LIVRE, query, true);

        assertThat(result.query()).isEqualTo("teclado mecanico");
        assertThat(result.provenance()).isEqualTo(DataProvenance.OBSERVED);
        assertThat(result.items()).isNotEmpty();

        MarketProductDto firstItem = result.items().get(0);
        assertThat(firstItem.provenance()).isEqualTo(DataProvenance.OBSERVED);
        assertThat(firstItem.price()).isNotNull();
        assertThat(firstItem.estimatedSalesRange()).isNotNull();
        assertThat(firstItem.estimationMethod()).contains("Estimativa");

        // Verify snapshot was created in database
        List<MarketSearchSnapshot> snapshots = snapshotRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        assertThat(snapshots).hasSize(1);
        MarketSearchSnapshot snapshot = snapshots.get(0);
        assertThat(snapshot.getQuery()).isEqualTo("teclado mecanico");
        assertThat(snapshot.getItems()).hasSize(result.items().size());
    }

    @Test
    @DisplayName("Empty search query returns empty result without throwing error")
    void shouldReturnEmptyResultForBlankQuery() {
        MarketSearchQuery query = new MarketSearchQuery(
                "   ",
                null, null, null, null, null, null, 0, 20
        );

        MarketSearchResult result = marketService.executeSearch(tenantId, Marketplace.MERCADO_LIVRE, query, false);

        assertThat(result.total()).isEqualTo(0);
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("Searching Shopee returns DataProvenance.UNAVAILABLE without illegal scraping")
    void shouldReturnUnavailableForShopeePublicSearch() {
        MarketSearchQuery query = new MarketSearchQuery(
                "fone bluetooth",
                null, null, null, null, null, null, 0, 20
        );

        MarketSearchResult result = marketService.executeSearch(tenantId, Marketplace.SHOPEE, query, false);

        assertThat(result.total()).isEqualTo(0);
        assertThat(result.items()).isEmpty();
        assertThat(result.provenance()).isEqualTo(DataProvenance.UNAVAILABLE);
    }
}
