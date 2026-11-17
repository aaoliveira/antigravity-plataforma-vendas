package br.com.sellerintelligence.financial.dashboard;

import br.com.sellerintelligence.financial.domain.*;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.sync.domain.Order;
import br.com.sellerintelligence.sync.domain.OrderRepository;
import br.com.sellerintelligence.tenancy.domain.Role;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FinancialDashboardService {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2, ROUNDING);
    private static final BigDecimal ZERO_PERCENT = BigDecimal.ZERO.setScale(4, ROUNDING);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100").setScale(2, ROUNDING);

    private final OrderFinancialCalculationRepository calculationRepository;
    private final OrderItemFinancialCalculationRepository itemCalculationRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final FinancialCsvExporter csvExporter;

    public FinancialDashboardService(
            OrderFinancialCalculationRepository calculationRepository,
            OrderItemFinancialCalculationRepository itemCalculationRepository,
            OrderRepository orderRepository,
            StoreRepository storeRepository,
            FinancialCsvExporter csvExporter
    ) {
        this.calculationRepository = calculationRepository;
        this.itemCalculationRepository = itemCalculationRepository;
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.csvExporter = csvExporter;
    }

    public DashboardOverviewResponse getDashboardOverview(FinancialDashboardFilter filter) {
        validateFinancialAccess();
        UUID tenantId = TenantContext.getTenantId();

        Instant to = filter.to() != null ? filter.to() : Instant.now();
        Instant from = filter.from() != null ? filter.from() : to.minus(Duration.ofDays(30));

        // 1. Current Period Calculations
        List<OrderFinancialCalculation> currentCalcs = calculationRepository.findCalculationsByPeriod(
                tenantId, from, to, filter.storeId(), filter.marketplace()
        );
        List<OrderItemFinancialCalculation> currentItems = itemCalculationRepository.findItemCalculationsByPeriod(
                tenantId, from, to, filter.storeId(), filter.marketplace()
        );

        // Fetch orders and stores for metadata
        List<UUID> orderIds = currentCalcs.stream().map(OrderFinancialCalculation::getOrderId).toList();
        Map<UUID, Order> ordersMap = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        Map<UUID, String> storeNamesMap = storeRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Store::getId, Store::getInternalName));

        Instant lastSyncedAt = ordersMap.values().stream()
                .map(Order::getOriginUpdatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(Instant.now());

        FinancialSummaryDto currentSummary = aggregateSummary(currentCalcs, lastSyncedAt);

        // 2. Period Comparison (if requested)
        PeriodComparisonDto comparison = null;
        if (filter.compareWithPrevious()) {
            Duration duration = Duration.between(from, to);
            Instant prevTo = from;
            Instant prevFrom = from.minus(duration);

            List<OrderFinancialCalculation> prevCalcs = calculationRepository.findCalculationsByPeriod(
                    tenantId, prevFrom, prevTo, filter.storeId(), filter.marketplace()
            );
            FinancialSummaryDto prevSummary = aggregateSummary(prevCalcs, null);

            BigDecimal revDelta = calculateDelta(currentSummary.grossRevenue(), prevSummary.grossRevenue());
            BigDecimal ordersDelta = calculateDelta(
                    BigDecimal.valueOf(currentSummary.orderCount()),
                    BigDecimal.valueOf(prevSummary.orderCount())
            );
            BigDecimal marginDelta = calculateDelta(currentSummary.contributionMarginValue(), prevSummary.contributionMarginValue());
            boolean isPrevIncomplete = prevSummary.partialCalculationsCount() > 0;

            comparison = new PeriodComparisonDto(currentSummary, prevSummary, revDelta, ordersDelta, marginDelta, isPrevIncomplete);
        }

        // 3. Product Profitability Breakdown
        List<ProductProfitabilityDto> byProduct = aggregateByProduct(currentItems);

        // 4. Store Profitability Breakdown
        List<StoreProfitabilityDto> byStore = aggregateByStore(currentCalcs, ordersMap, storeNamesMap);

        String notice = currentSummary.partialCalculationsCount() > 0
                ? String.format("Atenção: %d pedido(s) contêm custos ou impostos não informados (cálculo parcial).", currentSummary.partialCalculationsCount())
                : "100% dos pedidos calculados com todos os componentes cadastrados.";

        return new DashboardOverviewResponse(
                currentSummary,
                comparison,
                byProduct,
                byStore,
                DataProvenance.IMPORTED,
                notice
        );
    }

    public List<FinancialOrderListItemDto> getOrderList(FinancialDashboardFilter filter) {
        validateFinancialAccess();
        UUID tenantId = TenantContext.getTenantId();

        Instant to = filter.to() != null ? filter.to() : Instant.now();
        Instant from = filter.from() != null ? filter.from() : to.minus(Duration.ofDays(30));

        List<OrderFinancialCalculation> calcs = calculationRepository.findCalculationsByPeriod(
                tenantId, from, to, filter.storeId(), filter.marketplace()
        );

        List<UUID> orderIds = calcs.stream().map(OrderFinancialCalculation::getOrderId).toList();
        Map<UUID, Order> ordersMap = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        Map<UUID, String> storeNamesMap = storeRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Store::getId, Store::getInternalName));

        return calcs.stream().map(c -> {
            Order o = ordersMap.get(c.getOrderId());
            String storeName = o != null ? storeNamesMap.getOrDefault(o.getStoreId(), "Loja") : "Loja";
            return new FinancialOrderListItemDto(
                    c.getOrderId(),
                    o != null ? o.getExternalOrderId() : c.getOrderId().toString(),
                    o != null ? o.getOrderDate() : c.getCalculatedAt(),
                    o != null ? o.getMarketplace() : null,
                    storeName,
                    c.getItems() != null ? c.getItems().size() : 1,
                    c.getGrossRevenue(),
                    c.getContributionMarginValue(),
                    c.getContributionMarginPercentage(),
                    c.getCalculationQuality(),
                    c.getCalculationStatus()
            );
        }).toList();
    }

    public String exportOrdersCsv(FinancialDashboardFilter filter, ZoneId zoneId) {
        validateFinancialAccess();
        UUID tenantId = TenantContext.getTenantId();

        Instant to = filter.to() != null ? filter.to() : Instant.now();
        Instant from = filter.from() != null ? filter.from() : to.minus(Duration.ofDays(30));

        List<OrderFinancialCalculation> calcs = calculationRepository.findCalculationsByPeriod(
                tenantId, from, to, filter.storeId(), filter.marketplace()
        );

        List<UUID> orderIds = calcs.stream().map(OrderFinancialCalculation::getOrderId).toList();
        Map<UUID, Order> ordersMap = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        Map<UUID, String> storeNamesMap = storeRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Store::getId, Store::getInternalName));

        return csvExporter.generateCsv(tenantId, filter, zoneId, calcs, ordersMap, storeNamesMap);
    }

    private FinancialSummaryDto aggregateSummary(List<OrderFinancialCalculation> calcs, Instant lastSyncedAt) {
        long orderCount = calcs.size();
        BigDecimal grossRevenue = ZERO_MONEY;
        BigDecimal netRevenue = ZERO_MONEY;
        BigDecimal totalCogs = ZERO_MONEY;
        BigDecimal totalFees = ZERO_MONEY;
        BigDecimal totalShipping = ZERO_MONEY;
        BigDecimal totalTaxes = ZERO_MONEY;
        BigDecimal totalPackaging = ZERO_MONEY;
        BigDecimal totalOther = ZERO_MONEY;
        BigDecimal totalMargin = ZERO_MONEY;
        long completedCount = 0;
        long partialCount = 0;

        for (OrderFinancialCalculation c : calcs) {
            grossRevenue = grossRevenue.add(c.getGrossRevenue());
            netRevenue = netRevenue.add(c.getNetRevenue());
            totalCogs = totalCogs.add(c.getTotalCogs());
            totalFees = totalFees.add(c.getTotalMarketplaceFees());
            totalShipping = totalShipping.add(c.getTotalShippingSeller());
            totalTaxes = totalTaxes.add(c.getTotalTaxes());
            totalPackaging = totalPackaging.add(c.getTotalPackaging());
            totalOther = totalOther.add(c.getTotalOtherCosts());
            totalMargin = totalMargin.add(c.getContributionMarginValue());

            if (c.getCalculationQuality() == CalculationQuality.COMPLETE) {
                completedCount++;
            } else {
                partialCount++;
            }
        }

        BigDecimal avgTicket = orderCount > 0 ?
                grossRevenue.divide(BigDecimal.valueOf(orderCount), 2, ROUNDING) : ZERO_MONEY;

        BigDecimal marginPct = ZERO_PERCENT;
        if (grossRevenue.compareTo(BigDecimal.ZERO) > 0) {
            marginPct = totalMargin.divide(grossRevenue, 4, ROUNDING).multiply(ONE_HUNDRED).setScale(4, ROUNDING);
        }

        BigDecimal coveragePct = orderCount > 0 ?
                BigDecimal.valueOf(completedCount).divide(BigDecimal.valueOf(orderCount), 4, ROUNDING).multiply(ONE_HUNDRED).setScale(2, ROUNDING) :
                ONE_HUNDRED;

        return new FinancialSummaryDto(
                grossRevenue.setScale(2, ROUNDING),
                netRevenue.setScale(2, ROUNDING),
                orderCount,
                avgTicket,
                totalCogs.setScale(2, ROUNDING),
                totalFees.setScale(2, ROUNDING),
                totalShipping.setScale(2, ROUNDING),
                totalTaxes.setScale(2, ROUNDING),
                totalPackaging.setScale(2, ROUNDING),
                totalOther.setScale(2, ROUNDING),
                totalMargin.setScale(2, ROUNDING),
                marginPct,
                completedCount,
                partialCount,
                coveragePct,
                lastSyncedAt
        );
    }

    private List<ProductProfitabilityDto> aggregateByProduct(List<OrderItemFinancialCalculation> items) {
        Map<String, List<OrderItemFinancialCalculation>> bySku = items.stream()
                .filter(i -> i.getSku() != null && !i.getSku().isBlank())
                .collect(Collectors.groupingBy(OrderItemFinancialCalculation::getSku));

        List<ProductProfitabilityDto> result = new ArrayList<>();
        for (Map.Entry<String, List<OrderItemFinancialCalculation>> entry : bySku.entrySet()) {
            String sku = entry.getKey();
            List<OrderItemFinancialCalculation> skuItems = entry.getValue();

            long totalQty = skuItems.stream().mapToLong(OrderItemFinancialCalculation::getQuantity).sum();
            BigDecimal totalGross = skuItems.stream()
                    .map(OrderItemFinancialCalculation::getTotalPrice)
                    .reduce(ZERO_MONEY, BigDecimal::add);
            BigDecimal totalCogs = skuItems.stream()
                    .map(OrderItemFinancialCalculation::getTotalCogs)
                    .reduce(ZERO_MONEY, BigDecimal::add);
            BigDecimal totalMargin = skuItems.stream()
                    .map(OrderItemFinancialCalculation::getContributionMarginValue)
                    .reduce(ZERO_MONEY, BigDecimal::add);

            BigDecimal marginPct = totalGross.compareTo(BigDecimal.ZERO) > 0 ?
                    totalMargin.divide(totalGross, 4, ROUNDING).multiply(ONE_HUNDRED).setScale(4, ROUNDING) : ZERO_PERCENT;

            boolean anyPartial = skuItems.stream().anyMatch(i -> i.getCalculationQuality() == CalculationQuality.PARTIAL);
            CalculationQuality quality = anyPartial ? CalculationQuality.PARTIAL : CalculationQuality.COMPLETE;

            result.add(new ProductProfitabilityDto(
                    sku,
                    sku,
                    totalQty,
                    totalGross.setScale(2, ROUNDING),
                    totalCogs.setScale(2, ROUNDING),
                    totalMargin.setScale(2, ROUNDING),
                    marginPct,
                    quality
            ));
        }

        result.sort(Comparator.comparing(ProductProfitabilityDto::grossRevenue).reversed());
        return result;
    }

    private List<StoreProfitabilityDto> aggregateByStore(
            List<OrderFinancialCalculation> calcs,
            Map<UUID, Order> ordersMap,
            Map<UUID, String> storeNamesMap
    ) {
        Map<UUID, List<OrderFinancialCalculation>> byStoreId = calcs.stream()
                .filter(c -> ordersMap.containsKey(c.getOrderId()) && ordersMap.get(c.getOrderId()).getStoreId() != null)
                .collect(Collectors.groupingBy(c -> ordersMap.get(c.getOrderId()).getStoreId()));

        List<StoreProfitabilityDto> result = new ArrayList<>();
        for (Map.Entry<UUID, List<OrderFinancialCalculation>> entry : byStoreId.entrySet()) {
            UUID storeId = entry.getKey();
            List<OrderFinancialCalculation> storeCalcs = entry.getValue();
            String storeName = storeNamesMap.getOrDefault(storeId, "Loja " + storeId.toString().substring(0, 8));
            Order sampleOrder = ordersMap.get(storeCalcs.get(0).getOrderId());

            BigDecimal gross = storeCalcs.stream()
                    .map(OrderFinancialCalculation::getGrossRevenue)
                    .reduce(ZERO_MONEY, BigDecimal::add);
            BigDecimal margin = storeCalcs.stream()
                    .map(OrderFinancialCalculation::getContributionMarginValue)
                    .reduce(ZERO_MONEY, BigDecimal::add);

            BigDecimal marginPct = gross.compareTo(BigDecimal.ZERO) > 0 ?
                    margin.divide(gross, 4, ROUNDING).multiply(ONE_HUNDRED).setScale(4, ROUNDING) : ZERO_PERCENT;

            result.add(new StoreProfitabilityDto(
                    storeId,
                    storeName,
                    sampleOrder != null ? sampleOrder.getMarketplace() : null,
                    storeCalcs.size(),
                    gross.setScale(2, ROUNDING),
                    margin.setScale(2, ROUNDING),
                    marginPct
            ));
        }

        result.sort(Comparator.comparing(StoreProfitabilityDto::grossRevenue).reversed());
        return result;
    }

    private BigDecimal calculateDelta(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? ONE_HUNDRED : ZERO_PERCENT;
        }
        return current.subtract(previous)
                .divide(previous.abs(), 4, ROUNDING)
                .multiply(ONE_HUNDRED)
                .setScale(2, ROUNDING);
    }

    private void validateFinancialAccess() {
        Role role = TenantContext.getCurrentRole().orElse(Role.VIEWER);
        if (role == Role.OPERATOR || role == Role.VIEWER) {
            throw new BusinessException(
                    "FORBIDDEN_FINANCIAL_ACCESS",
                    "Acesso negado: seu papel de usuário (" + role + ") não possui permissão para visualizar relatórios financeiros.",
                    HttpStatus.FORBIDDEN
            );
        }
    }
}
