package com.sellerintelligence.modules.listinganalytics.service;

import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.catalog.MarketplaceListing;
import com.sellerintelligence.modules.catalog.MarketplaceListingRepository;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.listinganalytics.entity.AccountMetricRecord;
import com.sellerintelligence.modules.listinganalytics.model.AccountMetricDto;
import com.sellerintelligence.modules.listinganalytics.model.IngestMetricsCommand;
import com.sellerintelligence.modules.listinganalytics.model.MetricAvailabilityStatus;
import com.sellerintelligence.modules.listinganalytics.model.MetricCalculationResult;
import com.sellerintelligence.modules.listinganalytics.model.MetricCatalogEntry;
import com.sellerintelligence.modules.listinganalytics.model.MetricFormulaEngine;
import com.sellerintelligence.modules.listinganalytics.model.MetricGranularity;
import com.sellerintelligence.modules.listinganalytics.model.MetricProvenance;
import com.sellerintelligence.modules.listinganalytics.model.MetricScope;
import com.sellerintelligence.modules.listinganalytics.model.MetricUnit;
import com.sellerintelligence.modules.listinganalytics.model.MetricsSummaryDto;
import com.sellerintelligence.modules.listinganalytics.model.OfficialMetricCatalog;
import com.sellerintelligence.modules.listinganalytics.repository.AccountMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AccountMetricIngestionService {

    private static final Logger log = LoggerFactory.getLogger(AccountMetricIngestionService.class);

    private final AccountMetricRepository metricRepository;
    private final SellerAccountRepository sellerAccountRepository;
    private final MarketplaceListingRepository marketplaceListingRepository;
    private final OfficialMetricCatalog metricCatalog;
    private final MetricFormulaEngine formulaEngine;

    public AccountMetricIngestionService(
            AccountMetricRepository metricRepository,
            SellerAccountRepository sellerAccountRepository,
            MarketplaceListingRepository marketplaceListingRepository,
            OfficialMetricCatalog metricCatalog,
            MetricFormulaEngine formulaEngine
    ) {
        this.metricRepository = metricRepository;
        this.sellerAccountRepository = sellerAccountRepository;
        this.marketplaceListingRepository = marketplaceListingRepository;
        this.metricCatalog = metricCatalog;
        this.formulaEngine = formulaEngine;
    }

    /**
     * Executa a ingestão e derivação de métricas para a conta ou anúncio informado de forma idempotente.
     */
    public List<AccountMetricDto> ingestMetrics(UUID tenantId, IngestMetricsCommand command) {
        log.info("Iniciando ingestão de métricas para tenant={}, account={}, mkt={}, scope={}",
                tenantId, command.sellerAccountId(), command.marketplace(), command.metricScope());

        // 1. Validação de existência da conta e tenant
        SellerAccount account = sellerAccountRepository.findByIdAndTenantId(command.sellerAccountId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta de vendedor não encontrada: " + command.sellerAccountId()));

        // 2. Resolução do anúncio (se aplicável)
        UUID listingId = null;
        if (command.externalListingId() != null && !command.externalListingId().isBlank()) {
            Optional<MarketplaceListing> listingOpt = marketplaceListingRepository
                    .findByTenantIdAndMarketplaceAndExternalListingId(tenantId, command.marketplace(), command.externalListingId());
            if (listingOpt.isPresent()) {
                listingId = listingOpt.get().getId();
            }
        }

        String targetIdentifier = command.externalListingId() != null && !command.externalListingId().isBlank()
                ? command.externalListingId()
                : "__ACCOUNT__";

        Map<String, BigDecimal> rawMetrics = command.metrics() != null ? command.metrics() : Map.of();
        List<AccountMetricRecord> recordsToSave = new ArrayList<>();

        // 3. Processamento de Métricas Primárias conhecidas
        for (Map.Entry<String, BigDecimal> entry : rawMetrics.entrySet()) {
            String key = entry.getKey().toUpperCase();
            BigDecimal value = entry.getValue();

            Optional<MetricCatalogEntry> catalogEntryOpt = metricCatalog.findByMarketplaceAndKey(command.marketplace(), key);
            MetricCatalogEntry catalogEntry = catalogEntryOpt.orElse(null);

            MetricUnit unit = catalogEntry != null ? catalogEntry.unit() : MetricUnit.COUNT;
            String sourceEndpoint = command.sourceEndpoint() != null && !command.sourceEndpoint().isBlank()
                    ? command.sourceEndpoint()
                    : (catalogEntry != null ? catalogEntry.sourceEndpoint() : "API Oficial do Marketplace");

            boolean supported = catalogEntry == null || catalogEntry.supportedByChannel();
            MetricAvailabilityStatus status = supported
                    ? (value != null ? MetricAvailabilityStatus.AVAILABLE : MetricAvailabilityStatus.UNAVAILABLE)
                    : MetricAvailabilityStatus.UNSUPPORTED_BY_CHANNEL;

            String unavailReason = !supported && catalogEntry != null
                    ? catalogEntry.unavailabilityReason()
                    : (value == null ? "Métrica ausente na coleta da API oficial." : null);

            MetricProvenance provenance = supported && value != null
                    ? MetricProvenance.OFFICIAL_API
                    : MetricProvenance.UNAVAILABLE;

            BigDecimal sanitizedValue = supported ? value : null;

            AccountMetricRecord record = upsertMetricRecord(
                    tenantId,
                    command.sellerAccountId(),
                    command.marketplace(),
                    listingId,
                    command.externalListingId(),
                    targetIdentifier,
                    command.metricScope(),
                    command.granularity(),
                    command.windowStart(),
                    command.windowEnd(),
                    key,
                    sanitizedValue,
                    unit,
                    provenance,
                    null,
                    null,
                    null,
                    null,
                    null,
                    status,
                    unavailReason,
                    sourceEndpoint
            );
            recordsToSave.add(record);
        }

        // 4. Derivação Determinística: CTR e Conversão
        // a) CTR: clicks / impressions
        BigDecimal clicks = rawMetrics.get("CLICKS");
        BigDecimal impressions = rawMetrics.get("IMPRESSIONS");

        if (command.marketplace().equalsIgnoreCase("MERCADO_LIVRE") && impressions == null) {
            // No Mercado Livre orgânico, impressões e cliques não são disponibilizados
            AccountMetricRecord ctrRecord = upsertMetricRecord(
                    tenantId,
                    command.sellerAccountId(),
                    command.marketplace(),
                    listingId,
                    command.externalListingId(),
                    targetIdentifier,
                    command.metricScope(),
                    command.granularity(),
                    command.windowStart(),
                    command.windowEnd(),
                    "CTR",
                    null,
                    MetricUnit.PERCENTAGE,
                    MetricProvenance.UNAVAILABLE,
                    "clicks / impressions * 100",
                    "IMPRESSIONS",
                    null,
                    "CLICKS",
                    clicks,
                    MetricAvailabilityStatus.UNSUPPORTED_BY_CHANNEL,
                    "Indisponível no tráfego orgânico do Mercado Livre devido à ausência oficial de dados de impressões. Aplicável somente sob contratação de Mercado Ads.",
                    "Derivada (clicks / impressions)"
            );
            recordsToSave.add(ctrRecord);
        } else {
            MetricCalculationResult ctrResult = formulaEngine.calculateCtr(clicks, impressions);
            AccountMetricRecord ctrRecord = upsertMetricRecord(
                    tenantId,
                    command.sellerAccountId(),
                    command.marketplace(),
                    listingId,
                    command.externalListingId(),
                    targetIdentifier,
                    command.metricScope(),
                    command.granularity(),
                    command.windowStart(),
                    command.windowEnd(),
                    "CTR",
                    ctrResult.value(),
                    ctrResult.unit(),
                    ctrResult.provenance(),
                    ctrResult.formulaDefinition(),
                    ctrResult.denominatorMetric(),
                    ctrResult.denominatorValue(),
                    ctrResult.numeratorMetric(),
                    ctrResult.numeratorValue(),
                    ctrResult.status(),
                    ctrResult.unavailabilityReason(),
                    "Derivada (" + ctrResult.formulaDefinition() + ")"
            );
            recordsToSave.add(ctrRecord);
        }

        // b) Conversão de Pedidos: orders_count / visits (ou unique_visitors)
        BigDecimal visits = rawMetrics.containsKey("VISITS")
                ? rawMetrics.get("VISITS")
                : (rawMetrics.containsKey("UNIQUE_VISITORS") ? rawMetrics.get("UNIQUE_VISITORS") : rawMetrics.get("PAGE_VIEWS"));
        BigDecimal ordersCount = rawMetrics.get("ORDERS_COUNT");

        MetricCalculationResult convOrdersResult = formulaEngine.calculateConversionRateOrders(ordersCount, visits);
        AccountMetricRecord convOrdersRecord = upsertMetricRecord(
                tenantId,
                command.sellerAccountId(),
                command.marketplace(),
                listingId,
                command.externalListingId(),
                targetIdentifier,
                command.metricScope(),
                command.granularity(),
                command.windowStart(),
                command.windowEnd(),
                "CONVERSION_RATE_ORDERS",
                convOrdersResult.value(),
                convOrdersResult.unit(),
                convOrdersResult.provenance(),
                convOrdersResult.formulaDefinition(),
                convOrdersResult.denominatorMetric(),
                convOrdersResult.denominatorValue(),
                convOrdersResult.numeratorMetric(),
                convOrdersResult.numeratorValue(),
                convOrdersResult.status(),
                convOrdersResult.unavailabilityReason(),
                "Derivada (" + convOrdersResult.formulaDefinition() + ")"
        );
        recordsToSave.add(convOrdersRecord);

        // c) Conversão de Unidades: units_sold / visits
        BigDecimal unitsSold = rawMetrics.get("UNITS_SOLD");
        MetricCalculationResult convUnitsResult = formulaEngine.calculateConversionRateUnits(unitsSold, visits);
        AccountMetricRecord convUnitsRecord = upsertMetricRecord(
                tenantId,
                command.sellerAccountId(),
                command.marketplace(),
                listingId,
                command.externalListingId(),
                targetIdentifier,
                command.metricScope(),
                command.granularity(),
                command.windowStart(),
                command.windowEnd(),
                "CONVERSION_RATE_UNITS",
                convUnitsResult.value(),
                convUnitsResult.unit(),
                convUnitsResult.provenance(),
                convUnitsResult.formulaDefinition(),
                convUnitsResult.denominatorMetric(),
                convUnitsResult.denominatorValue(),
                convUnitsResult.numeratorMetric(),
                convUnitsResult.numeratorValue(),
                convUnitsResult.status(),
                convUnitsResult.unavailabilityReason(),
                "Derivada (" + convUnitsResult.formulaDefinition() + ")"
        );
        recordsToSave.add(convUnitsRecord);

        return recordsToSave.stream().map(this::toDto).toList();
    }

    private AccountMetricRecord upsertMetricRecord(
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace,
            UUID listingId,
            String externalListingId,
            String targetIdentifier,
            MetricScope scope,
            MetricGranularity granularity,
            Instant windowStart,
            Instant windowEnd,
            String metricKey,
            BigDecimal value,
            MetricUnit unit,
            MetricProvenance provenance,
            String formulaDef,
            String denMetric,
            BigDecimal denValue,
            String numMetric,
            BigDecimal numValue,
            MetricAvailabilityStatus status,
            String unavailReason,
            String sourceEndpoint
    ) {
        Optional<AccountMetricRecord> existingOpt = metricRepository
                .findByTenantIdAndSellerAccountIdAndMarketplaceAndTargetIdentifierAndMetricKeyAndGranularityAndWindowStartAndWindowEnd(
                        tenantId,
                        sellerAccountId,
                        marketplace,
                        targetIdentifier,
                        metricKey,
                        granularity,
                        windowStart,
                        windowEnd
                );

        AccountMetricRecord record;
        Instant now = Instant.now();

        if (existingOpt.isPresent()) {
            record = existingOpt.get();
            record.setListingId(listingId);
            record.setMetricValue(value);
            record.setProvenance(provenance);
            record.setAvailabilityStatus(status);
            record.setUnavailabilityReason(unavailReason);
            record.setSourceEndpoint(sourceEndpoint);
            record.setIngestedAt(now);
            record.setUpdatedAt(now);
        } else {
            record = new AccountMetricRecord(
                    UUID.randomUUID(),
                    tenantId,
                    sellerAccountId,
                    marketplace,
                    listingId,
                    externalListingId,
                    targetIdentifier,
                    scope,
                    granularity,
                    windowStart,
                    windowEnd,
                    metricKey,
                    value,
                    unit,
                    provenance,
                    formulaDef,
                    denMetric,
                    denValue,
                    numMetric,
                    numValue,
                    status,
                    unavailReason,
                    sourceEndpoint,
                    now,
                    null,
                    now,
                    now
            );
        }

        return metricRepository.save(record);
    }

    @Transactional(readOnly = true)
    public Page<AccountMetricDto> searchMetrics(
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace,
            String externalListingId,
            MetricScope scope,
            String metricKey,
            MetricAvailabilityStatus status,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        return metricRepository.searchMetrics(
                tenantId,
                sellerAccountId,
                marketplace,
                externalListingId,
                scope,
                metricKey,
                status,
                startDate,
                endDate,
                pageable
        ).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public MetricsSummaryDto getSummary(
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace,
            Instant startDate,
            Instant endDate
    ) {
        List<AccountMetricRecord> metrics = metricRepository.findForSummary(
                tenantId,
                sellerAccountId,
                marketplace,
                startDate,
                endDate
        );

        long totalCount = metrics.size();
        long availableCount = 0;
        long unavailableCount = 0;
        long insufficientDataCount = 0;
        long unsupportedCount = 0;

        BigDecimal totalVisits = BigDecimal.ZERO;
        BigDecimal totalOrders = BigDecimal.ZERO;
        BigDecimal totalUnits = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalClicks = BigDecimal.ZERO;
        BigDecimal totalImpressions = BigDecimal.ZERO;

        for (AccountMetricRecord m : metrics) {
            switch (m.getAvailabilityStatus()) {
                case AVAILABLE -> availableCount++;
                case UNAVAILABLE -> unavailableCount++;
                case INSUFFICIENT_DATA -> insufficientDataCount++;
                case UNSUPPORTED_BY_CHANNEL -> unsupportedCount++;
            }

            if (m.getAvailabilityStatus() == MetricAvailabilityStatus.AVAILABLE && m.getMetricValue() != null) {
                switch (m.getMetricKey()) {
                    case "VISITS", "PAGE_VIEWS", "UNIQUE_VISITORS" -> totalVisits = totalVisits.add(m.getMetricValue());
                    case "ORDERS_COUNT" -> totalOrders = totalOrders.add(m.getMetricValue());
                    case "UNITS_SOLD" -> totalUnits = totalUnits.add(m.getMetricValue());
                    case "REVENUE" -> totalRevenue = totalRevenue.add(m.getMetricValue());
                    case "CLICKS" -> totalClicks = totalClicks.add(m.getMetricValue());
                    case "IMPRESSIONS" -> totalImpressions = totalImpressions.add(m.getMetricValue());
                }
            }
        }

        MetricCalculationResult overallConversion = formulaEngine.calculateConversionRateOrders(totalOrders, totalVisits);
        MetricCalculationResult overallCtr = formulaEngine.calculateCtr(totalClicks, totalImpressions);

        String antiHallucinationNotice = "Métricas oficiais sincronizadas conforme documentação homologada dos canais. " +
                "Estimativas silenciosas, scrapers não licenciados e números de concorrentes não autorizados são expressamente proibidos e excluídos.";

        return new MetricsSummaryDto(
                totalCount,
                availableCount,
                unavailableCount,
                insufficientDataCount,
                unsupportedCount,
                totalVisits,
                totalOrders,
                totalUnits,
                totalRevenue,
                totalClicks,
                totalImpressions,
                overallConversion.value(),
                overallCtr.value(),
                antiHallucinationNotice
        );
    }

    public AccountMetricDto toDto(AccountMetricRecord r) {
        return new AccountMetricDto(
                r.getId(),
                r.getTenantId(),
                r.getSellerAccountId(),
                r.getMarketplace(),
                r.getListingId(),
                r.getExternalListingId(),
                r.getTargetIdentifier(),
                r.getMetricScope(),
                r.getGranularity(),
                r.getWindowStart(),
                r.getWindowEnd(),
                r.getMetricKey(),
                r.getMetricValue(),
                r.getMetricUnit(),
                r.getProvenance(),
                r.getFormulaDefinition(),
                r.getDenominatorMetric(),
                r.getDenominatorValue(),
                r.getNumeratorMetric(),
                r.getNumeratorValue(),
                r.getAvailabilityStatus(),
                r.getUnavailabilityReason(),
                r.getSourceEndpoint(),
                r.getIngestedAt(),
                r.getMetadataJson()
        );
    }
}
