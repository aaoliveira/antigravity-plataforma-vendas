package com.sellerintelligence.modules.financialanalytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.company.entity.CompanyAccountLinkRecord;
import com.sellerintelligence.modules.company.repository.CompanyAccountLinkRepository;
import com.sellerintelligence.modules.financialanalytics.entity.OrderFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.entity.OrderItemFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.model.FinancialFactSummaryDto;
import com.sellerintelligence.modules.financialanalytics.model.FinancialValueProvenance;
import com.sellerintelligence.modules.financialanalytics.model.OrderFinancialFactDto;
import com.sellerintelligence.modules.financialanalytics.model.OrderItemFinancialFactDto;
import com.sellerintelligence.modules.financialanalytics.model.ReconcileSettlementCommand;
import com.sellerintelligence.modules.financialanalytics.model.ReconciliationStatus;
import com.sellerintelligence.modules.financialanalytics.repository.OrderFinancialFactRepository;
import com.sellerintelligence.modules.financialanalytics.repository.OrderItemFinancialFactRepository;
import com.sellerintelligence.modules.orderingestion.Order;
import com.sellerintelligence.modules.orderingestion.OrderItem;
import com.sellerintelligence.modules.orderingestion.OrderItemRepository;
import com.sellerintelligence.modules.orderingestion.OrderRepository;
import com.sellerintelligence.modules.pricing.entity.ProductCostRecord;
import com.sellerintelligence.modules.pricing.model.FeeResolutionRequest;
import com.sellerintelligence.modules.pricing.model.FeeResolutionResult;
import com.sellerintelligence.modules.pricing.model.FeeType;
import com.sellerintelligence.modules.pricing.model.ResolvedFeeItem;
import com.sellerintelligence.modules.pricing.repository.ProductCostRepository;
import com.sellerintelligence.modules.pricing.service.FeeResolutionService;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço canônico de apuração de Fatos Financeiros por Pedido e Item, Reconciliação Externa e Auditoria.
 * Aplica precisão bancária determinística com BigDecimal (scale 4, HALF_EVEN), rateio proporcional com
 * preservação de soma exata, e cumprimento estrito da regra anti-alucinação vedando o rótulo de Lucro Líquido.
 */
@Service
@Transactional
public class OrderFinancialFactService {

    private static final Logger log = LoggerFactory.getLogger(OrderFinancialFactService.class);
    private static final String ENGINE_VERSION = "v1.0.0";
    private static final BigDecimal TOLERANCE_CENT = new BigDecimal("0.0100");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CompanyAccountLinkRepository companyAccountLinkRepository;
    private final ProductCostRepository productCostRepository;
    private final FeeResolutionService feeResolutionService;
    private final OrderFinancialFactRepository orderFinancialFactRepository;
    private final OrderItemFinancialFactRepository orderItemFinancialFactRepository;
    private final ObjectMapper objectMapper;

    public OrderFinancialFactService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CompanyAccountLinkRepository companyAccountLinkRepository,
            ProductCostRepository productCostRepository,
            FeeResolutionService feeResolutionService,
            OrderFinancialFactRepository orderFinancialFactRepository,
            OrderItemFinancialFactRepository orderItemFinancialFactRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.companyAccountLinkRepository = companyAccountLinkRepository;
        this.productCostRepository = productCostRepository;
        this.feeResolutionService = feeResolutionService;
        this.orderFinancialFactRepository = orderFinancialFactRepository;
        this.orderItemFinancialFactRepository = orderItemFinancialFactRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Calcula deterministamente ou reprocessa os fatos financeiros de um pedido de forma idempotente.
     */
    public OrderFinancialFactDto computeOrReprocessOrderFacts(UUID tenantId, UUID orderId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(orderId, "orderId não pode ser nulo");

        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + orderId));

        List<OrderItem> items = orderItemRepository.findByTenantIdAndOrderId(tenantId, orderId);
        if (items.isEmpty()) {
            throw new IllegalStateException("Pedido " + orderId + " não possui itens cadastrados.");
        }

        // 1. Resolve empresa vinculada à conta do vendedor
        UUID companyId = companyAccountLinkRepository
                .findActiveBySellerAccount(tenantId, order.getSellerAccountId())
                .map(CompanyAccountLinkRecord::getCompanyId)
                .orElse(null);

        // 2. Prepara listas e acumuladores
        List<String> orderMissingReasons = new ArrayList<>();
        boolean orderHasMissingData = false;
        List<Map<String, Object>> itemTraces = new ArrayList<>();

        // Soma preliminar para rateio de frete e custos do pedido
        BigDecimal totalOrderNetRevenue = BigDecimal.ZERO;
        for (OrderItem item : items) {
            BigDecimal itemGross = scale4(item.getTotalItemPrice());
            totalOrderNetRevenue = totalOrderNetRevenue.add(itemGross);
        }

        BigDecimal orderShipping = scale4(order.getShippingCost());
        BigDecimal runningApportionedShipping = BigDecimal.ZERO;

        List<ComputedItemFact> computedItems = new ArrayList<>();
        FinancialValueProvenance overallFeeProvenance = FinancialValueProvenance.ESTIMATED;
        FinancialValueProvenance overallCostProvenance = FinancialValueProvenance.USER_INFORMED;
        FinancialValueProvenance overallTaxProvenance = FinancialValueProvenance.USER_INFORMED;

        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            boolean isLastItem = (i == items.size() - 1);

            int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
            BigDecimal unitPrice = scale4(item.getUnitPrice());
            BigDecimal grossRevenue = scale4(item.getTotalItemPrice());
            BigDecimal discountAmount = scale4(BigDecimal.ZERO);
            BigDecimal netRevenue = scale4(grossRevenue.subtract(discountAmount));

            // Rateio proporcional do frete com compensação de centavos no último item
            BigDecimal itemShipping;
            if (orderShipping.compareTo(BigDecimal.ZERO) == 0) {
                itemShipping = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            } else if (isLastItem) {
                itemShipping = scale4(orderShipping.subtract(runningApportionedShipping));
            } else if (totalOrderNetRevenue.compareTo(BigDecimal.ZERO) > 0) {
                itemShipping = scale4(orderShipping.multiply(netRevenue).divide(totalOrderNetRevenue, 4, RoundingMode.HALF_EVEN));
                runningApportionedShipping = runningApportionedShipping.add(itemShipping);
            } else {
                BigDecimal equalShare = scale4(orderShipping.divide(BigDecimal.valueOf(items.size()), 4, RoundingMode.HALF_EVEN));
                itemShipping = equalShare;
                runningApportionedShipping = runningApportionedShipping.add(itemShipping);
            }

            // Resolução de Custos do Produto (ProductCostRecord)
            Optional<ProductCostRecord> costOpt = Optional.empty();
            if (item.getProductId() != null) {
                List<ProductCostRecord> candidateCosts = productCostRepository.findCandidateCosts(
                        tenantId,
                        item.getProductId(),
                        item.getProductVariantId(),
                        companyId,
                        order.getOrderCreatedAt()
                );
                if (!candidateCosts.isEmpty()) {
                    costOpt = Optional.of(candidateCosts.get(0));
                }
            }

            BigDecimal unitProductCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal productCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal packagingCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal additionalCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal taxRatePercent = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal taxAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal otherExpensesAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            FinancialValueProvenance itemCostProvenance;
            boolean itemHasMissingData = false;
            List<String> itemMissingReasons = new ArrayList<>();

            if (costOpt.isPresent()) {
                ProductCostRecord c = costOpt.get();
                unitProductCost = scale4(c.getAcquisitionCost());
                productCost = scale4(unitProductCost.multiply(BigDecimal.valueOf(qty)));
                packagingCost = scale4(c.getPackagingCost().multiply(BigDecimal.valueOf(qty)));
                additionalCost = scale4(c.getAdditionalCost().multiply(BigDecimal.valueOf(qty)));
                taxRatePercent = scale4(c.getTaxRatePercent());
                taxAmount = scale4(netRevenue.multiply(taxRatePercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_EVEN));
                otherExpensesAmount = scale4(netRevenue.multiply(c.getOtherExpensesPercent()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_EVEN));
                itemCostProvenance = FinancialValueProvenance.USER_INFORMED;
            } else {
                itemCostProvenance = FinancialValueProvenance.MISSING;
                overallCostProvenance = FinancialValueProvenance.MISSING;
                overallTaxProvenance = FinancialValueProvenance.MISSING;
                itemHasMissingData = true;
                itemMissingReasons.add("PRODUCT_COST_MISSING");
                if (!orderMissingReasons.contains("PRODUCT_COST_MISSING")) {
                    orderMissingReasons.add("PRODUCT_COST_MISSING");
                }
            }

            // Resolução de Tarifas e Comissões do Canal (FeeResolutionService)
            BigDecimal itemMarketplaceFee = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal itemFixedFee = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            List<String> appliedRules = new ArrayList<>();

            try {
                FeeResolutionResult feeResult = feeResolutionService.resolveFees(
                        tenantId,
                        new FeeResolutionRequest(
                                order.getMarketplace(),
                                "CLASSIC",
                                null,
                                unitPrice,
                                order.getOrderCreatedAt()
                        )
                );

                for (ResolvedFeeItem fee : feeResult.resolvedFees()) {
                    appliedRules.add(fee.feeType() + ":" + fee.ruleId() + "(v" + fee.ruleVersion() + ")");
                    if (fee.feeType() == FeeType.COMMISSION) {
                        BigDecimal feeCalc = fee.calculatedAmount() != null
                                ? fee.calculatedAmount().multiply(BigDecimal.valueOf(qty))
                                : BigDecimal.ZERO;
                        itemMarketplaceFee = itemMarketplaceFee.add(scale4(feeCalc));
                    } else if (fee.feeType() == FeeType.FIXED_FEE) {
                        BigDecimal feeFixed = fee.calculatedAmount() != null
                                ? fee.calculatedAmount().multiply(BigDecimal.valueOf(qty))
                                : BigDecimal.ZERO;
                        itemFixedFee = itemFixedFee.add(scale4(feeFixed));
                    } else {
                        // Outras taxas somadas à taxa de marketplace
                        BigDecimal otherFee = fee.calculatedAmount() != null
                                ? fee.calculatedAmount().multiply(BigDecimal.valueOf(qty))
                                : BigDecimal.ZERO;
                        itemMarketplaceFee = itemMarketplaceFee.add(scale4(otherFee));
                    }
                }

                if (feeResult.hasMissingFees() || feeResult.resolvedFees().isEmpty()) {
                    itemHasMissingData = true;
                    itemMissingReasons.add("MARKETPLACE_FEE_RULE_MISSING");
                    overallFeeProvenance = FinancialValueProvenance.MISSING;
                    if (!orderMissingReasons.contains("MARKETPLACE_FEE_RULE_MISSING")) {
                        orderMissingReasons.add("MARKETPLACE_FEE_RULE_MISSING");
                    }
                }
            } catch (Exception e) {
                log.warn("Erro ao resolver taxas para item {}: {}", item.getId(), e.getMessage());
                itemHasMissingData = true;
                itemMissingReasons.add("MARKETPLACE_FEE_RESOLUTION_FAILED");
                overallFeeProvenance = FinancialValueProvenance.MISSING;
                if (!orderMissingReasons.contains("MARKETPLACE_FEE_RESOLUTION_FAILED")) {
                    orderMissingReasons.add("MARKETPLACE_FEE_RESOLUTION_FAILED");
                }
            }

            // Tratamento de Cancelamento / Devolução
            BigDecimal cancellationAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal returnAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);

            String statusStr = order.getStatus() != null ? order.getStatus().name() : "PAID";
            if ("CANCELLED".equalsIgnoreCase(statusStr)) {
                cancellationAmount = netRevenue;
            } else if ("RETURNED".equalsIgnoreCase(statusStr)) {
                returnAmount = netRevenue;
            }

            // Deduções totais e Margem de Contribuição
            BigDecimal totalDeductions = scale4(
                    itemMarketplaceFee
                            .add(itemFixedFee)
                            .add(itemShipping)
                            .add(productCost)
                            .add(packagingCost)
                            .add(additionalCost)
                            .add(taxAmount)
                            .add(otherExpensesAmount)
            );

            BigDecimal contributionMarginAmount = scale4(netRevenue.subtract(cancellationAmount).subtract(totalDeductions));
            BigDecimal contributionMarginRatio = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            if (netRevenue.compareTo(BigDecimal.ZERO) > 0 && cancellationAmount.compareTo(BigDecimal.ZERO) == 0) {
                contributionMarginRatio = scale4(
                        contributionMarginAmount.multiply(BigDecimal.valueOf(100)).divide(netRevenue, 4, RoundingMode.HALF_EVEN)
                );
            }

            if (itemHasMissingData) {
                orderHasMissingData = true;
            }

            // Auditoria do item
            Map<String, Object> itemTrace = new LinkedHashMap<>();
            itemTrace.put("orderItemId", item.getId());
            itemTrace.put("sku", item.getSku());
            itemTrace.put("unitPrice", unitPrice);
            itemTrace.put("quantity", qty);
            itemTrace.put("appliedCostRecordId", costOpt.map(ProductCostRecord::getId).orElse(null));
            itemTrace.put("appliedCostVersion", costOpt.map(ProductCostRecord::getVersion).orElse(null));
            itemTrace.put("appliedFeeRules", appliedRules);
            itemTrace.put("missingDataReasons", itemMissingReasons);
            itemTraces.add(itemTrace);

            computedItems.add(new ComputedItemFact(
                    item,
                    qty,
                    unitPrice,
                    grossRevenue,
                    discountAmount,
                    netRevenue,
                    cancellationAmount,
                    returnAmount,
                    itemMarketplaceFee,
                    itemFixedFee,
                    itemShipping,
                    unitProductCost,
                    productCost,
                    packagingCost,
                    additionalCost,
                    taxRatePercent,
                    taxAmount,
                    otherExpensesAmount,
                    totalDeductions,
                    contributionMarginAmount,
                    contributionMarginRatio,
                    itemCostProvenance,
                    itemHasMissingData,
                    toJson(itemMissingReasons)
            ));
        }

        // 3. Totais Consolidados do Pedido
        BigDecimal totalGrossRevenue = BigDecimal.ZERO;
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        BigDecimal totalNetRevenue = BigDecimal.ZERO;
        BigDecimal totalCancellationAmount = BigDecimal.ZERO;
        BigDecimal totalReturnAmount = BigDecimal.ZERO;
        BigDecimal totalMarketplaceFee = BigDecimal.ZERO;
        BigDecimal totalFixedFee = BigDecimal.ZERO;
        BigDecimal totalShippingCost = BigDecimal.ZERO;
        BigDecimal totalProductCost = BigDecimal.ZERO;
        BigDecimal totalPackagingCost = BigDecimal.ZERO;
        BigDecimal totalAdditionalCost = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        BigDecimal totalOtherExpensesAmount = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalContributionMarginAmount = BigDecimal.ZERO;

        for (ComputedItemFact ci : computedItems) {
            totalGrossRevenue = totalGrossRevenue.add(ci.grossRevenue());
            totalDiscountAmount = totalDiscountAmount.add(ci.discountAmount());
            totalNetRevenue = totalNetRevenue.add(ci.netRevenue());
            totalCancellationAmount = totalCancellationAmount.add(ci.cancellationAmount());
            totalReturnAmount = totalReturnAmount.add(ci.returnAmount());
            totalMarketplaceFee = totalMarketplaceFee.add(ci.marketplaceFee());
            totalFixedFee = totalFixedFee.add(ci.fixedFee());
            totalShippingCost = totalShippingCost.add(ci.shippingCost());
            totalProductCost = totalProductCost.add(ci.productCost());
            totalPackagingCost = totalPackagingCost.add(ci.packagingCost());
            totalAdditionalCost = totalAdditionalCost.add(ci.additionalCost());
            totalTaxAmount = totalTaxAmount.add(ci.taxAmount());
            totalOtherExpensesAmount = totalOtherExpensesAmount.add(ci.otherExpensesAmount());
            totalDeductions = totalDeductions.add(ci.totalDeductions());
            totalContributionMarginAmount = totalContributionMarginAmount.add(ci.contributionMarginAmount());
        }

        BigDecimal totalContributionMarginRatio = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
        if (totalNetRevenue.compareTo(BigDecimal.ZERO) > 0 && totalCancellationAmount.compareTo(BigDecimal.ZERO) == 0) {
            totalContributionMarginRatio = scale4(
                    totalContributionMarginAmount.multiply(BigDecimal.valueOf(100)).divide(totalNetRevenue, 4, RoundingMode.HALF_EVEN)
            );
        }

        // Trilha JSON de cálculo
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("engineVersion", ENGINE_VERSION);
        trace.put("calculatedAt", Instant.now().toString());
        trace.put("orderId", order.getId());
        trace.put("externalOrderId", order.getExternalOrderId());
        trace.put("companyId", companyId);
        trace.put("antiHallucinationDisclaimer", OrderFinancialFactRecord.ANTI_HALLUCINATION_DISCLAIMER);
        trace.put("items", itemTraces);
        trace.put("missingDataReasons", orderMissingReasons);
        String traceJson = toJson(trace);

        // 4. Persistência Idempotente do Pedido
        Optional<OrderFinancialFactRecord> existingOrderFactOpt =
                orderFinancialFactRepository.findByTenantIdAndOrderId(tenantId, orderId);

        OrderFinancialFactRecord orderFact;
        if (existingOrderFactOpt.isPresent()) {
            orderFact = existingOrderFactOpt.get();
            orderFact.updateComputedValues(
                    companyId,
                    order.getStatus() != null ? order.getStatus().name() : "PAID",
                    totalGrossRevenue,
                    totalDiscountAmount,
                    totalNetRevenue,
                    totalCancellationAmount,
                    totalReturnAmount,
                    totalMarketplaceFee,
                    totalFixedFee,
                    totalShippingCost,
                    totalProductCost,
                    totalPackagingCost,
                    totalAdditionalCost,
                    totalTaxAmount,
                    totalOtherExpensesAmount,
                    totalDeductions,
                    totalContributionMarginAmount,
                    totalContributionMarginRatio,
                    overallFeeProvenance,
                    FinancialValueProvenance.ESTIMATED,
                    overallCostProvenance,
                    overallTaxProvenance,
                    orderHasMissingData,
                    toJson(orderMissingReasons),
                    ENGINE_VERSION,
                    traceJson
            );
        } else {
            orderFact = new OrderFinancialFactRecord(
                    UUID.randomUUID(),
                    tenantId,
                    order.getId(),
                    order.getSellerAccountId(),
                    companyId,
                    order.getMarketplace(),
                    order.getExternalOrderId(),
                    order.getStatus() != null ? order.getStatus().name() : "PAID",
                    order.getOrderCreatedAt() != null ? order.getOrderCreatedAt() : Instant.now(),
                    totalGrossRevenue,
                    totalDiscountAmount,
                    totalNetRevenue,
                    totalCancellationAmount,
                    totalReturnAmount,
                    totalMarketplaceFee,
                    totalFixedFee,
                    totalShippingCost,
                    totalProductCost,
                    totalPackagingCost,
                    totalAdditionalCost,
                    totalTaxAmount,
                    totalOtherExpensesAmount,
                    totalDeductions,
                    totalContributionMarginAmount,
                    totalContributionMarginRatio,
                    order.getCurrency() != null ? order.getCurrency() : "BRL",
                    ReconciliationStatus.ESTIMATED,
                    overallFeeProvenance,
                    FinancialValueProvenance.ESTIMATED,
                    overallCostProvenance,
                    overallTaxProvenance,
                    orderHasMissingData,
                    toJson(orderMissingReasons),
                    ENGINE_VERSION,
                    traceJson
            );
        }

        orderFact = orderFinancialFactRepository.save(orderFact);

        // 5. Persistência Idempotente dos Itens
        List<OrderItemFinancialFactRecord> savedItemFacts = new ArrayList<>();
        for (ComputedItemFact ci : computedItems) {
            Optional<OrderItemFinancialFactRecord> existingItemFactOpt =
                    orderItemFinancialFactRepository.findByTenantIdAndOrderItemId(tenantId, ci.item().getId());

            OrderItemFinancialFactRecord itemFact;
            if (existingItemFactOpt.isPresent()) {
                itemFact = existingItemFactOpt.get();
                itemFact.updateValues(
                        ci.qty(),
                        ci.unitPrice(),
                        ci.grossRevenue(),
                        ci.discountAmount(),
                        ci.netRevenue(),
                        ci.cancellationAmount(),
                        ci.returnAmount(),
                        ci.marketplaceFee(),
                        ci.fixedFee(),
                        ci.shippingCost(),
                        ci.unitProductCost(),
                        ci.productCost(),
                        ci.packagingCost(),
                        ci.additionalCost(),
                        ci.taxRatePercent(),
                        ci.taxAmount(),
                        ci.otherExpensesAmount(),
                        ci.totalDeductions(),
                        ci.contributionMarginAmount(),
                        ci.contributionMarginRatio(),
                        ci.costProvenance(),
                        ci.hasMissingData(),
                        ci.missingDataReasons()
                );
            } else {
                itemFact = new OrderItemFinancialFactRecord(
                        UUID.randomUUID(),
                        tenantId,
                        orderFact.getId(),
                        order.getId(),
                        ci.item().getId(),
                        ci.item().getProductId(),
                        ci.item().getProductVariantId(),
                        ci.item().getSku(),
                        ci.item().getTitle(),
                        ci.qty(),
                        ci.unitPrice(),
                        ci.grossRevenue(),
                        ci.discountAmount(),
                        ci.netRevenue(),
                        ci.cancellationAmount(),
                        ci.returnAmount(),
                        ci.marketplaceFee(),
                        ci.fixedFee(),
                        ci.shippingCost(),
                        ci.unitProductCost(),
                        ci.productCost(),
                        ci.packagingCost(),
                        ci.additionalCost(),
                        ci.taxRatePercent(),
                        ci.taxAmount(),
                        ci.otherExpensesAmount(),
                        ci.totalDeductions(),
                        ci.contributionMarginAmount(),
                        ci.contributionMarginRatio(),
                        ci.costProvenance(),
                        ci.hasMissingData(),
                        ci.missingDataReasons()
                );
            }
            savedItemFacts.add(orderItemFinancialFactRepository.save(itemFact));
        }

        return mapToDto(orderFact, savedItemFacts);
    }

    /**
     * Reconcilia os fatos financeiros de um pedido com o extrato de liquidação financeira externa.
     */
    public OrderFinancialFactDto reconcileWithExternalSettlement(
            UUID tenantId,
            UUID orderId,
            ReconcileSettlementCommand command
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(orderId, "orderId não pode ser nulo");
        Objects.requireNonNull(command, "command não pode ser nulo");

        OrderFinancialFactRecord orderFact = orderFinancialFactRepository.findByTenantIdAndOrderId(tenantId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Fato financeiro não encontrado para o pedido: " + orderId));

        List<OrderItemFinancialFactRecord> itemFacts =
                orderItemFinancialFactRepository.findAllByTenantIdAndOrderFactIdOrderByCreatedAtAsc(tenantId, orderFact.getId());

        BigDecimal confirmedFee = command.confirmedMarketplaceFee() != null ? scale4(command.confirmedMarketplaceFee()) : orderFact.getMarketplaceFee();
        BigDecimal confirmedFixedFee = command.confirmedFixedFee() != null ? scale4(command.confirmedFixedFee()) : orderFact.getFixedFee();
        BigDecimal confirmedShipping = command.confirmedShippingCost() != null ? scale4(command.confirmedShippingCost()) : orderFact.getShippingCost();

        // Verificação de divergência
        BigDecimal feeDiff = confirmedFee.subtract(orderFact.getMarketplaceFee()).abs();
        BigDecimal fixedFeeDiff = confirmedFixedFee.subtract(orderFact.getFixedFee()).abs();
        BigDecimal shippingDiff = confirmedShipping.subtract(orderFact.getShippingCost()).abs();

        boolean isDivergent = feeDiff.compareTo(TOLERANCE_CENT) > 0
                || fixedFeeDiff.compareTo(TOLERANCE_CENT) > 0
                || shippingDiff.compareTo(TOLERANCE_CENT) > 0;

        ReconciliationStatus status = isDivergent ? ReconciliationStatus.DIVERGENT : ReconciliationStatus.RECONCILED;

        // Atualiza trilha JSON
        Map<String, Object> trace = parseJsonToMap(orderFact.getCalculationTraceJson());
        Map<String, Object> reconciliationTrace = new LinkedHashMap<>();
        reconciliationTrace.put("reconciledAt", Instant.now().toString());
        reconciliationTrace.put("settlementExternalId", command.settlementExternalId());
        reconciliationTrace.put("settlementDate", command.settlementDate().toString());
        reconciliationTrace.put("payoutAmount", command.payoutAmount());
        reconciliationTrace.put("confirmedFee", confirmedFee);
        reconciliationTrace.put("confirmedFixedFee", confirmedFixedFee);
        reconciliationTrace.put("confirmedShipping", confirmedShipping);
        reconciliationTrace.put("feeDiff", feeDiff);
        reconciliationTrace.put("fixedFeeDiff", fixedFeeDiff);
        reconciliationTrace.put("shippingDiff", shippingDiff);
        reconciliationTrace.put("status", status.name());
        trace.put("reconciliation", reconciliationTrace);

        orderFact.applyReconciliation(
                command.settlementExternalId(),
                command.settlementDate(),
                confirmedFee,
                confirmedFixedFee,
                confirmedShipping,
                status,
                command.notes(),
                toJson(trace)
        );

        orderFact = orderFinancialFactRepository.save(orderFact);

        // Rateio das taxas confirmadas proporcionalmente entre os itens
        BigDecimal totalNetRevenue = orderFact.getNetRevenue();
        BigDecimal runningApportionedFee = BigDecimal.ZERO;
        BigDecimal runningApportionedFixedFee = BigDecimal.ZERO;
        BigDecimal runningApportionedShipping = BigDecimal.ZERO;

        for (int i = 0; i < itemFacts.size(); i++) {
            OrderItemFinancialFactRecord item = itemFacts.get(i);
            boolean isLast = (i == itemFacts.size() - 1);

            BigDecimal itemFee;
            BigDecimal itemFixed;
            BigDecimal itemShip;

            if (isLast) {
                itemFee = scale4(confirmedFee.subtract(runningApportionedFee));
                itemFixed = scale4(confirmedFixedFee.subtract(runningApportionedFixedFee));
                itemShip = scale4(confirmedShipping.subtract(runningApportionedShipping));
            } else if (totalNetRevenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal shareRatio = item.getNetRevenue().divide(totalNetRevenue, 4, RoundingMode.HALF_EVEN);
                itemFee = scale4(confirmedFee.multiply(shareRatio));
                itemFixed = scale4(confirmedFixedFee.multiply(shareRatio));
                itemShip = scale4(confirmedShipping.multiply(shareRatio));

                runningApportionedFee = runningApportionedFee.add(itemFee);
                runningApportionedFixedFee = runningApportionedFixedFee.add(itemFixed);
                runningApportionedShipping = runningApportionedShipping.add(itemShip);
            } else {
                BigDecimal equalDiv = BigDecimal.valueOf(itemFacts.size());
                itemFee = scale4(confirmedFee.divide(equalDiv, 4, RoundingMode.HALF_EVEN));
                itemFixed = scale4(confirmedFixedFee.divide(equalDiv, 4, RoundingMode.HALF_EVEN));
                itemShip = scale4(confirmedShipping.divide(equalDiv, 4, RoundingMode.HALF_EVEN));

                runningApportionedFee = runningApportionedFee.add(itemFee);
                runningApportionedFixedFee = runningApportionedFixedFee.add(itemFixed);
                runningApportionedShipping = runningApportionedShipping.add(itemShip);
            }

            item.updateReconciledFees(itemFee, itemFixed, itemShip);
            orderItemFinancialFactRepository.save(item);
        }

        return mapToDto(orderFact, itemFacts);
    }

    /**
     * Recupera os fatos financeiros de um pedido específico.
     */
    @Transactional(readOnly = true)
    public OrderFinancialFactDto getOrderFinancialFacts(UUID tenantId, UUID orderId) {
        OrderFinancialFactRecord fact = orderFinancialFactRepository.findByTenantIdAndOrderId(tenantId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Fato financeiro não encontrado para o pedido: " + orderId));

        List<OrderItemFinancialFactRecord> items =
                orderItemFinancialFactRepository.findAllByTenantIdAndOrderFactIdOrderByCreatedAtAsc(tenantId, fact.getId());

        return mapToDto(fact, items);
    }

    /**
     * Consulta paginada com filtros.
     */
    @Transactional(readOnly = true)
    public Page<OrderFinancialFactDto> listFinancialFacts(
            UUID tenantId,
            UUID companyId,
            String marketplace,
            ReconciliationStatus status,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        return orderFinancialFactRepository.searchFacts(
                tenantId, companyId, marketplace, status, startDate, endDate, pageable
        ).map(fact -> {
            List<OrderItemFinancialFactRecord> items =
                    orderItemFinancialFactRepository.findAllByTenantIdAndOrderFactIdOrderByCreatedAtAsc(tenantId, fact.getId());
            return mapToDto(fact, items);
        });
    }

    /**
     * Sumarização consolidada para relatórios e dashboards com garantia anti-alucinação.
     */
    @Transactional(readOnly = true)
    public FinancialFactSummaryDto getFinancialSummary(
            UUID tenantId,
            UUID companyId,
            String marketplace,
            Instant startDate,
            Instant endDate
    ) {
        List<OrderFinancialFactRecord> facts =
                orderFinancialFactRepository.findForSummary(tenantId, companyId, marketplace, startDate, endDate);

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

        return new FinancialFactSummaryDto(
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
                reconciled,
                estimated,
                divergent,
                missingDataCount,
                OrderFinancialFactRecord.ANTI_HALLUCINATION_DISCLAIMER
        );
    }

    private OrderFinancialFactDto mapToDto(OrderFinancialFactRecord fact, List<OrderItemFinancialFactRecord> items) {
        List<OrderItemFinancialFactDto> itemDtos = items.stream()
                .map(i -> new OrderItemFinancialFactDto(
                        i.getId(),
                        i.getOrderItemId(),
                        i.getProductId(),
                        i.getVariantId(),
                        i.getSku(),
                        i.getTitle(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getGrossRevenue(),
                        i.getDiscountAmount(),
                        i.getNetRevenue(),
                        i.getCancellationAmount(),
                        i.getReturnAmount(),
                        i.getMarketplaceFee(),
                        i.getFixedFee(),
                        i.getShippingCost(),
                        i.getUnitProductCost(),
                        i.getProductCost(),
                        i.getPackagingCost(),
                        i.getAdditionalCost(),
                        i.getTaxRatePercent(),
                        i.getTaxAmount(),
                        i.getOtherExpensesAmount(),
                        i.getTotalDeductions(),
                        i.getContributionMarginAmount(),
                        i.getContributionMarginRatio(),
                        i.getCostProvenance(),
                        i.isHasMissingData(),
                        i.getMissingDataReasons()
                ))
                .toList();

        return new OrderFinancialFactDto(
                fact.getId(),
                fact.getOrderId(),
                fact.getSellerAccountId(),
                fact.getCompanyId(),
                fact.getMarketplace(),
                fact.getExternalOrderId(),
                fact.getOrderStatus(),
                fact.getOrderDate(),
                fact.getGrossRevenue(),
                fact.getDiscountAmount(),
                fact.getNetRevenue(),
                fact.getCancellationAmount(),
                fact.getReturnAmount(),
                fact.getMarketplaceFee(),
                fact.getFixedFee(),
                fact.getShippingCost(),
                fact.getProductCost(),
                fact.getPackagingCost(),
                fact.getAdditionalCost(),
                fact.getTaxAmount(),
                fact.getOtherExpensesAmount(),
                fact.getTotalDeductions(),
                fact.getContributionMarginAmount(),
                fact.getContributionMarginRatio(),
                fact.getCurrency(),
                fact.getReconciliationStatus(),
                fact.getFeeProvenance(),
                fact.getShippingProvenance(),
                fact.getCostProvenance(),
                fact.getTaxProvenance(),
                fact.isHasMissingData(),
                fact.getMissingDataReasons(),
                fact.getCalculationVersion(),
                fact.getCalculationTraceJson(),
                fact.getSettlementExternalId(),
                fact.getSettlementDate(),
                fact.getSettlementNotes(),
                fact.getReconciledAt(),
                OrderFinancialFactRecord.ANTI_HALLUCINATION_DISCLAIMER,
                itemDtos,
                fact.getCreatedAt(),
                fact.getUpdatedAt()
        );
    }

    private static BigDecimal scale4(BigDecimal val) {
        return val != null ? val.setScale(4, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private record ComputedItemFact(
            OrderItem item,
            int qty,
            BigDecimal unitPrice,
            BigDecimal grossRevenue,
            BigDecimal discountAmount,
            BigDecimal netRevenue,
            BigDecimal cancellationAmount,
            BigDecimal returnAmount,
            BigDecimal marketplaceFee,
            BigDecimal fixedFee,
            BigDecimal shippingCost,
            BigDecimal unitProductCost,
            BigDecimal productCost,
            BigDecimal packagingCost,
            BigDecimal additionalCost,
            BigDecimal taxRatePercent,
            BigDecimal taxAmount,
            BigDecimal otherExpensesAmount,
            BigDecimal totalDeductions,
            BigDecimal contributionMarginAmount,
            BigDecimal contributionMarginRatio,
            FinancialValueProvenance costProvenance,
            boolean hasMissingData,
            String missingDataReasons
    ) {}
}
