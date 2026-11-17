package br.com.sellerintelligence.ai.service;

import br.com.sellerintelligence.ai.domain.*;
import br.com.sellerintelligence.ai.dto.*;
import br.com.sellerintelligence.ai.provider.AiServiceUnavailableException;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.sync.domain.Listing;
import br.com.sellerintelligence.sync.domain.ListingRepository;
import br.com.sellerintelligence.sync.domain.Product;
import br.com.sellerintelligence.sync.domain.ProductRepository;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ListingStudioIntegrationTest {

    @Autowired
    private ListingStudioService listingStudioService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ListingVersionRepository versionRepository;

    @Autowired
    private ListingQualityAuditRepository auditRepository;

    @Autowired
    private AiGenerationLogRepository logRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    private UUID tenantAId;
    private UUID tenantBId;
    private Listing listingA;

    @BeforeEach
    void setUp() {
        Organization orgA = organizationRepository.save(new Organization("Org AI Test A", "org-ai-a"));
        Organization orgB = organizationRepository.save(new Organization("Org AI Test B", "org-ai-b"));
        tenantAId = orgA.getId();
        tenantBId = orgB.getId();

        TenantContext.setTenant(new TenantContext.TenantInfo(tenantAId, UUID.randomUUID(), "sub-a", "user-a@test.com", Role.OWNER));

        Company compA = companyRepository.save(new Company(tenantAId, "11222333000181", "Empresa AI A", "Razao AI", TaxRegime.SIMPLES_NACIONAL));
        Store storeA = storeRepository.save(new Store(tenantAId, compA, Marketplace.MERCADO_LIVRE, "Loja ML A"));

        Product productA = productRepository.save(new Product(
                tenantAId, storeA.getId(), "SKU-MONITOR-4K", "Monitor Gamer 27 4K 144Hz",
                "Dell", "Monitores", "7891234567891", new BigDecimal("1800.00"),
                DataProvenance.IMPORTED
        ));

        listingA = listingRepository.save(new Listing(
                tenantAId, storeA.getId(), productA.getId(), Marketplace.MERCADO_LIVRE, "MLB-MON-27",
                "SKU-MONITOR-4K", "Monitor Gamer Dell 27 Polegadas 4K Ultra HD 144Hz IPS", "ACTIVE",
                new BigDecimal("2499.00"), "BRL", 20, 5, "gold_pro",
                "https://mercadolivre.com/mlb-mon", "https://img.com/mon.jpg", DataProvenance.IMPORTED,
                "{\"modelo\":\"G2724D\",\"cor\":\"Preto\",\"garantia\":\"36 meses\"}", Instant.now()
        ));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Auditoria de qualidade persiste registro e detalha score e recomendações")
    void shouldAuditListingAndPersistAudit() {
        QualityAuditResultDto audit = listingStudioService.auditListing(listingA.getId());

        assertThat(audit).isNotNull();
        assertThat(audit.overallScore()).isGreaterThanOrEqualTo(60);
        assertThat(audit.criteriaBreakdown()).isNotEmpty();
        assertThat(audit.missingRequiredAttributes()).contains("Condicao");

        // Verifica persistência na tabela listing_quality_audits
        List<ListingQualityAudit> audits = auditRepository.findAll();
        assertThat(audits).anyMatch(a -> a.getListingId().equals(listingA.getId()) && a.getOverallScore() == audit.overallScore());
    }

    @Test
    @DisplayName("Geração de título assistida usa atributos confirmados e registra log de auditoria")
    void shouldGenerateTitleAndLogExecution() {
        ListingGenerationRequest request = new ListingGenerationRequest(
                "TITLE",
                List.of("Gamer", "4K"),
                Map.of("voltagem", "Bivolt")
        );

        ListingGenerationResponse response = listingStudioService.generateContent(listingA.getId(), request);

        assertThat(response.promptType()).isEqualTo("TITLE");
        assertThat(response.suggestedTitle()).isNotBlank();
        assertThat(response.suggestedTitle()).contains("Dell");
        assertThat(response.confirmedAttributesUsed()).containsKey("marca");
        assertThat(response.dataProvenance()).isEqualTo("AI_GENERATED");
        assertThat(response.disclaimer()).contains("Sugestão gerada por Inteligência Artificial");

        // Verifica registro de log em ai_generation_logs
        List<AiGenerationLog> logs = logRepository.findByTenantIdOrderByCreatedAtDesc(tenantAId);
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(logs.get(0).getPromptType()).isEqualTo("TITLE");
        assertThat(logs.get(0).getLatencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Geração de descrição técnica estrutura tópicos e seções sem alucinação")
    void shouldGenerateDescriptionStructured() {
        ListingGenerationRequest request = new ListingGenerationRequest("DESCRIPTION", List.of(), Map.of());

        ListingGenerationResponse response = listingStudioService.generateContent(listingA.getId(), request);

        assertThat(response.promptType()).isEqualTo("DESCRIPTION");
        assertThat(response.suggestedDescription()).contains("## Apresentação do Produto");
        assertThat(response.suggestedDescription()).contains("## Ficha Técnica Confirmada");
        assertThat(response.suggestedDescription()).contains("Dell");
    }

    @Test
    @DisplayName("Fallback explícito: Quando o provedor está indisponível, lança erro sem fabricar texto")
    void shouldThrowWhenAiProviderIsUnavailableAndLogFailure() {
        ListingGenerationRequest request = new ListingGenerationRequest(
                "TITLE",
                List.of(),
                Map.of("teste", "SIMULATE_AI_UNAVAILABLE")
        );

        assertThatThrownBy(() -> listingStudioService.generateContent(listingA.getId(), request))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("Provedor de inteligência artificial indisponível");

        // Confirma que log de erro foi gravado
        List<AiGenerationLog> logs = logRepository.findByTenantIdOrderByCreatedAtDesc(tenantAId);
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getStatus()).isEqualTo("ERROR");
        assertThat(logs.get(0).getErrorCode()).isEqualTo("AI_SERVICE_UNAVAILABLE");
    }

    @Test
    @DisplayName("Controle de versões e rascunhos: Salva múltiplas versões e aceita sem alterar marketplace")
    void shouldManageDraftVersionsAndAccept() {
        // Salva versão 1
        SaveListingVersionRequest req1 = new SaveListingVersionRequest(
                "Monitor Gamer Dell 27 4K 144Hz G2724D Preto - Versao 1",
                "Descricao inicial",
                "{}",
                "DRAFT"
        );
        ListingVersionDto v1 = listingStudioService.saveVersion(listingA.getId(), req1);
        assertThat(v1.versionNumber()).isEqualTo(1);
        assertThat(v1.status()).isEqualTo("DRAFT");

        // Salva versão 2
        SaveListingVersionRequest req2 = new SaveListingVersionRequest(
                "Monitor Gamer Dell 27 4K 144Hz G2724D Preto - Versao 2 Otimizada",
                "Descricao revisada",
                "{}",
                "DRAFT"
        );
        ListingVersionDto v2 = listingStudioService.saveVersion(listingA.getId(), req2);
        assertThat(v2.versionNumber()).isEqualTo(2);

        // Lista versões
        List<ListingVersionDto> versions = listingStudioService.listVersions(listingA.getId());
        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).versionNumber()).isEqualTo(2);

        // Aceita versão 2
        ListingVersionDto accepted = listingStudioService.acceptVersion(listingA.getId(), v2.id());
        assertThat(accepted.status()).isEqualTo("ACCEPTED");

        // Verifica que o anúncio original no marketplace NÃO foi alterado
        Listing unchanged = listingRepository.findById(listingA.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("Monitor Gamer Dell 27 Polegadas 4K Ultra HD 144Hz IPS");
    }

    @Test
    @DisplayName("Isolamento multi-tenant: Tenant B não pode auditar ou gerar conteúdo para anúncio de Tenant A")
    void shouldEnforceMultiTenantIsolation() {
        // Muda para Tenant B
        TenantContext.setTenant(new TenantContext.TenantInfo(tenantBId, UUID.randomUUID(), "sub-b", "user-b@test.com", Role.OWNER));

        assertThatThrownBy(() -> listingStudioService.auditListing(listingA.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Anúncio não encontrado para o tenant ativo");

        assertThatThrownBy(() -> listingStudioService.generateContent(listingA.getId(), new ListingGenerationRequest("TITLE", List.of(), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Anúncio não encontrado para o tenant ativo");

        assertThatThrownBy(() -> listingStudioService.listVersions(listingA.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Anúncio não encontrado para o tenant ativo");
    }
}
