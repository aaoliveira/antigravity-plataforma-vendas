package br.com.sellerintelligence.sync.web;

import br.com.sellerintelligence.audit.domain.AuditEvent;
import br.com.sellerintelligence.audit.domain.AuditEventRepository;
import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.sync.domain.*;
import br.com.sellerintelligence.sync.service.SyncJobService;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SyncCrossTenantIntegrationTest {

    @Autowired
    private SyncJobService syncJobService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private UUID tenantAId;
    private UUID tenantBId;
    private Store storeA;
    private Store storeB;
    private User userA;

    @BeforeEach
    void setUp() {
        // Tenant A
        Organization orgA = organizationRepository.save(new Organization("Tenant A", "tenant-a"));
        tenantAId = orgA.getId();

        userA = userRepository.save(new User("sub-user-a", "user-a@seller.test", "User A"));
        membershipRepository.save(new Membership(orgA, userA, Role.OWNER));

        Company compA = companyRepository.save(new Company(tenantAId, "11111111000111", "Empresa A", "Fantasia A", TaxRegime.SIMPLES_NACIONAL));
        storeA = new Store(tenantAId, compA, Marketplace.MERCADO_LIVRE, "Loja A");
        storeA.setExternalAccountId("ACCT-A");
        storeA = storeRepository.save(storeA);

        // Tenant B
        Organization orgB = organizationRepository.save(new Organization("Tenant B", "tenant-b"));
        tenantBId = orgB.getId();

        User userB = userRepository.save(new User("sub-user-b", "user-b@seller.test", "User B"));
        membershipRepository.save(new Membership(orgB, userB, Role.OWNER));

        Company compB = companyRepository.save(new Company(tenantBId, "22222222000122", "Empresa B", "Fantasia B", TaxRegime.SIMPLES_NACIONAL));
        storeB = new Store(tenantBId, compB, Marketplace.SHOPEE, "Loja B");
        storeB.setExternalAccountId("ACCT-B");
        storeB = storeRepository.save(storeB);

        // Create listing in Tenant B
        Listing listingB = new Listing(
                tenantBId, storeB.getId(), null, Marketplace.SHOPEE,
                "ITEM-B-999", "SKU-B", "Item Tenant B", "ACTIVE",
                new BigDecimal("199.90"), "BRL", 5, 1,
                "shopee", null, null, DataProvenance.IMPORTED, null, Instant.now()
        );
        listingRepository.save(listingB);

        // Set context to Tenant A
        TenantContext.setTenant(new TenantContext.TenantInfo(
                tenantAId, userA.getId(), userA.getKeycloakSub(), userA.getEmail(), Role.OWNER
        ));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Tenant A cannot trigger sync for store belonging to Tenant B (denied with audit log)")
    void shouldDenySyncTriggerOnCrossTenantStore() {
        assertThatThrownBy(() -> syncJobService.triggerSync(storeB.getId(), SyncResourceType.CATALOG, SyncTriggerType.MANUAL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Store not found or access denied");

        List<AuditEvent> auditEvents = auditEventRepository.findByTenantIdOrderByCreatedAtDesc(tenantAId);
        boolean deniedFound = auditEvents.stream()
                .anyMatch(e -> "ACCESS_DENIED".equals(e.getAction()) && e.getResult() == AuditResult.DENIED);
        assertThat(deniedFound).isTrue();
    }

    @Test
    @DisplayName("Tenant A cannot see listings or products belonging to Tenant B")
    void shouldIsolateCatalogListingsBetweenTenants() {
        List<Listing> listingsTenantA = listingRepository.findByTenantId(tenantAId);
        assertThat(listingsTenantA).isEmpty();

        List<Listing> listingsTenantB = listingRepository.findByTenantId(tenantBId);
        assertThat(listingsTenantB).hasSize(1);
        assertThat(listingsTenantB.get(0).getTitle()).isEqualTo("Item Tenant B");
    }
}
