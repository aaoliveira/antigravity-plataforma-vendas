package com.sellerintelligence.modules.competitorresearch;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchHistoryItemDto;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchRequest;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchResponse;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchSourcePolicyDto;
import com.sellerintelligence.modules.competitorresearch.model.PageResponse;
import com.sellerintelligence.modules.competitorresearch.service.MarketProductSearchService;
import com.sellerintelligence.modules.marketplaceadapters.exception.RateLimitExceededException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para pesquisa autorizada de produtos e anúncios em conformidade com as políticas oficiais dos canais (P010-BE).
 * Fornece:
 * 1. Pesquisa com normalização, proveniência e respeito rigoroso a quotas.
 * 2. Catálogo de políticas oficiais e limitações por marketplace.
 * 3. Histórico e trilha de auditoria tenant-isolated.
 */
@RestController
@RequestMapping("/api/v1/market-search")
public class MarketSearchController {

    private final MarketProductSearchService searchService;

    public MarketSearchController(MarketProductSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<MarketSearchResponse> search(
            @Valid @ModelAttribute MarketSearchRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        MarketSearchResponse response = searchService.search(tenantId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sources")
    public ResponseEntity<List<MarketSearchSourcePolicyDto>> listSources() {
        // As políticas oficiais são públicas para todos os usuários autenticados
        List<MarketSearchSourcePolicyDto> policies = searchService.getPolicies();
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponse<MarketSearchHistoryItemDto>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        PageResponse<MarketSearchHistoryItemDto> history = searchService.getSearchHistory(
                tenantId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "executedAt"))
        );
        return ResponseEntity.ok(history);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                ex.getMessage()
        );
        problem.setTitle("Limite de Taxa Excedido");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/rate-limit-exceeded"));
        problem.setProperty("marketplace_id", ex.getMarketplaceId());
        problem.setProperty("retry_after_seconds", ex.getRetryAfter().toSeconds());
        problem.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(Math.max(1, ex.getRetryAfter().toSeconds())))
                .body(problem);
    }
}
