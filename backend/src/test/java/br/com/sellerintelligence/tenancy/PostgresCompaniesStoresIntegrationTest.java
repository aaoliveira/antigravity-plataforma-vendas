package br.com.sellerintelligence.tenancy;

import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
import br.com.sellerintelligence.tenancy.domain.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class PostgresCompaniesStoresIntegrationTest {

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

    @Test
    @DisplayName("Verify Flyway V3 and PostgreSQL schema constraints for companies and stores")
    void testPostgresV3SchemaAndConstraints() {
        assertThat(postgres.isRunning()).isTrue();

        User user = userRepository.save(new User("sub-pg-v3", "pgv3@example.com", "Postgres V3 User"));
        Organization org = organizationRepository.save(new Organization("PG V3 Holding", "pg-v3-holding"));

        Company comp = companyRepository.save(new Company(
                org.getId(), "06.990.590/0001-23", "Google PG", "Google Brasil Ltda", TaxRegime.LUCRO_REAL));
        assertThat(comp.getId()).isNotNull();

        Store store = storeRepository.save(new Store(org.getId(), comp, Marketplace.MERCADO_LIVRE, "PG ML Store"));
        assertThat(store.getId()).isNotNull();

        // Enforce unique CNPJ per tenant in PostgreSQL
        Company duplicateCnpj = new Company(org.getId(), "06.990.590/0001-23", "Google Copy", null, TaxRegime.SIMPLES_NACIONAL);
        assertThatThrownBy(() -> companyRepository.saveAndFlush(duplicateCnpj))
                .isNotNull();
    }
}
