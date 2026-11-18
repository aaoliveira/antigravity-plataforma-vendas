package com.sellerintelligence.modules.competitorresearch.comparison.service;

import com.sellerintelligence.modules.competitorresearch.comparison.entity.ListingPriceHistoryRecord;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ListingPriceHistoryResponse;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ListingPricePointDto;
import com.sellerintelligence.modules.competitorresearch.comparison.model.RecordPricePointRequest;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ShippingStatus;
import com.sellerintelligence.modules.competitorresearch.comparison.repository.ListingPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Serviço de histórico auditado de preços de anúncios.
 * Cumpre a regra mandatória: "histórico sem retroatividade inventada".
 * Apenas observações reais devidamente coletadas são persistidas e retornadas.
 */
@Service
public class ListingPriceHistoryService {

    private final ListingPriceHistoryRepository historyRepository;

    public ListingPriceHistoryService(ListingPriceHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public ListingPricePointDto recordPricePoint(UUID tenantId, RecordPricePointRequest request) {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        Objects.requireNonNull(request, "request é obrigatório");

        ListingPriceHistoryRecord entity = new ListingPriceHistoryRecord(
                UUID.randomUUID(),
                tenantId,
                request.marketplace(),
                request.externalListingId(),
                request.title(),
                request.price(),
                "BRL",
                request.shippingCost(),
                request.observedAt() != null ? request.observedAt() : Instant.now(),
                Instant.now(),
                request.sourceType(),
                request.sourceUrl()
        );

        ListingPriceHistoryRecord saved = historyRepository.save(entity);

        ShippingStatus shippingStatus = saved.getShippingCost() != null ? ShippingStatus.KNOWN : ShippingStatus.UNKNOWN;
        BigDecimal finalPriceWithShipping = shippingStatus == ShippingStatus.KNOWN 
                ? saved.getPrice().add(saved.getShippingCost()).setScale(4, RoundingMode.HALF_UP)
                : null;

        return new ListingPricePointDto(
                saved.getId(),
                saved.getPrice(),
                saved.getShippingCost(),
                shippingStatus,
                finalPriceWithShipping,
                saved.getObservedAt(),
                saved.getCollectedAt(),
                saved.getSourceType(),
                saved.getSourceUrl()
        );
    }

    @Transactional(readOnly = true)
    public ListingPriceHistoryResponse getPriceHistory(
            UUID tenantId,
            String marketplace,
            String externalListingId,
            Instant startDate,
            Instant endDate
    ) {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        Objects.requireNonNull(marketplace, "marketplace é obrigatório");
        Objects.requireNonNull(externalListingId, "externalListingId é obrigatório");

        List<ListingPriceHistoryRecord> records = historyRepository.findHistoryInWindow(
                tenantId, marketplace, externalListingId, startDate, endDate
        );

        if (records.isEmpty()) {
            return new ListingPriceHistoryResponse(
                    marketplace,
                    externalListingId,
                    "",
                    0,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0L,
                    "Nenhum histórico registrado para este anúncio.",
                    "Histórico iniciado na primeira observação coletada. Zero retroatividade inventada.",
                    List.of()
            );
        }

        List<ListingPricePointDto> points = records.stream()
                .map(r -> {
                    ShippingStatus shippingStatus = r.getShippingCost() != null ? ShippingStatus.KNOWN : ShippingStatus.UNKNOWN;
                    BigDecimal finalPriceWithShipping = shippingStatus == ShippingStatus.KNOWN 
                            ? r.getPrice().add(r.getShippingCost()).setScale(4, RoundingMode.HALF_UP)
                            : null;
                    return new ListingPricePointDto(
                            r.getId(),
                            r.getPrice(),
                            r.getShippingCost(),
                            shippingStatus,
                            finalPriceWithShipping,
                            r.getObservedAt(),
                            r.getCollectedAt(),
                            r.getSourceType(),
                            r.getSourceUrl()
                    );
                })
                .toList();

        String title = records.get(records.size() - 1).getTitle();
        int count = points.size();

        if (count == 1) {
            BigDecimal singlePrice = points.get(0).price();
            return new ListingPriceHistoryResponse(
                    marketplace,
                    externalListingId,
                    title,
                    1,
                    false,
                    singlePrice,
                    singlePrice,
                    singlePrice,
                    singlePrice,
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    0L,
                    "Apenas uma observação registrada. Tendência histórica exige ao menos dois momentos temporais distintos.",
                    "Histórico legítimo iniciado na data de registro da primeira observação. A plataforma NUNCA inventa dados retroativos passados.",
                    points
            );
        }

        BigDecimal earliestPrice = points.get(0).price();
        BigDecimal latestPrice = points.get(count - 1).price();
        BigDecimal minPrice = points.stream().map(ListingPricePointDto::price).min(Comparator.naturalOrder()).orElse(earliestPrice);
        BigDecimal maxPrice = points.stream().map(ListingPricePointDto::price).max(Comparator.naturalOrder()).orElse(earliestPrice);

        BigDecimal variationPct = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        if (earliestPrice.compareTo(BigDecimal.ZERO) > 0) {
            variationPct = latestPrice.subtract(earliestPrice)
                    .divide(earliestPrice, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        long windowDays = Duration.between(points.get(0).observedAt(), points.get(count - 1).observedAt()).toDays();

        String trend;
        if (variationPct.compareTo(BigDecimal.ZERO) > 0) {
            trend = String.format(java.util.Locale.ROOT, "Aumento de preço de +%.2f%% no período observado (%d dias).", variationPct, windowDays);
        } else if (variationPct.compareTo(BigDecimal.ZERO) < 0) {
            trend = String.format(java.util.Locale.ROOT, "Redução de preço de %.2f%% no período observado (%d dias).", variationPct, windowDays);
        } else {
            trend = String.format(java.util.Locale.ROOT, "Preço estável no período observado (%d dias).", windowDays);
        }

        String historyNotice = String.format("Histórico auditado com %d pontos reais observados no canal %s.", count, marketplace);

        return new ListingPriceHistoryResponse(
                marketplace,
                externalListingId,
                title,
                count,
                true,
                minPrice,
                maxPrice,
                latestPrice,
                earliestPrice,
                variationPct,
                windowDays,
                trend,
                historyNotice,
                points
        );
    }
}
