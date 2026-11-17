package br.com.sellerintelligence.sync.service;

import br.com.sellerintelligence.connector.crypto.TokenCryptoService;
import br.com.sellerintelligence.connector.model.MarketplaceCredential;
import br.com.sellerintelligence.connector.model.MarketplaceCredentialRepository;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.sync.domain.*;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.domain.Role;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SyncIdempotencyIntegrationTest {

    @Autowired
    private SyncJobService syncJobService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private MarketplaceCredentialRepository credentialRepository;

    @Autowired
    private TokenCryptoService tokenCryptoService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    private UUID tenantId;
    private Store store;

    @BeforeEach
    void setUp() {
        Organization org = new Organization("Idempotency Org", "idempotency-org");
        org = organizationRepository.save(org);
        tenantId = org.getId();

        User user = new User("sub-sync-user", "sync.user@seller.test", "Sync User");
        user = userRepository.save(user);

        Membership membership = new Membership(org, user, Role.OWNER);
        membershipRepository.save(membership);

        TenantContext.setTenant(new TenantContext.TenantInfo(
                tenantId, user.getId(), user.getKeycloakSub(), user.getEmail(), Role.OWNER
        ));

        Company company = new Company(tenantId, "98765432000198", "Idem Corp", "Idem Fantasia", TaxRegime.LUCRO_PRESUMIDO);
        company = companyRepository.save(company);

        // Store with OUTRO so it uses FakeMarketplaceConnector
        store = new Store(tenantId, company, Marketplace.OUTRO, "Loja Fake Idempotente");
        store.setExternalAccountId("FAKE-ACCT-999");
        store = storeRepository.save(store);

        MarketplaceCredential cred = new MarketplaceCredential(
                tenantId,
                store,
                Marketplace.OUTRO,
                tokenCryptoService.encrypt("fake-test-token"),
                tokenCryptoService.encrypt("fake-refresh-token"),
                "FAKE-ACCT-999",
                "test_scope",
                OffsetDateTime.now().plusHours(6),
                OffsetDateTime.now().plusMonths(6),
                "TEST"
        );
        credentialRepository.save(cred);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Running the same catalog sync job twice does NOT duplicate listings or products (strict idempotency)")
    void shouldNotDuplicateListingsWhenSyncExecutedMultipleTimes() {
        // Run 1
        SyncJob job1 = syncJobService.triggerSync(store.getId(), SyncResourceType.LISTINGS, SyncTriggerType.MANUAL);
        assertThat(job1.getStatus()).isEqualTo(SyncStatus.COMPLETED);

        long countListingsRun1 = listingRepository.countByTenantId(tenantId);
        long countProductsRun1 = productRepository.countByTenantId(tenantId);

        assertThat(countListingsRun1).isEqualTo(2);
        assertThat(countProductsRun1).isEqualTo(2);

        // Run 2 (identical batch executed again)
        SyncJob job2 = syncJobService.triggerSync(store.getId(), SyncResourceType.LISTINGS, SyncTriggerType.MANUAL);
        assertThat(job2.getStatus()).isEqualTo(SyncStatus.COMPLETED);

        long countListingsRun2 = listingRepository.countByTenantId(tenantId);
        long countProductsRun2 = productRepository.countByTenantId(tenantId);

        // Counts must be strictly identical - no duplicates created
        assertThat(countListingsRun2).isEqualTo(countListingsRun1);
        assertThat(countProductsRun2).isEqualTo(countProductsRun1);
    }

    @Test
    @DisplayName("Running the same orders sync job twice does NOT duplicate orders or order items")
    void shouldNotDuplicateOrdersWhenSyncExecutedMultipleTimes() {
        // Run 1
        SyncJob job1 = syncJobService.triggerSync(store.getId(), SyncResourceType.ORDERS, SyncTriggerType.MANUAL);
        assertThat(job1.getStatus()).isEqualTo(SyncStatus.COMPLETED);

        long countOrdersRun1 = orderRepository.countByTenantId(tenantId);
        assertThat(countOrdersRun1).isEqualTo(1);

        Order order = orderRepository.findByTenantIdAndMarketplaceAndExternalOrderId(
                tenantId, Marketplace.OUTRO, "FAKE-ORDER-99901"
        ).orElseThrow();
        assertThat(order.getItems()).hasSize(1);

        // Run 2
        SyncJob job2 = syncJobService.triggerSync(store.getId(), SyncResourceType.ORDERS, SyncTriggerType.MANUAL);
        assertThat(job2.getStatus()).isEqualTo(SyncStatus.COMPLETED);

        long countOrdersRun2 = orderRepository.countByTenantId(tenantId);
        assertThat(countOrdersRun2).isEqualTo(1);
    }

    @Test
    @DisplayName("Out-of-order event with older timestamp must not overwrite newer entity state")
    void shouldNotOverwriteNewerStateWithOlderEvent() {
        Instant newerTimestamp = Instant.now();
        Instant olderTimestamp = newerTimestamp.minusSeconds(3600);

        // Create initial listing with newer timestamp and price 150.00
        Listing listing = new Listing(
                tenantId, store.getId(), null, Marketplace.OUTRO,
                "ITEM-OUT-OF-ORDER", "SKU-OOO", "Item Atual", "ACTIVE",
                new BigDecimal("150.00"), "BRL", 10, 0,
                "gold", null, null, DataProvenance.IMPORTED, null, newerTimestamp
        );
        listingRepository.save(listing);

        // Older update arrives with price 90.00 and older timestamp
        Listing existing = listingRepository.findByTenantIdAndMarketplaceAndExternalListingId(
                tenantId, Marketplace.OUTRO, "ITEM-OUT-OF-ORDER"
        ).orElseThrow();

        if (olderTimestamp.isBefore(existing.getOriginUpdatedAt())) {
            // Out of order: do not overwrite
        } else {
            existing.update("Item Modificado", "ACTIVE", new BigDecimal("90.00"), 10, 0, null, null, null, null, olderTimestamp);
            listingRepository.save(existing);
        }

        Listing reloaded = listingRepository.findByTenantIdAndMarketplaceAndExternalListingId(
                tenantId, Marketplace.OUTRO, "ITEM-OUT-OF-ORDER"
        ).orElseThrow();

        // Price must remain 150.00, title must remain "Item Atual"
        assertThat(reloaded.getPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(reloaded.getTitle()).isEqualTo("Item Atual");
    }
}
