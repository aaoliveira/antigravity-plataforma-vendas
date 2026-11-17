package br.com.sellerintelligence.sync.service;

import br.com.sellerintelligence.connector.model.MarketplaceCredentialRepository;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.sync.domain.*;
import br.com.sellerintelligence.sync.infrastructure.rabbitmq.RabbitMQConfig;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SyncResilienceIntegrationTest {

    @Autowired
    private SyncJobService syncJobService;

    @Autowired
    private SyncJobRepository syncJobRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private MarketplaceCredentialRepository credentialRepository;

    private UUID tenantId;
    private Store store;

    @BeforeEach
    void setUp() {
        Organization org = organizationRepository.save(new Organization("Resilience Org", "resilience-org"));
        tenantId = org.getId();

        User user = userRepository.save(new User("sub-resilience-user", "resilience@seller.test", "Resilience User"));
        membershipRepository.save(new Membership(org, user, Role.OWNER));

        TenantContext.setTenant(new TenantContext.TenantInfo(
                tenantId, user.getId(), user.getKeycloakSub(), user.getEmail(), Role.OWNER
        ));

        Company company = companyRepository.save(new Company(
                tenantId, "06.990.590/0001-23", "Resilience Corp", "Resilience Fantasia", TaxRegime.SIMPLES_NACIONAL
        ));

        store = storeRepository.save(new Store(tenantId, company, Marketplace.OUTRO, "Resilience Store"));
    }

    @Test
    @DisplayName("Resilience: Job fails gracefully when credentials are missing during processing, recording explicit error")
    void jobFailsGracefullyWithoutCredentials() {
        // Save pending job directly
        SyncJob job = new SyncJob(tenantId, store.getId(), Marketplace.OUTRO, SyncResourceType.CATALOG, SyncTriggerType.MANUAL);
        job = syncJobRepository.save(job);
        assertThat(job.getStatus()).isEqualTo(SyncStatus.PENDING);

        // Process job
        syncJobService.processJob(job.getId());

        SyncJob finishedJob = syncJobRepository.findById(job.getId()).orElseThrow();
        assertThat(finishedJob.getStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(finishedJob.getErrorSummary()).contains("Credentials missing for store");
        assertThat(finishedJob.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Resilience: RabbitMQ DLQ constants and dead-letter routing are configured")
    void rabbitMqDlqConfigurationIsConsistent() {
        assertThat(RabbitMQConfig.SYNC_EXCHANGE).isEqualTo("sync.exchange");
        assertThat(RabbitMQConfig.SYNC_QUEUE).isEqualTo("sync.job.queue");
        assertThat(RabbitMQConfig.SYNC_DLX).isEqualTo("sync.dlx");
        assertThat(RabbitMQConfig.SYNC_DLQ).isEqualTo("sync.job.dlq");
        assertThat(RabbitMQConfig.SYNC_DLQ_ROUTING_KEY).isEqualTo("sync.job.dead");
    }

    @Test
    @DisplayName("Resilience: Duplicate execution of an already completed job is a no-op")
    void duplicateExecutionOfCompletedJobIsNoop() {
        SyncJob job = new SyncJob(tenantId, store.getId(), Marketplace.OUTRO, SyncResourceType.CATALOG, SyncTriggerType.MANUAL);
        job.start();
        job.complete("cursor-999");
        job = syncJobRepository.save(job);

        // Run processJob again
        syncJobService.processJob(job.getId());

        SyncJob afterJob = syncJobRepository.findById(job.getId()).orElseThrow();
        assertThat(afterJob.getStatus()).isEqualTo(SyncStatus.COMPLETED);
    }
}
