package br.com.sellerintelligence.discovery.web;

import br.com.sellerintelligence.discovery.domain.*;
import br.com.sellerintelligence.discovery.service.*;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryCatalogController {

    private final CatalogReviewService catalogReviewService;
    private final ProductSimilarityMatcherService similarityMatcherService;
    private final VisualSearchService visualSearchService;

    public DiscoveryCatalogController(CatalogReviewService catalogReviewService,
                                     ProductSimilarityMatcherService similarityMatcherService,
                                     VisualSearchService visualSearchService) {
        this.catalogReviewService = catalogReviewService;
        this.similarityMatcherService = similarityMatcherService;
        this.visualSearchService = visualSearchService;
    }

    // 1. Upload e Gestão de Catálogos
    @PostMapping("/catalogs/upload")
    public ResponseEntity<DiscoveryCatalog> uploadCatalog(@Valid @RequestBody UploadCatalogRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(request.fileContentBase64());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Conteúdo do arquivo não é um Base64 válido.");
        }

        DiscoveryCatalog catalog = catalogReviewService.uploadAndProcessCatalog(
                tenantId, request.filename(), fileBytes, request.supplierName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog);
    }

    @GetMapping("/catalogs")
    public ResponseEntity<List<DiscoveryCatalog>> listCatalogs() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(catalogReviewService.listCatalogs(tenantId));
    }

    @GetMapping("/catalogs/{id}/items")
    public ResponseEntity<List<DiscoveryCatalogItem>> listCatalogItems(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(catalogReviewService.listCatalogItems(tenantId, id));
    }

    @DeleteMapping("/catalogs/{id}")
    public ResponseEntity<Void> deleteCatalog(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        catalogReviewService.deleteCatalog(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // 2. Revisão Humana de Itens
    @PutMapping("/items/{id}/review")
    public ResponseEntity<DiscoveryCatalogItem> reviewItem(@PathVariable UUID id,
                                                          @Valid @RequestBody ReviewItemRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        DiscoveryCatalogItem reviewed = catalogReviewService.reviewAndConfirmItem(
                tenantId, id, request.name(), request.supplierSku(), request.ean(),
                request.description(), request.costPrice(), request.weightKg(), request.dimensionsCm()
        );
        return ResponseEntity.ok(reviewed);
    }

    @PostMapping("/items/{id}/reject")
    public ResponseEntity<DiscoveryCatalogItem> rejectItem(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(catalogReviewService.rejectItem(tenantId, id));
    }

    // 3. Busca de Similaridade e Candidatos
    @PostMapping("/items/{id}/matches")
    public ResponseEntity<List<DiscoveryProductMatch>> generateMatches(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();

        // Candidatos de mercado padrão para comparação de similaridade
        List<ProductSimilarityMatcherService.CandidateProductProposal> candidates = List.of(
                new ProductSimilarityMatcherService.CandidateProductProposal(
                        "MERCADO_LIVRE", "MLB1029384", "Teclado Mecânico RGB Switch Blue",
                        new BigDecimal("199.90"), "7891234567895", "https://mercadolivre.com.br/item/MLB1029384"
                ),
                new ProductSimilarityMatcherService.CandidateProductProposal(
                        "SHOPEE", "SP8899123", "Teclado Gamer Mecânico ABNT2",
                        new BigDecimal("179.00"), null, "https://shopee.com.br/product/SP8899123"
                )
        );

        List<DiscoveryProductMatch> matches = similarityMatcherService.findAndGenerateMatches(tenantId, id, candidates);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/items/{id}/matches")
    public ResponseEntity<List<DiscoveryProductMatch>> listMatches(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(similarityMatcherService.getMatchesForItem(tenantId, id));
    }

    @PostMapping("/matches/{id}/confirm")
    public ResponseEntity<DiscoveryProductMatch> confirmMatch(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(similarityMatcherService.confirmMatch(tenantId, id));
    }

    @PostMapping("/matches/{id}/reject")
    public ResponseEntity<DiscoveryProductMatch> rejectMatch(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(similarityMatcherService.rejectMatch(tenantId, id));
    }

    // 4. Busca Visual por Imagem
    @PostMapping("/visual-search")
    public ResponseEntity<VisualSearchService.VisualSearchResult> visualSearch(@Valid @RequestBody UploadCatalogRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        byte[] imageBytes = Base64.getDecoder().decode(request.fileContentBase64());
        VisualSearchService.VisualSearchResult result = visualSearchService.performVisualSearch(
                tenantId, imageBytes, request.filename()
        );
        return ResponseEntity.ok(result);
    }
}
