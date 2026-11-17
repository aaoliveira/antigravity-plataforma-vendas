package br.com.sellerintelligence.ai.web;

import br.com.sellerintelligence.ai.dto.*;
import br.com.sellerintelligence.ai.service.ListingStudioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/listings")
public class ListingStudioController {

    private final ListingStudioService listingStudioService;

    public ListingStudioController(ListingStudioService listingStudioService) {
        this.listingStudioService = listingStudioService;
    }

    @PostMapping("/{listingId}/audit")
    public ResponseEntity<QualityAuditResultDto> auditListing(@PathVariable UUID listingId) {
        QualityAuditResultDto audit = listingStudioService.auditListing(listingId);
        return ResponseEntity.ok(audit);
    }

    @GetMapping("/{listingId}/audit")
    public ResponseEntity<QualityAuditResultDto> getLatestAudit(@PathVariable UUID listingId) {
        QualityAuditResultDto audit = listingStudioService.getLatestAudit(listingId);
        return ResponseEntity.ok(audit);
    }

    @PostMapping("/{listingId}/generate")
    public ResponseEntity<ListingGenerationResponse> generateContent(
            @PathVariable UUID listingId,
            @RequestBody(required = false) ListingGenerationRequest request
    ) {
        ListingGenerationRequest req = request != null ? request : new ListingGenerationRequest("TITLE", List.of(), null);
        ListingGenerationResponse response = listingStudioService.generateContent(listingId, req);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{listingId}/versions")
    public ResponseEntity<List<ListingVersionDto>> listVersions(@PathVariable UUID listingId) {
        List<ListingVersionDto> versions = listingStudioService.listVersions(listingId);
        return ResponseEntity.ok(versions);
    }

    @PostMapping("/{listingId}/versions")
    public ResponseEntity<ListingVersionDto> saveVersion(
            @PathVariable UUID listingId,
            @RequestBody SaveListingVersionRequest request
    ) {
        ListingVersionDto saved = listingStudioService.saveVersion(listingId, request);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{listingId}/versions/{versionId}/accept")
    public ResponseEntity<ListingVersionDto> acceptVersion(
            @PathVariable UUID listingId,
            @PathVariable UUID versionId
    ) {
        ListingVersionDto accepted = listingStudioService.acceptVersion(listingId, versionId);
        return ResponseEntity.ok(accepted);
    }
}
