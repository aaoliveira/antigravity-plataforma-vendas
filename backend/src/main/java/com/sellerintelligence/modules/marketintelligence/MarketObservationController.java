package com.sellerintelligence.modules.marketintelligence;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.marketintelligence.model.CreateObservationRequest;
import com.sellerintelligence.modules.marketintelligence.model.ExecuteMatchRequest;
import com.sellerintelligence.modules.marketintelligence.model.MarketObservationSourceType;
import com.sellerintelligence.modules.marketintelligence.model.MatchStatus;
import com.sellerintelligence.modules.marketintelligence.model.ObservationResponse;
import com.sellerintelligence.modules.marketintelligence.model.PageResponse;
import com.sellerintelligence.modules.marketintelligence.model.ProductMatchCandidateResponse;
import com.sellerintelligence.modules.marketintelligence.service.MarketObservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para inteligência de mercado, observações de produtos e correspondência explicável (P009-BE).
 * Garante filtros server-side, validação de proveniência de dados e isolamento multi-tenant estrito.
 */
@RestController
@RequestMapping("/api/v1/market-intelligence")
public class MarketObservationController {

    private final MarketObservationService observationService;

    public MarketObservationController(MarketObservationService observationService) {
        this.observationService = observationService;
    }

    @PostMapping("/observations")
    public ResponseEntity<ObservationResponse> createObservation(
            @Valid @RequestBody CreateObservationRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        ObservationResponse response = observationService.createObservation(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/observations")
    public ResponseEntity<PageResponse<ObservationResponse>> listObservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) MarketObservationSourceType sourceType,
            @RequestParam(required = false) String gtin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String search
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        int safeSize = Math.min(Math.max(1, size), 100);
        int safePage = Math.max(0, page);

        PageResponse<ObservationResponse> response = observationService.listObservations(
                tenantId,
                marketplace,
                sourceType,
                gtin,
                startDate,
                endDate,
                search,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "collectedAt"))
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/observations/{id}")
    public ResponseEntity<ObservationResponse> getObservation(
            @PathVariable UUID id
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        ObservationResponse response = observationService.getObservation(tenantId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/observations/{id}/match")
    public ResponseEntity<List<ProductMatchCandidateResponse>> executeMatch(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ExecuteMatchRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        BigDecimal threshold = (request != null) ? request.threshold() : null;

        List<ProductMatchCandidateResponse> candidates = observationService.executeMatching(tenantId, id, threshold);
        return ResponseEntity.ok(candidates);
    }

    @GetMapping("/matches")
    public ResponseEntity<PageResponse<ProductMatch>> listMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) MatchStatus status
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        int safeSize = Math.min(Math.max(1, size), 100);
        int safePage = Math.max(0, page);

        PageResponse<ProductMatch> response = observationService.listMatches(
                tenantId,
                status,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "matchedAt"))
        );

        return ResponseEntity.ok(response);
    }
}
