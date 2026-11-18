package com.sellerintelligence.modules.competitorresearch.comparison;

import com.sellerintelligence.modules.competitorresearch.comparison.model.CompareListingsRequest;
import com.sellerintelligence.modules.competitorresearch.comparison.model.FindEquivalentsRequest;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ListingComparisonResult;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ListingPriceHistoryResponse;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ListingPricePointDto;
import com.sellerintelligence.modules.competitorresearch.comparison.model.RecordPricePointRequest;
import com.sellerintelligence.modules.competitorresearch.comparison.model.SavedComparisonDto;
import com.sellerintelligence.modules.competitorresearch.comparison.service.ListingComparisonService;
import com.sellerintelligence.modules.competitorresearch.comparison.service.ListingPriceHistoryService;
import com.sellerintelligence.modules.competitorresearch.model.PageResponse;
import com.sellerintelligence.core.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para comparação de listings equivalentes,
 * cálculo de variação temporal de preços e consulta de histórico legítimo.
 */
@RestController
@RequestMapping("/api/v1/market-comparison")
public class ListingComparisonController {

    private final ListingComparisonService comparisonService;
    private final ListingPriceHistoryService priceHistoryService;

    public ListingComparisonController(
            ListingComparisonService comparisonService,
            ListingPriceHistoryService priceHistoryService
    ) {
        this.comparisonService = comparisonService;
        this.priceHistoryService = priceHistoryService;
    }

    @PostMapping("/compare")
    public ResponseEntity<ListingComparisonResult> compareListings(
            @Valid @RequestBody CompareListingsRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        ListingComparisonResult result = comparisonService.compareListings(tenantId, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/find-equivalents")
    public ResponseEntity<List<ListingComparisonResult>> findEquivalents(
            @Valid @RequestBody FindEquivalentsRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        List<ListingComparisonResult> results = comparisonService.findEquivalents(tenantId, request);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/price-history")
    public ResponseEntity<ListingPriceHistoryResponse> getPriceHistory(
            @RequestParam("marketplace") String marketplace,
            @RequestParam("externalListingId") String externalListingId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        ListingPriceHistoryResponse response = priceHistoryService.getPriceHistory(
                tenantId, marketplace, externalListingId, startDate, endDate
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/price-history")
    public ResponseEntity<ListingPricePointDto> recordPricePoint(
            @Valid @RequestBody RecordPricePointRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        ListingPricePointDto created = priceHistoryService.recordPricePoint(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/saved")
    public ResponseEntity<PageResponse<SavedComparisonDto>> listSavedComparisons(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<SavedComparisonDto> results = comparisonService.listSavedComparisons(
                tenantId, PageRequest.of(safePage, safeSize)
        );
        return ResponseEntity.ok(PageResponse.of(results));
    }

    @PostMapping("/saved")
    public ResponseEntity<SavedComparisonDto> saveComparison(
            @Valid @RequestBody ListingComparisonResult result
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        SavedComparisonDto saved = comparisonService.saveComparison(tenantId, result);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
