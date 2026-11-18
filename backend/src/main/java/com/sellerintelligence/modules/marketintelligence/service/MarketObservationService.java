package com.sellerintelligence.modules.marketintelligence.service;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.correlation.CorrelationIdFilter;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.marketintelligence.MarketObservation;
import com.sellerintelligence.modules.marketintelligence.MarketObservationRepository;
import com.sellerintelligence.modules.marketintelligence.ProductMatch;
import com.sellerintelligence.modules.marketintelligence.ProductMatchRepository;
import com.sellerintelligence.modules.marketintelligence.model.CreateObservationRequest;
import com.sellerintelligence.modules.marketintelligence.model.MarketObservationSourceType;
import com.sellerintelligence.modules.marketintelligence.model.MatchStatus;
import com.sellerintelligence.modules.marketintelligence.model.ObservationResponse;
import com.sellerintelligence.modules.marketintelligence.model.PageResponse;
import com.sellerintelligence.modules.marketintelligence.model.ProductMatchCandidateResponse;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MarketObservationService {

    private final MarketObservationRepository observationRepository;
    private final ProductMatchRepository productMatchRepository;
    private final MarketObservationValidator validator;
    private final ObservationDeduplicationService deduplicationService;
    private final ProductMatchingService matchingService;
    private final AuditPublisher auditPublisher;

    public MarketObservationService(
            MarketObservationRepository observationRepository,
            ProductMatchRepository productMatchRepository,
            MarketObservationValidator validator,
            ObservationDeduplicationService deduplicationService,
            ProductMatchingService matchingService,
            AuditPublisher auditPublisher
    ) {
        this.observationRepository = observationRepository;
        this.productMatchRepository = productMatchRepository;
        this.validator = validator;
        this.deduplicationService = deduplicationService;
        this.matchingService = matchingService;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public ObservationResponse createObservation(UUID tenantId, CreateObservationRequest request) {
        // Validações de conformidade e regras anti-alucinação
        validator.validate(request);

        // Deduplicação e persistência segura
        MarketObservation saved = deduplicationService.processObservation(tenantId, request);

        // Registro de Auditoria
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        auditPublisher.publish(
                "MARKET_OBSERVATION_INGESTED",
                null,
                null,
                tenantId,
                null,
                null,
                correlationId,
                String.format("Observação processada: id=%s, mkt=%s, listingId=%s, source=%s",
                        saved.getId(), saved.getMarketplace(), saved.getObservedListingId(), saved.getSourceType())
        );

        return ObservationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ObservationResponse getObservation(UUID tenantId, UUID id) {
        return observationRepository.findByIdAndTenantId(id, tenantId)
                .map(ObservationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Observação de mercado não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ObservationResponse> listObservations(
            UUID tenantId,
            String marketplace,
            MarketObservationSourceType sourceType,
            String gtin,
            Instant startDate,
            Instant endDate,
            String search,
            Pageable pageable
    ) {
        Page<MarketObservation> page = observationRepository.findWithFilters(
                tenantId,
                (marketplace != null && !marketplace.isBlank()) ? marketplace : null,
                sourceType,
                (gtin != null && !gtin.isBlank()) ? gtin : null,
                startDate,
                endDate,
                (search != null && !search.isBlank()) ? search : null,
                pageable
        );

        return PageResponse.from(page.map(ObservationResponse::from));
    }

    @Transactional
    public List<ProductMatchCandidateResponse> executeMatching(
            UUID tenantId,
            UUID observationId,
            BigDecimal threshold
    ) {
        List<ProductMatchCandidateResponse> matches = matchingService.matchObservation(tenantId, observationId, threshold);

        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        auditPublisher.publish(
                "MARKET_MATCHING_EXECUTED",
                null,
                null,
                tenantId,
                null,
                null,
                correlationId,
                String.format("Matching executado para observação %s: %d candidatos avaliados", observationId, matches.size())
        );

        return matches;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductMatch> listMatches(
            UUID tenantId,
            MatchStatus status,
            Pageable pageable
    ) {
        Page<ProductMatch> page = (status != null)
                ? productMatchRepository.findAllByTenantIdAndMatchStatus(tenantId, status, pageable)
                : productMatchRepository.findAllByTenantId(tenantId, pageable);

        return PageResponse.from(page);
    }
}
