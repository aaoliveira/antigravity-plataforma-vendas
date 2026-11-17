package br.com.sellerintelligence.financial.web;

import br.com.sellerintelligence.financial.domain.CalculationQuality;
import br.com.sellerintelligence.financial.domain.CalculationStatus;
import br.com.sellerintelligence.financial.domain.OrderFinancialCalculation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCalculationResponse(
        UUID id,
        UUID orderId,
        UUID ruleSetId,
        Integer ruleVersion,
        CalculationStatus calculationStatus,
        CalculationQuality calculationQuality,
        BigDecimal grossRevenue,
        BigDecimal netRevenue,
        BigDecimal totalCogs,
        BigDecimal totalMarketplaceFees,
        BigDecimal totalShippingSeller,
        BigDecimal totalTaxes,
        BigDecimal totalPackaging,
        BigDecimal totalOtherCosts,
        BigDecimal contributionMarginValue,
        BigDecimal contributionMarginPercentage,
        String currency,
        List<String> missingFields,
        String assumptions,
        Instant calculatedAt,
        Instant closedAt,
        Instant recalculatedAt,
        String recalculationReason,
        List<OrderItemCalculationDto> items
) {
    public record OrderItemCalculationDto(
            UUID id,
            UUID orderItemId,
            String sku,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal totalPrice,
            BigDecimal unitCogs,
            BigDecimal totalCogs,
            BigDecimal marketplaceFee,
            BigDecimal shippingSeller,
            BigDecimal taxValue,
            BigDecimal packagingCost,
            BigDecimal otherCosts,
            BigDecimal contributionMarginValue,
            BigDecimal contributionMarginPercentage,
            CalculationQuality calculationQuality,
            String memoryBreakdown
    ) {}

    public static OrderCalculationResponse fromDomain(OrderFinancialCalculation c) {
        List<String> mfs = (c.getMissingFields() != null && !c.getMissingFields().isBlank()) ?
                List.of(c.getMissingFields().split(",")) : List.of();

        List<OrderItemCalculationDto> itemDtos = c.getItems().stream()
                .map(i -> new OrderItemCalculationDto(
                        i.getId(),
                        i.getOrderItemId(),
                        i.getSku(),
                        i.getUnitPrice(),
                        i.getQuantity(),
                        i.getTotalPrice(),
                        i.getUnitCogs(),
                        i.getTotalCogs(),
                        i.getMarketplaceFee(),
                        i.getShippingSeller(),
                        i.getTaxValue(),
                        i.getPackagingCost(),
                        i.getOtherCosts(),
                        i.getContributionMarginValue(),
                        i.getContributionMarginPercentage(),
                        i.getCalculationQuality(),
                        i.getMemoryBreakdown()
                ))
                .toList();

        return new OrderCalculationResponse(
                c.getId(),
                c.getOrderId(),
                c.getRuleSetId(),
                c.getRuleVersion(),
                c.getCalculationStatus(),
                c.getCalculationQuality(),
                c.getGrossRevenue(),
                c.getNetRevenue(),
                c.getTotalCogs(),
                c.getTotalMarketplaceFees(),
                c.getTotalShippingSeller(),
                c.getTotalTaxes(),
                c.getTotalPackaging(),
                c.getTotalOtherCosts(),
                c.getContributionMarginValue(),
                c.getContributionMarginPercentage(),
                c.getCurrency(),
                mfs,
                c.getAssumptions(),
                c.getCalculatedAt(),
                c.getClosedAt(),
                c.getRecalculatedAt(),
                c.getRecalculationReason(),
                itemDtos
        );
    }
}
