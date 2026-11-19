package com.sellerintelligence.modules.aiassistant.draft;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftApproveRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftEditRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftGenerationRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftListingDetailDto;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftListingSummaryDto;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftListingVersionDto;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftRejectRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.MarketplaceRuleLimitsDto;
import com.sellerintelligence.modules.aiassistant.draft.service.DraftListingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para geração assistida de anúncios, edição e controle de versões (P020-BE).
 * Restringe claims e valida limites de caracteres oficiais de cada canal.
 * Nenhuma publicação externa é permitida no MVP.
 */
@RestController
@RequestMapping("/api/v1/ai/drafts")
public class DraftListingController {

    private final DraftListingService draftListingService;

    public DraftListingController(DraftListingService draftListingService) {
        this.draftListingService = draftListingService;
    }

    /**
     * Gera um novo rascunho de anúncio a partir de atributos confirmados.
     */
    @PostMapping("/generate")
    public ResponseEntity<DraftListingDetailDto> generateDraft(
            @Valid @RequestBody DraftGenerationRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        DraftListingDetailDto response = draftListingService.generateDraft(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista rascunhos de anúncios do tenant com paginação e filtros opcionais.
     */
    @GetMapping
    public ResponseEntity<Page<DraftListingSummaryDto>> listDrafts(
            @RequestParam(required = false) String targetMarketplace,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Page<DraftListingSummaryDto> drafts = draftListingService.listDrafts(tenantId, targetMarketplace, status, pageable);
        return ResponseEntity.ok(drafts);
    }

    /**
     * Consulta detalhe do rascunho com histórico de versões.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DraftListingDetailDto> getDraft(
            @PathVariable UUID id
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        DraftListingDetailDto draft = draftListingService.getDraft(tenantId, id);
        return ResponseEntity.ok(draft);
    }

    /**
     * Consulta versão histórica específica do rascunho.
     */
    @GetMapping("/{id}/versions/{versionNumber}")
    public ResponseEntity<DraftListingVersionDto> getDraftVersion(
            @PathVariable UUID id,
            @PathVariable Integer versionNumber
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        DraftListingVersionDto version = draftListingService.getDraftVersion(tenantId, id, versionNumber);
        return ResponseEntity.ok(version);
    }

    /**
     * Edição do rascunho pelo lojista, gerando uma nova versão (status REVIEWED).
     */
    @PutMapping("/{id}/edit")
    public ResponseEntity<DraftListingDetailDto> editDraft(
            @PathVariable UUID id,
            @Valid @RequestBody DraftEditRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        DraftListingDetailDto updated = draftListingService.editDraft(tenantId, id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Aprovação formal do rascunho pelo lojista (status APPROVED).
     * Nenhuma publicação externa é executada.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<DraftListingDetailDto> approveDraft(
            @PathVariable UUID id,
            @RequestBody(required = false) DraftApproveRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        DraftListingDetailDto approved = draftListingService.approveDraft(tenantId, id, request);
        return ResponseEntity.ok(approved);
    }

    /**
     * Rejeição formal do rascunho pelo lojista (status REJECTED).
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<DraftListingDetailDto> rejectDraft(
            @PathVariable UUID id,
            @Valid @RequestBody DraftRejectRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        DraftListingDetailDto rejected = draftListingService.rejectDraft(tenantId, id, request);
        return ResponseEntity.ok(rejected);
    }

    /**
     * Consulta regras oficiais e limites de caracteres homologados por marketplace.
     */
    @GetMapping("/rules")
    public ResponseEntity<List<MarketplaceRuleLimitsDto>> listMarketplaceRules() {
        List<MarketplaceRuleLimitsDto> rules = draftListingService.listMarketplaceRules();
        return ResponseEntity.ok(rules);
    }
}
