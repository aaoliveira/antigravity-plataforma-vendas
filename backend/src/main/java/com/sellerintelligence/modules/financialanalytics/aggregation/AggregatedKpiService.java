package com.sellerintelligence.modules.financialanalytics.aggregation;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.company.repository.CompanyRepository;
import com.sellerintelligence.modules.financialanalytics.entity.OrderFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.model.ReconciliationStatus;
import com.sellerintelligence.modules.financialanalytics.repository.OrderFinancialFactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * Serviço de consolidação e agregação de KPIs analíticos e financeiros com cache tenant-aware.
 * Aplica precisão bancária determinística com BigDecimal (escala 4, HALF_EVEN), cumpre a regra
 * anti-alucinação de transparência de proveniência e isola estritamente o cache por organização.
 */
@Service
@Transactional(readOnly = true)
public class AggregatedKpiService {

    private static final Logger log = LoggerFactory.getLogger(AggregatedKpiService.class);
    private static final DateTimeFormatter ISO_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter ISO_MONTH = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC);

    private final OrderFinancialFactRepository orderFinancialFactRepository;
    private final CompanyRepository companyRepository;
    private final TenantAwareKpiCacheService cacheService;

    public AggregatedKpiService(
            OrderFinancialFactRepository orderFinancialFactRepository,
            CompanyRepository companyRepository,
            TenantAwareKpiCacheService cacheService
    ) {
        this.orderFinancialFactRepository = orderFinancialFactRepository;
        this.companyRepository = companyRepository;
        this.cacheService = cacheService;
    }

    /**
     * Resumo consolidado de KPIs com filtros combináveis e cache tenant-aware.
     */
    public AggregatedKpiSummaryDto getAggregatedSummary(UUID tenantId, AggregatedKpiFilter filter) {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        AggregatedKpiFilter safeFilter = filter != null ? filter : new AggregatedKpiFilter(null, null, null, null, null);

        // 1. Consulta em cache
        Optional<AggregatedKpiSummaryDto> cached = cacheService.get(
                tenantId, "summary", safeFilter.cacheKeySuffix(), AggregatedKpiSummaryDto.class
        );
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. Consulta no repositório de fatos
        List<OrderFinancialFactRecord> facts = orderFinancialFactRepository.findForAggregation(
                tenantId,
                safeFilter.companyId(),
                safeFilter.marketplace(),
                safeFilter.sellerAccountId(),
                safeFilter.startDate(),
                safeFilter.endDate()
        );

        long totalOrders = facts.size();
        BigDecimal grossRev = BigDecimal.ZERO;
        BigDecimal disc = BigDecimal.ZERO;
        BigDecimal netRev = BigDecimal.ZERO;
        BigDecimal cancel = BigDecimal.ZERO;
        BigDecimal ret = BigDecimal.ZERO;
        BigDecimal mkpFee = BigDecimal.ZERO;
        BigDecimal fixFee = BigDecimal.ZERO;
        BigDecimal ship = BigDecimal.ZERO;
        BigDecimal prodCost = BigDecimal.ZERO;
        BigDecimal packCost = BigDecimal.ZERO;
        BigDecimal addCost = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal otherExp = BigDecimal.ZERO;
        BigDecimal totalDed = BigDecimal.ZERO;
        BigDecimal contribMargin = BigDecimal.ZERO;

        long reconciled = 0;
        long estimated = 0;
        long divergent = 0;
        long missingDataCount = 0;

        for (OrderFinancialFactRecord f : facts) {
            grossRev = grossRev.add(f.getGrossRevenue());
            disc = disc.add(f.getDiscountAmount());
            netRev = netRev.add(f.getNetRevenue());
            cancel = cancel.add(f.getCancellationAmount());
            ret = ret.add(f.getReturnAmount());
            mkpFee = mkpFee.add(f.getMarketplaceFee());
            fixFee = fixFee.add(f.getFixedFee());
            ship = ship.add(f.getShippingCost());
            prodCost = prodCost.add(f.getProductCost());
            packCost = packCost.add(f.getPackagingCost());
            addCost = addCost.add(f.getAdditionalCost());
            tax = tax.add(f.getTaxAmount());
            otherExp = otherExp.add(f.getOtherExpensesAmount());
            totalDed = totalDed.add(f.getTotalDeductions());
            contribMargin = contribMargin.add(f.getContributionMarginAmount());

            if (f.getReconciliationStatus() == ReconciliationStatus.RECONCILED) reconciled++;
            else if (f.getReconciliationStatus() == ReconciliationStatus.ESTIMATED) estimated++;
            else if (f.getReconciliationStatus() == ReconciliationStatus.DIVERGENT) divergent++;

            if (f.isHasMissingData()) missingDataCount++;
        }

        BigDecimal avgMarginRatio = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
        if (netRev.compareTo(BigDecimal.ZERO) > 0) {
            avgMarginRatio = scale4(
                    contribMargin.multiply(BigDecimal.valueOf(100)).divide(netRev, 4, RoundingMode.HALF_EVEN)
            );
        }

        BigDecimal aov = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
        if (totalOrders > 0) {
            aov = scale4(
                    grossRev.divide(BigDecimal.valueOf(totalOrders), 4, RoundingMode.HALF_EVEN)
            );
        }

        BigDecimal reconciliationRate = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
        if (totalOrders > 0) {
            reconciliationRate = scale4(
                    BigDecimal.valueOf(reconciled)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(totalOrders), 4, RoundingMode.HALF_EVEN)
            );
        }

        AggregatedKpiSummaryDto result = new AggregatedKpiSummaryDto(
                tenantId,
                totalOrders,
                scale4(grossRev),
                scale4(disc),
                scale4(netRev),
                scale4(cancel),
                scale4(ret),
                scale4(mkpFee),
                scale4(fixFee),
                scale4(ship),
                scale4(prodCost),
                scale4(packCost),
                scale4(addCost),
                scale4(tax),
                scale4(otherExp),
                scale4(totalDed),
                scale4(contribMargin),
                avgMarginRatio,
                aov,
                reconciled,
                estimated,
                divergent,
                missingDataCount,
                reconciliationRate,
                false,
                Instant.now(),
                OrderFinancialFactRecord.ANTI_HALLUCINATION_DISCLAIMER,
                KpiCatalog.getAllDefinitions()
        );

        // 3. Salva no cache com flag cached=true
        AggregatedKpiSummaryDto cachedVersion = new AggregatedKpiSummaryDto(
                result.tenantId(),
                result.totalOrders(),
                result.totalGrossRevenue(),
                result.totalDiscountAmount(),
                result.totalNetRevenue(),
                result.totalCancellationAmount(),
                result.totalReturnAmount(),
                result.totalMarketplaceFees(),
                result.totalFixedFees(),
                result.totalShippingCost(),
                result.totalProductCost(),
                result.totalPackagingCost(),
                result.totalAdditionalCost(),
                result.totalTaxAmount(),
                result.totalOtherExpensesAmount(),
                result.totalDeductions(),
                result.totalContributionMarginAmount(),
                result.averageContributionMarginRatio(),
                result.averageOrderValue(),
                result.reconciledCount(),
                result.estimatedCount(),
                result.divergentCount(),
                result.ordersWithMissingDataCount(),
                result.reconciliationRatePercent(),
                true,
                result.calculatedAt(),
                result.antiHallucinationDisclaimer(),
                result.kpiDefinitions()
        );
        cacheService.put(tenantId, "summary", safeFilter.cacheKeySuffix(), cachedVersion);

        return result;
    }

    /**
     * Agregação analítica comparativa por Marketplace / Canal.
     */
    @SuppressWarnings("unchecked")
    public List<ChannelKpiAggregateDto> getKpisByChannel(UUID tenantId, AggregatedKpiFilter filter) {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        AggregatedKpiFilter safeFilter = filter != null ? filter : new AggregatedKpiFilter(null, null, null, null, null);

        // 1. Cache
        Optional<List> cached = cacheService.get(tenantId, "by-channel", safeFilter.cacheKeySuffix(), List.class);
        if (cached.isPresent()) {
            return (List<ChannelKpiAggregateDto>) cached.get();
        }

        // 2. Consulta
        List<OrderFinancialFactRecord> facts = orderFinancialFactRepository.findForAggregation(
                tenantId,
                safeFilter.companyId(),
                safeFilter.marketplace(),
                safeFilter.sellerAccountId(),
                safeFilter.startDate(),
                safeFilter.endDate()
        );

        // Agrupamento por marketplace
        Map<String, List<OrderFinancialFactRecord>> byChannel = new LinkedHashMap<>();
        BigDecimal totalOverallNetRevenue = BigDecimal.ZERO;

        for (OrderFinancialFactRecord f : facts) {
            String ch = f.getMarketplace() != null ? f.getMarketplace() : "UNKNOWN";
            byChannel.computeIfAbsent(ch, k -> new ArrayList<>()).add(f);
            totalOverallNetRevenue = totalOverallNetRevenue.add(f.getNetRevenue());
        }

        List<ChannelKpiAggregateDto> list = new ArrayList<>();
        for (var entry : byChannel.entrySet()) {
            String channel = entry.getKey();
            List<OrderFinancialFactRecord> cFacts = entry.getValue();

            long count = cFacts.size();
            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal net = BigDecimal.ZERO;
            BigDecimal ded = BigDecimal.ZERO;
            BigDecimal margin = BigDecimal.ZERO;

            for (OrderFinancialFactRecord f : cFacts) {
                gross = gross.add(f.getGrossRevenue());
                net = net.add(f.getNetRevenue());
                ded = ded.add(f.getTotalDeductions());
                margin = margin.add(f.getContributionMarginAmount());
            }

            BigDecimal ratio = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                ratio = scale4(margin.multiply(BigDecimal.valueOf(100)).divide(net, 4, RoundingMode.HALF_EVEN));
            }

            BigDecimal aov = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            if (count > 0) {
                aov = scale4(gross.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_EVEN));
            }

            BigDecimal share = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            if (totalOverallNetRevenue.compareTo(BigDecimal.ZERO) > 0) {
                share = scale4(net.multiply(BigDecimal.valueOf(100)).divide(totalOverallNetRevenue, 4, RoundingMode.HALF_EVEN));
            }

            list.add(new ChannelKpiAggregateDto(
                    channel,
                    count,
                    scale4(gross),
                    scale4(net),
                    scale4(ded),
                    scale4(margin),
                    ratio,
                    aov,
                    share
            ));
        }

        list.sort(Comparator.comparing(ChannelKpiAggregateDto::grossRevenue).reversed());
        cacheService.put(tenantId, "by-channel", safeFilter.cacheKeySuffix(), list);
        return list;
    }

    /**
     * Agregação periódica (diária, semanal, mensal) para gráficos de tendência e DRE temporal.
     */
    @SuppressWarnings("unchecked")
    public List<PeriodicKpiAggregateDto> getKpisByPeriod(
            UUID tenantId,
            AggregatedKpiFilter filter,
            KpiGranularity granularity
    ) {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        KpiGranularity safeGranularity = granularity != null ? granularity : KpiGranularity.DAILY;
        AggregatedKpiFilter safeFilter = filter != null ? filter : new AggregatedKpiFilter(null, null, null, null, null);

        String cacheKey = String.format("%s:%s", safeGranularity.name().toLowerCase(), safeFilter.cacheKeySuffix());
        Optional<List> cached = cacheService.get(tenantId, "by-period", cacheKey, List.class);
        if (cached.isPresent()) {
            return (List<PeriodicKpiAggregateDto>) cached.get();
        }

        List<OrderFinancialFactRecord> facts = orderFinancialFactRepository.findForAggregation(
                tenantId,
                safeFilter.companyId(),
                safeFilter.marketplace(),
                safeFilter.sellerAccountId(),
                safeFilter.startDate(),
                safeFilter.endDate()
        );

        Map<String, List<OrderFinancialFactRecord>> byPeriod = new TreeMap<>();
        for (OrderFinancialFactRecord f : facts) {
            String pKey = resolvePeriodKey(f.getOrderDate(), safeGranularity);
            byPeriod.computeIfAbsent(pKey, k -> new ArrayList<>()).add(f);
        }

        List<PeriodicKpiAggregateDto> list = new ArrayList<>();
        for (var entry : byPeriod.entrySet()) {
            String pKey = entry.getKey();
            List<OrderFinancialFactRecord> pFacts = entry.getValue();

            long count = pFacts.size();
            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal net = BigDecimal.ZERO;
            BigDecimal ded = BigDecimal.ZERO;
            BigDecimal margin = BigDecimal.ZERO;

            Instant start = pFacts.get(0).getOrderDate();
            Instant end = pFacts.get(pFacts.size() - 1).getOrderDate();

            for (OrderFinancialFactRecord f : pFacts) {
                gross = gross.add(f.getGrossRevenue());
                net = net.add(f.getNetRevenue());
                ded = ded.add(f.getTotalDeductions());
                margin = margin.add(f.getContributionMarginAmount());
                if (f.getOrderDate().isBefore(start)) start = f.getOrderDate();
                if (f.getOrderDate().isAfter(end)) end = f.getOrderDate();
            }

            BigDecimal ratio = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                ratio = scale4(margin.multiply(BigDecimal.valueOf(100)).divide(net, 4, RoundingMode.HALF_EVEN));
            }

            list.add(new PeriodicKpiAggregateDto(
                    pKey,
                    start,
                    end,
                    count,
                    scale4(gross),
                    scale4(net),
                    scale4(ded),
                    scale4(margin),
                    ratio
            ));
        }

        cacheService.put(tenantId, "by-period", cacheKey, list);
        return list;
    }

    /**
     * Agregação analítica consolidada por Empresa / Unidade de Negócio.
     */
    @SuppressWarnings("unchecked")
    public List<CompanyKpiAggregateDto> getKpisByCompany(UUID tenantId, AggregatedKpiFilter filter) {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        AggregatedKpiFilter safeFilter = filter != null ? filter : new AggregatedKpiFilter(null, null, null, null, null);

        Optional<List> cached = cacheService.get(tenantId, "by-company", safeFilter.cacheKeySuffix(), List.class);
        if (cached.isPresent()) {
            return (List<CompanyKpiAggregateDto>) cached.get();
        }

        List<OrderFinancialFactRecord> facts = orderFinancialFactRepository.findForAggregation(
                tenantId,
                safeFilter.companyId(),
                safeFilter.marketplace(),
                safeFilter.sellerAccountId(),
                safeFilter.startDate(),
                safeFilter.endDate()
        );

        Map<UUID, List<OrderFinancialFactRecord>> byCompany = new LinkedHashMap<>();
        BigDecimal totalOverallNetRevenue = BigDecimal.ZERO;

        for (OrderFinancialFactRecord f : facts) {
            UUID cId = f.getCompanyId();
            byCompany.computeIfAbsent(cId, k -> new ArrayList<>()).add(f);
            totalOverallNetRevenue = totalOverallNetRevenue.add(f.getNetRevenue());
        }

        List<CompanyKpiAggregateDto> list = new ArrayList<>();
        for (var entry : byCompany.entrySet()) {
            UUID cId = entry.getKey();
            List<OrderFinancialFactRecord> cFacts = entry.getValue();

            String name = "Empresa Não Vinculada";
            if (cId != null) {
                name = companyRepository.findByIdAndTenantId(cId, tenantId)
                        .map(c -> c.getCorporateName() != null ? c.getCorporateName() : c.getTradeName())
                        .orElse("Empresa " + cId.toString().substring(0, 8));
            }

            long count = cFacts.size();
            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal net = BigDecimal.ZERO;
            BigDecimal margin = BigDecimal.ZERO;

            for (OrderFinancialFactRecord f : cFacts) {
                gross = gross.add(f.getGrossRevenue());
                net = net.add(f.getNetRevenue());
                margin = margin.add(f.getContributionMarginAmount());
            }

            BigDecimal ratio = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                ratio = scale4(margin.multiply(BigDecimal.valueOf(100)).divide(net, 4, RoundingMode.HALF_EVEN));
            }

            BigDecimal share = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            if (totalOverallNetRevenue.compareTo(BigDecimal.ZERO) > 0) {
                share = scale4(net.multiply(BigDecimal.valueOf(100)).divide(totalOverallNetRevenue, 4, RoundingMode.HALF_EVEN));
            }

            list.add(new CompanyKpiAggregateDto(
                    cId,
                    name,
                    count,
                    scale4(gross),
                    scale4(net),
                    scale4(margin),
                    ratio,
                    share
            ));
        }

        list.sort(Comparator.comparing(CompanyKpiAggregateDto::grossRevenue).reversed());
        cacheService.put(tenantId, "by-company", safeFilter.cacheKeySuffix(), list);
        return list;
    }

    /**
     * Retorna o catálogo oficial documentado com todas as definições e fórmulas de KPIs.
     */
    public List<KpiDefinitionDto> getKpiDefinitions() {
        return KpiCatalog.getAllDefinitions();
    }

    /**
     * Invalida explicitamente todo o cache do tenant autenticado.
     */
    public long invalidateTenantCache(UUID tenantId) {
        return cacheService.invalidateTenant(tenantId);
    }

    private String resolvePeriodKey(Instant instant, KpiGranularity granularity) {
        LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
        return switch (granularity) {
            case DAILY -> date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            case WEEKLY -> {
                int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
                int year = date.get(WeekFields.ISO.weekBasedYear());
                yield String.format("%d-W%02d", year, week);
            }
            case MONTHLY -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        };
    }

    private static BigDecimal scale4(BigDecimal val) {
        return val != null ? val.setScale(4, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
    }
}
