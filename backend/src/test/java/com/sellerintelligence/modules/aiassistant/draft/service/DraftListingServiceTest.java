package com.sellerintelligence.modules.aiassistant.draft.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.modules.aiassistant.draft.catalog.MarketplaceLimitCatalog;
import com.sellerintelligence.modules.aiassistant.draft.entity.DraftListing;
import com.sellerintelligence.modules.aiassistant.draft.entity.DraftListingVersion;
import com.sellerintelligence.modules.aiassistant.draft.guardrails.ClaimViolationException;
import com.sellerintelligence.modules.aiassistant.draft.guardrails.ListingClaimGuardrailService;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftApproveRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftEditRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftGenerationRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftListingDetailDto;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftListingSummaryDto;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftListingVersionDto;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftRejectRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftTone;
import com.sellerintelligence.modules.aiassistant.draft.repository.DraftListingRepository;
import com.sellerintelligence.modules.aiassistant.draft.repository.DraftListingVersionRepository;
import com.sellerintelligence.modules.catalog.Product;
import com.sellerintelligence.modules.catalog.ProductRepository;
import com.sellerintelligence.modules.catalog.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DraftListingService - Testes de Negócio e Geração de Rascunhos de Anúncios (P020-BE)")
class DraftListingServiceTest {

    @Mock
    private DraftListingRepository draftListingRepository;

    @Mock
    private DraftListingVersionRepository draftVersionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    private MarketplaceLimitCatalog marketplaceLimitCatalog;
    private ListingClaimGuardrailService claimGuardrailService;
    private ObjectMapper objectMapper;

    private DraftListingService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        marketplaceLimitCatalog = new MarketplaceLimitCatalog();
        claimGuardrailService = new ListingClaimGuardrailService();
        objectMapper = new ObjectMapper();

        service = new DraftListingService(
                draftListingRepository,
                draftVersionRepository,
                productRepository,
                productVariantRepository,
                marketplaceLimitCatalog,
                claimGuardrailService,
                objectMapper
        );
    }

    @Test
    @DisplayName("Deve gerar rascunho para Mercado Livre respeitando limite oficial de 60 caracteres no título")
    void shouldGenerateDraftForMercadoLivreWithin60CharLimit() {
        Product mockProduct = new Product(
                productId,
                tenantId,
                "Jogo de Panelas Solar",
                "PAN-SOLAR-001",
                "Tramontina",
                "Panelas",
                "ACTIVE"
        );
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(mockProduct));

        DraftGenerationRequest request = new DraftGenerationRequest(
                productId,
                null,
                "MERCADO_LIVRE",
                DraftTone.INFORMATIVE_OBJECTIVE,
                "Tramontina",
                "Solar",
                "Panelas",
                List.of("Aço Inox"),
                Map.of("diâmetro", "20cm"),
                "90 dias legal",
                Map.of("fundo", "triplo"),
                List.of("Resistente à corrosão"),
                List.of("cozinha", "indução"),
                "LISTING_DRAFT_GENERATION",
                null
        );

        DraftListingDetailDto detail = service.generateDraft(tenantId, request);

        assertThat(detail).isNotNull();
        assertThat(detail.targetMarketplace()).isEqualTo("MERCADO_LIVRE");
        assertThat(detail.status()).isEqualTo("DRAFT");
        assertThat(detail.currentVersionNumber()).isEqualTo(1);
        assertThat(detail.publicationStatus()).isEqualTo("NOT_PUBLISHED_DRAFT_ONLY");
        assertThat(detail.externalPublishBlocked()).isTrue();

        DraftListingVersionDto version = detail.currentVersion();
        assertThat(version.generatedTitle()).isNotBlank();
        assertThat(version.generatedTitle().length()).isLessThanOrEqualTo(60);
        assertThat(version.titleCharCount()).isLessThanOrEqualTo(60);
        assertThat(version.characterLimitValid()).isTrue();
        assertThat(version.userEdited()).isFalse();
        assertThat(version.generatedDescription()).contains("[DESCRIÇÃO DO PRODUTO]");
        assertThat(version.generatedDescription()).contains("[ESPECIFICAÇÕES TÉCNICAS CONFIRMADAS]");
        assertThat(version.generatedDescription()).contains("Tramontina");

        verify(draftListingRepository).save(any(DraftListing.class));
        verify(draftVersionRepository).save(any(DraftListingVersion.class));
    }

    @Test
    @DisplayName("Deve gerar rascunho para Shopee respeitando limites oficiais de [20-120] caracteres no título")
    void shouldGenerateDraftForShopeeWithinOfficialLimits() {
        DraftGenerationRequest request = new DraftGenerationRequest(
                null,
                null,
                "SHOPEE",
                DraftTone.COMMERCIAL_POLITE,
                "Baseus",
                "GaN5",
                "Carregador",
                List.of("Plástico ABS"),
                Map.of("potência", "65W"),
                "90 dias",
                null,
                List.of("Carregamento rápido seguro"),
                List.of("usb-c", "notebook"),
                null,
                null
        );

        DraftListingDetailDto detail = service.generateDraft(tenantId, request);

        assertThat(detail).isNotNull();
        assertThat(detail.targetMarketplace()).isEqualTo("SHOPEE");
        assertThat(detail.currentVersion().generatedTitle().length()).isBetween(20, 120);
        assertThat(detail.currentVersion().generatedDescription().length()).isBetween(20, 3000);
        assertThat(detail.publicationStatus()).isEqualTo("NOT_PUBLISHED_DRAFT_ONLY");
    }

    @Test
    @DisplayName("Deve gerar rascunho com Tom Técnico focado em parâmetros e engenharia")
    void shouldGenerateDraftWithTechnicalTone() {
        DraftGenerationRequest request = new DraftGenerationRequest(
                null,
                null,
                "MERCADO_LIVRE",
                DraftTone.TECHNICAL,
                "Bosch",
                "GSR 120-LI",
                "Parafusadeira",
                List.of("Aço e Polímero"),
                Map.of("voltagem", "12V"),
                "12 meses conforme manual",
                Map.of("torque", "30 Nm"),
                List.of("Mandril de aperto rápido"),
                List.of("bateria", "ferramenta"),
                null,
                null
        );

        DraftListingDetailDto detail = service.generateDraft(tenantId, request);

        assertThat(detail.currentVersion().tone()).isEqualTo("TECHNICAL");
        assertThat(detail.currentVersion().generatedDescription()).contains("parâmetros técnicos de robustez");
    }

    @Test
    @DisplayName("Deve rejeitar geração quando nenhum atributo confirmado for fornecido")
    void shouldRejectWhenNoConfirmedAttributesProvided() {
        DraftGenerationRequest request = new DraftGenerationRequest(
                null,
                null,
                "MERCADO_LIVRE",
                DraftTone.INFORMATIVE_OBJECTIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null
        );

        assertThatThrownBy(() -> service.generateDraft(tenantId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pelo menos um atributo confirmado");
    }

    @Test
    @DisplayName("Deve rejeitar marketplace não validado na geração")
    void shouldRejectUnvalidatedMarketplaceOnGeneration() {
        DraftGenerationRequest request = new DraftGenerationRequest(
                null,
                null,
                "AMAZON_UNVERIFIED",
                DraftTone.INFORMATIVE_OBJECTIVE,
                "Marca",
                "Modelo",
                "Categoria",
                null,
                null,
                null,
                null,
                null,
                List.of("kw"),
                null,
                null
        );

        assertThatThrownBy(() -> service.generateDraft(tenantId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não possui regras oficiais de limites de caracteres validadas no MVP");
    }

    @Test
    @DisplayName("Deve permitir edição humana gerando nova versão com status REVIEWED e userEdited=true")
    void shouldEditDraftSuccessfully() {
        UUID draftId = UUID.randomUUID();
        DraftListing draft = new DraftListing(draftId, tenantId, productId, null, "MERCADO_LIVRE", 1, "DRAFT");

        DraftListingVersion v1 = new DraftListingVersion(
                UUID.randomUUID(),
                tenantId,
                draftId,
                1,
                "LISTING_DRAFT_GENERATION",
                "v1.0.0",
                "mock-deterministic-v1",
                "MOCK_PROVIDER",
                "INFORMATIVE_OBJECTIVE",
                "MERCADO_LIVRE",
                "{\"brand\":\"Tramontina\",\"model\":\"Solar\",\"category\":\"Panela\",\"dimensions\":{\"diametro\":\"20cm\"}}",
                "Panela Tramontina Solar Aço Inox",
                "Descrição inicial...",
                null,
                null,
                "{\"brand\":\"Tramontina\"}",
                null,
                32,
                20,
                true,
                false,
                null
        );

        when(draftListingRepository.findByIdAndTenantId(draftId, tenantId)).thenReturn(Optional.of(draft));
        when(draftVersionRepository.findByTenantIdAndDraftListingIdAndVersionNumber(tenantId, draftId, 1))
                .thenReturn(Optional.of(v1));

        DraftEditRequest editRequest = new DraftEditRequest(
                "Panela Tramontina Solar Aço Inox 20cm Original",
                "Descrição revisada pelo vendedor com especificações completas de fábrica.",
                "Ajustado para maior apelo no Mercado Livre"
        );

        DraftListingDetailDto updated = service.editDraft(tenantId, draftId, editRequest);

        assertThat(updated.currentVersionNumber()).isEqualTo(2);
        assertThat(updated.status()).isEqualTo("REVIEWED");
        assertThat(updated.currentVersion().userEdited()).isTrue();
        assertThat(updated.currentVersion().effectiveTitle()).isEqualTo(editRequest.reviewedTitle());
        assertThat(updated.currentVersion().effectiveDescription()).isEqualTo(editRequest.reviewedDescription());
        assertThat(updated.currentVersion().reviewNotes()).isEqualTo("Ajustado para maior apelo no Mercado Livre");

        ArgumentCaptor<DraftListing> draftCaptor = ArgumentCaptor.forClass(DraftListing.class);
        verify(draftListingRepository).save(draftCaptor.capture());
        assertThat(draftCaptor.getValue().getCurrentVersionNumber()).isEqualTo(2);
        assertThat(draftCaptor.getValue().getStatus()).isEqualTo("REVIEWED");
    }

    @Test
    @DisplayName("Deve bloquear edição que tente injetar certificação falsa ou claim não confirmada")
    void shouldBlockEditWithUnprovenClaims() {
        UUID draftId = UUID.randomUUID();
        DraftListing draft = new DraftListing(draftId, tenantId, productId, null, "MERCADO_LIVRE", 1, "DRAFT");

        DraftListingVersion v1 = new DraftListingVersion(
                UUID.randomUUID(),
                tenantId,
                draftId,
                1,
                "LISTING_DRAFT_GENERATION",
                "v1.0.0",
                "mock-deterministic-v1",
                "MOCK_PROVIDER",
                "INFORMATIVE_OBJECTIVE",
                "MERCADO_LIVRE",
                "{\"brand\":\"Tramontina\",\"category\":\"Panela\"}",
                "Panela Tramontina",
                "Desc",
                null,
                null,
                "{}",
                null,
                17,
                4,
                true,
                false,
                null
        );

        when(draftListingRepository.findByIdAndTenantId(draftId, tenantId)).thenReturn(Optional.of(draft));
        when(draftVersionRepository.findByTenantIdAndDraftListingIdAndVersionNumber(tenantId, draftId, 1))
                .thenReturn(Optional.of(v1));

        DraftEditRequest editRequest = new DraftEditRequest(
                "Panela Tramontina Certificado Inmetro",
                "Produto com garantia vitalícia e fórmula milagrosa.",
                "Tentativa de superlativos e certificações"
        );

        assertThatThrownBy(() -> service.editDraft(tenantId, draftId, editRequest))
                .isInstanceOf(ClaimViolationException.class)
                .hasMessageContaining("guardrails de claims");
    }

    @Test
    @DisplayName("Deve aprovar rascunho com sucesso mantendo publicação externa categoricamente bloqueada")
    void shouldApproveDraftWithoutExternalPublish() {
        UUID draftId = UUID.randomUUID();
        DraftListing draft = new DraftListing(draftId, tenantId, productId, null, "MERCADO_LIVRE", 1, "REVIEWED");

        DraftListingVersion v1 = new DraftListingVersion(
                UUID.randomUUID(),
                tenantId,
                draftId,
                1,
                "LISTING_DRAFT_GENERATION",
                "v1.0.0",
                "mock-deterministic-v1",
                "MOCK_PROVIDER",
                "INFORMATIVE_OBJECTIVE",
                "MERCADO_LIVRE",
                "{\"brand\":\"Tramontina\"}",
                "Panela Tramontina Solar",
                "Descrição homologada",
                null,
                null,
                "{}",
                null,
                23,
                20,
                true,
                false,
                null
        );

        when(draftListingRepository.findByIdAndTenantId(draftId, tenantId)).thenReturn(Optional.of(draft));
        when(draftVersionRepository.findByTenantIdAndDraftListingIdAndVersionNumber(tenantId, draftId, 1))
                .thenReturn(Optional.of(v1));

        DraftApproveRequest approveRequest = new DraftApproveRequest("Aprovado pelo gestor de canal");
        DraftListingDetailDto approved = service.approveDraft(tenantId, draftId, approveRequest);

        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(approved.publicationStatus()).isEqualTo("NOT_PUBLISHED_DRAFT_ONLY");
        assertThat(approved.externalPublishBlocked()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar rascunho registrando motivo nos apontamentos de revisão")
    void shouldRejectDraftWithReason() {
        UUID draftId = UUID.randomUUID();
        DraftListing draft = new DraftListing(draftId, tenantId, productId, null, "MERCADO_LIVRE", 1, "DRAFT");

        DraftListingVersion v1 = new DraftListingVersion(
                UUID.randomUUID(),
                tenantId,
                draftId,
                1,
                "LISTING_DRAFT_GENERATION",
                "v1.0.0",
                "mock-deterministic-v1",
                "MOCK_PROVIDER",
                "INFORMATIVE_OBJECTIVE",
                "MERCADO_LIVRE",
                "{\"brand\":\"Tramontina\"}",
                "Panela Tramontina",
                "Desc",
                null,
                null,
                "{}",
                null,
                17,
                4,
                true,
                false,
                null
        );

        when(draftListingRepository.findByIdAndTenantId(draftId, tenantId)).thenReturn(Optional.of(draft));
        when(draftVersionRepository.findByTenantIdAndDraftListingIdAndVersionNumber(tenantId, draftId, 1))
                .thenReturn(Optional.of(v1));

        DraftRejectRequest rejectRequest = new DraftRejectRequest("Título muito genérico, aguardar nova foto");
        DraftListingDetailDto rejected = service.rejectDraft(tenantId, draftId, rejectRequest);

        assertThat(rejected.status()).isEqualTo("REJECTED");
        assertThat(rejected.currentVersion().reviewNotes()).contains("Rejeição: Título muito genérico");
    }

    @Test
    @DisplayName("Deve listar rascunhos filtrando por marketplace e status")
    void shouldListDraftsWithFilters() {
        DraftListing d1 = new DraftListing(UUID.randomUUID(), tenantId, productId, null, "MERCADO_LIVRE", 1, "DRAFT");
        PageRequest pageable = PageRequest.of(0, 10);

        when(draftListingRepository.findByTenantIdAndTargetMarketplaceAndStatus(tenantId, "MERCADO_LIVRE", "DRAFT", pageable))
                .thenReturn(new PageImpl<>(List.of(d1)));

        Page<DraftListingSummaryDto> page = service.listDrafts(tenantId, "MERCADO_LIVRE", "DRAFT", pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).targetMarketplace()).isEqualTo("MERCADO_LIVRE");
        assertThat(page.getContent().get(0).status()).isEqualTo("DRAFT");
        assertThat(page.getContent().get(0).publicationStatus()).isEqualTo("NOT_PUBLISHED_DRAFT_ONLY");
    }
}
