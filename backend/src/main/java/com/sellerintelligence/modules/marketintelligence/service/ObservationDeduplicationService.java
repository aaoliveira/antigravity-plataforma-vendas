package com.sellerintelligence.modules.marketintelligence.service;

import com.sellerintelligence.modules.marketintelligence.MarketObservation;
import com.sellerintelligence.modules.marketintelligence.MarketObservationRepository;
import com.sellerintelligence.modules.marketintelligence.model.CreateObservationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de deduplicação e reconciliação temporal de observações de mercado.
 * Evita duplicação inútil de registros para o mesmo anúncio/marketplace dentro da janela de validade,
 * preservando histórico temporal e atualizando métricas de forma atômica.
 */
@Service
public class ObservationDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(ObservationDeduplicationService.class);

    private final MarketObservationRepository observationRepository;
    private final MarketObservationSanitizer sanitizer;

    public ObservationDeduplicationService(
            MarketObservationRepository observationRepository,
            MarketObservationSanitizer sanitizer
    ) {
        this.observationRepository = observationRepository;
        this.sanitizer = sanitizer;
    }

    @Transactional
    public MarketObservation processObservation(UUID tenantId, CreateObservationRequest request) {
        Instant now = Instant.now();
        Instant collectedAt = (request.collectedAt() != null) ? request.collectedAt() : now;
        Instant observedAt = (request.observedAt() != null) ? request.observedAt() : now;
        Instant validUntil = (request.validUntil() != null) ? request.validUntil() : now.plus(7, ChronoUnit.DAYS);

        String sanitizedUrl = sanitizer.sanitizeSourceUrl(request.sourceUrl());
        String minimizedPayload = sanitizer.minimizePayload(request.rawPayload());

        BigDecimal confidence = (request.confidence() != null) ? request.confidence() : BigDecimal.ONE;

        Optional<MarketObservation> existingOpt = observationRepository
                .findByTenantIdAndMarketplaceAndObservedListingId(tenantId, request.marketplace(), request.observedListingId());

        if (existingOpt.isPresent()) {
            MarketObservation existing = existingOpt.get();
            log.info("Deduplicação acionada para observação existente: id={}, mkt={}, listing={}",
                    existing.getId(), existing.getMarketplace(), existing.getObservedListingId());

            existing.updateObservation(
                    request.title(),
                    request.price(),
                    request.itemCondition(),
                    request.sellerIdentifier(),
                    observedAt,
                    collectedAt,
                    validUntil,
                    confidence,
                    request.gtin(),
                    request.brand(),
                    request.salesVolume(),
                    request.visitsCount(),
                    request.ctr(),
                    request.revenue(),
                    minimizedPayload
            );

            return observationRepository.save(existing);
        }

        MarketObservation newObservation = new MarketObservation(
                UUID.randomUUID(),
                tenantId,
                request.observedListingId(),
                request.marketplace(),
                request.title(),
                request.price(),
                request.currency(),
                request.itemCondition(),
                request.sellerIdentifier(),
                request.sourceType(),
                sanitizedUrl,
                collectedAt,
                observedAt,
                validUntil,
                request.method(),
                confidence,
                request.methodologyDescription(),
                request.modelVersion(),
                request.timeWindow(),
                request.gtin(),
                request.brand(),
                request.salesVolume(),
                request.visitsCount(),
                request.ctr(),
                request.revenue(),
                minimizedPayload
        );

        return observationRepository.save(newObservation);
    }
}
