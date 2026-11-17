package br.com.sellerintelligence.market.service;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.market.domain.SavedMarketProduct;
import br.com.sellerintelligence.market.domain.SavedSearch;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SavedSearchesAndProductsIntegrationTest {

    @Autowired
    private MarketIntelligenceService marketService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private UUID tenantAId;
    private UUID tenantBId;

    @BeforeEach
    void setUp() {
        Organization orgA = organizationRepository.save(new Organization("Tenant A Org", "tenant-a-market"));
        tenantAId = orgA.getId();

        Organization orgB = organizationRepository.save(new Organization("Tenant B Org", "tenant-b-market"));
        tenantBId = orgB.getId();
    }

    @Test
    @DisplayName("Tenant A can save and list searches with full multi-tenant isolation from Tenant B")
    void shouldSaveAndIsolateSearchesBetweenTenants() {
        SavedSearch searchA = marketService.saveSearch(
                tenantAId,
                "Monitor Gamer 144Hz",
                Marketplace.MERCADO_LIVRE,
                "monitor gamer",
                "MLB1055",
                new BigDecimal("500.00"),
                new BigDecimal("1500.00"),
                "new",
                "LG",
                "Verificar margem para revenda"
        );
        assertThat(searchA.getId()).isNotNull();

        List<SavedSearch> listA = marketService.listSavedSearches(tenantAId);
        assertThat(listA).hasSize(1);
        assertThat(listA.get(0).getName()).isEqualTo("Monitor Gamer 144Hz");

        List<SavedSearch> listB = marketService.listSavedSearches(tenantBId);
        assertThat(listB).isEmpty();

        // Tenant B cannot delete Tenant A's search
        assertThatThrownBy(() -> marketService.deleteSavedSearch(tenantBId, searchA.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Saved search not found or access denied");
    }

    @Test
    @DisplayName("Tenant A can save market products with notes and update notes, isolated from Tenant B")
    void shouldSaveAndManageMarketProductsWithIsolation() {
        SavedMarketProduct productA = marketService.saveProduct(
                tenantAId,
                Marketplace.MERCADO_LIVRE,
                "MLB999888",
                "Cadeira Gamer Ergonômica",
                new BigDecimal("699.90"),
                "BRL",
                "moveis_pro",
                "https://produto.mercadolivre.com.br/MLB999888",
                "https://http2.mlstatic.com/cadeira.jpg",
                "Concorrente com frete grátis agressivo"
        );

        assertThat(productA.getId()).isNotNull();
        assertThat(productA.getProvenance()).isEqualTo(DataProvenance.OBSERVED);

        // Update notes
        SavedMarketProduct updated = marketService.updateProductNotes(tenantAId, productA.getId(), "Nota atualizada: concorrente reduziu preço");
        assertThat(updated.getNotes()).isEqualTo("Nota atualizada: concorrente reduziu preço");

        // Isolation: Tenant B cannot view or update
        List<SavedMarketProduct> listB = marketService.listSavedProducts(tenantBId);
        assertThat(listB).isEmpty();

        assertThatThrownBy(() -> marketService.updateProductNotes(tenantBId, productA.getId(), "Tentativa indevida"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Saved product not found or access denied");

        assertThatThrownBy(() -> marketService.deleteSavedProduct(tenantBId, productA.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Saved product not found or access denied");
    }
}
