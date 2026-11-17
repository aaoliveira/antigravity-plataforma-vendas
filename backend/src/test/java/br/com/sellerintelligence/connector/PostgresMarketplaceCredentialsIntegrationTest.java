package br.com.sellerintelligence.connector;

import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.connector.model.MarketplaceCredential;
import br.com.sellerintelligence.connector.model.MarketplaceCredentialRepository;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class PostgresMarketplaceCredentialsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private MarketplaceCredentialRepository credentialRepository;

    @Test
    @DisplayName("Verify Flyway V4 and PostgreSQL schema constraints for marketplace credentials")
    void testPostgresV4SchemaAndConstraints() {
        assertThat(postgres.isRunning()).isTrue();

        User user = userRepository.save(new User("sub-pg-v4", "pgv4@example.com", "Postgres V4 User"));
        Organization org = organizationRepository.save(new Organization("PG V4 Holding", "pg-v4-holding"));

        Company comp = companyRepository.save(new Company(
                org.getId(), "06.990.590/0001-23", "Company V4", "Company V4 Razao", TaxRegime.SIMPLES_NACIONAL));

        Store store = storeRepository.save(new Store(org.getId(), comp, Marketplace.MERCADO_LIVRE, "ML V4 Store"));

        MarketplaceCredential cred = new MarketplaceCredential(
                org.getId(),
                store,
                Marketplace.MERCADO_LIVRE,
                "encrypted-token-xyz",
                "encrypted-refresh-xyz",
                "ext-seller-999",
                "read,write",
                OffsetDateTime.now().plusHours(6),
                OffsetDateTime.now().plusMonths(6),
                "PRODUCTION"
        );
        credentialRepository.save(cred);
        assertThat(cred.getId()).isNotNull();

        // Enforce unique store_id in marketplace_credentials
        MarketplaceCredential duplicateCred = new MarketplaceCredential(
                org.getId(),
                store,
                Marketplace.MERCADO_LIVRE,
                "encrypted-token-duplicate",
                null,
                "ext-seller-999",
                "read",
                OffsetDateTime.now().plusHours(6),
                null,
                "PRODUCTION"
        );
        assertThatThrownBy(() -> credentialRepository.saveAndFlush(duplicateCred))
                .isNotNull();
    }
}
