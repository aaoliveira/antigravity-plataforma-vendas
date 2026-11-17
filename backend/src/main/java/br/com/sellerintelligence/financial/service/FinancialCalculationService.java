package br.com.sellerintelligence.financial.service;

import br.com.sellerintelligence.financial.domain.*;
import br.com.sellerintelligence.financial.engine.*;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.sync.domain.Order;
import br.com.sellerintelligence.sync.domain.OrderItem;
import br.com.sellerintelligence.sync.domain.OrderRepository;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class FinancialCalculationService {

    private final FinancialCalculationEngine calculationEngine;
    private final SkuCostService skuCostService;
    private final FinancialRuleService financialRuleService;
    private final OrderRepository orderRepository;
    private final OrderFinancialCalculationRepository calculationRepository;
    private final OrderItemFinancialCalculationRepository itemCalculationRepository;
    private final ObjectMapper objectMapper;

    public FinancialCalculationService(
            FinancialCalculationEngine calculationEngine,
            SkuCostService skuCostService,
            FinancialRuleService financialRuleService,
            OrderRepository orderRepository,
            OrderFinancialCalculationRepository calculationRepository,
            OrderItemFinancialCalculationRepository itemCalculationRepository,
            ObjectMapper objectMapper
    ) {
        this.calculationEngine = calculationEngine;
        this.skuCostService = skuCostService;
        this.financialRuleService = financialRuleService;
        this.orderRepository = orderRepository;
        this.calculationRepository = calculationRepository;
        this.itemCalculationRepository = itemCalculationRepository;
        this.objectMapper = objectMapper;
    }

    public OrderFinancialCalculation calculateAndSaveOrder(UUID orderId, UUID optionalRuleSetId) {
        UUID tenantId = TenantContext.getTenantId();

        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Pedido não encontrado para o tenant: " + orderId, HttpStatus.NOT_FOUND));

        FinancialRuleSet ruleSet;
        if (optionalRuleSetId != null) {
            ruleSet = financialRuleService.getRuleById(optionalRuleSetId)
                    .orElseThrow(() -> new BusinessException("RULE_SET_NOT_FOUND", "Regra financeira não encontrada: " + optionalRuleSetId, HttpStatus.NOT_FOUND));
        } else {
            ruleSet = financialRuleService.findBestMatchingRule(
                    order.getMarketplace() != null ? order.getMarketplace().name() : null,
                    order.getOrderDate()
            ).orElse(null);
        }

        // Extract components from rule set
        Map<FinancialComponentType, BigDecimal> ruleParams = new EnumMap<>(FinancialComponentType.class);
        BigDecimal fixedThreshold = new BigDecimal("79.00");
        if (ruleSet != null) {
            for (FinancialRuleComponent comp : ruleSet.getComponents()) {
                ruleParams.put(comp.getComponentType(), comp.getComponentValue());
                if (comp.getComponentType() == FinancialComponentType.MARKETPLACE_FIXED_FEE_THRESHOLD && comp.getThresholdAmount() != null) {
                    fixedThreshold = comp.getThresholdAmount();
                }
            }
        }

        // Build item inputs
        List<ItemCalculationInput> itemInputs = new ArrayList<>();
        List<OrderItem> orderItems = order.getItems();

        for (OrderItem oi : orderItems) {
            BigDecimal unitCogs = null;
            if (oi.getSku() != null && !oi.getSku().isBlank()) {
                Optional<SkuCost> costOpt = skuCostService.getEffectiveCost(oi.getSku(), order.getOrderDate());
                if (costOpt.isPresent()) {
                    unitCogs = costOpt.get().getCostPrice();
                }
            }

            // Commission & fees: if observed sale_fee exists in order item, we can derive or use configured
            BigDecimal commPct = ruleParams.get(FinancialComponentType.MARKETPLACE_COMMISSION_PERCENTAGE);
            BigDecimal fixedFee = ruleParams.get(FinancialComponentType.MARKETPLACE_FIXED_FEE);
            BigDecimal packaging = ruleParams.get(FinancialComponentType.PACKAGING_COST);
            BigDecimal taxPct = ruleParams.get(FinancialComponentType.TAX_PERCENTAGE);
            BigDecimal otherPct = ruleParams.get(FinancialComponentType.OTHER_VARIABLE_PERCENTAGE);
            BigDecimal otherFixed = ruleParams.get(FinancialComponentType.OTHER_VARIABLE_FIXED);
            BigDecimal shippingSeller = ruleParams.get(FinancialComponentType.SHIPPING_SELLER_COST);

            itemInputs.add(new ItemCalculationInput(
                    oi.getId(),
                    oi.getSku(),
                    oi.getUnitPrice(),
                    oi.getQuantity(),
                    unitCogs,
                    commPct,
                    fixedFee,
                    fixedThreshold,
                    shippingSeller,
                    packaging,
                    taxPct,
                    otherPct,
                    otherFixed
            ));
        }

        // Check if order was cancelled in marketplace
        CalculationStatus calcStatus = CalculationStatus.CLOSED;
        if ("CANCELLED".equalsIgnoreCase(order.getStatus()) || "CANCELADO".equalsIgnoreCase(order.getStatus())) {
            calcStatus = CalculationStatus.CANCELLED;
        }

        BigDecimal orderShipping = order.getShippingCostSeller();

        OrderCalculationInput orderInput = new OrderCalculationInput(
                order.getId(),
                ruleSet != null ? ruleSet.getId() : null,
                ruleSet != null ? ruleSet.getVersion() : 1,
                calcStatus,
                itemInputs,
                orderShipping,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        OrderCalculationResult result = calculationEngine.calculateOrder(orderInput);

        // Delete existing calculation if any (idempotency)
        Optional<OrderFinancialCalculation> existing = calculationRepository.findByTenantIdAndOrderId(tenantId, orderId);
        existing.ifPresent(calculationRepository::delete);

        OrderFinancialCalculation calculation = new OrderFinancialCalculation(
                UUID.randomUUID(),
                tenantId,
                order.getId(),
                result.ruleSetId(),
                result.ruleVersion(),
                result.status(),
                result.quality(),
                result.grossRevenue(),
                result.netRevenue(),
                result.totalCogs(),
                result.totalMarketplaceFees(),
                result.totalShippingSeller(),
                result.totalTaxes(),
                result.totalPackaging(),
                result.totalOtherCosts(),
                result.contributionMarginValue(),
                result.contributionMarginPercentage(),
                order.getCurrency() != null ? order.getCurrency() : "BRL",
                String.join(",", result.missingFields()),
                result.assumptions(),
                Instant.now(),
                Instant.now()
        );

        for (ItemCalculationResult itemRes : result.items()) {
            String memoryJson = serializeMemory(itemRes.memory());
            OrderItemFinancialCalculation itemCalc = new OrderItemFinancialCalculation(
                    UUID.randomUUID(),
                    tenantId,
                    calculation,
                    itemRes.orderItemId(),
                    itemRes.sku(),
                    itemRes.unitPrice(),
                    itemRes.quantity(),
                    itemRes.totalPrice(),
                    itemRes.unitCogs(),
                    itemRes.totalCogs(),
                    itemRes.marketplaceFee(),
                    itemRes.shippingSeller(),
                    itemRes.taxValue(),
                    itemRes.packagingCost(),
                    itemRes.otherCosts(),
                    itemRes.contributionMarginValue(),
                    itemRes.contributionMarginPercentage(),
                    itemRes.quality(),
                    memoryJson
            );
            calculation.addItemCalculation(itemCalc);
        }

        return calculationRepository.save(calculation);
    }

    public OrderFinancialCalculation recalculateHistoricalOrder(UUID orderId, UUID newRuleSetId, String auditReason) {
        UUID tenantId = TenantContext.getTenantId();

        OrderFinancialCalculation calculation = calculationRepository.findByTenantIdAndOrderId(tenantId, orderId)
                .orElseThrow(() -> new BusinessException("CALCULATION_NOT_FOUND", "Cálculo anterior não encontrado para recálculo: " + orderId, HttpStatus.NOT_FOUND));

        OrderFinancialCalculation newlyCalculated = calculateAndSaveOrder(orderId, newRuleSetId);
        newlyCalculated.markRecalculated(auditReason != null ? auditReason : "Recálculo histórico autorizado");
        return calculationRepository.save(newlyCalculated);
    }

    public OrderFinancialCalculation handleOrderCancellation(UUID orderId, String reason) {
        UUID tenantId = TenantContext.getTenantId();

        OrderFinancialCalculation calculation = calculationRepository.findByTenantIdAndOrderId(tenantId, orderId)
                .orElseThrow(() -> new BusinessException("CALCULATION_NOT_FOUND", "Cálculo de pedido não encontrado: " + orderId, HttpStatus.NOT_FOUND));

        calculation.markCancelled(reason != null ? reason : "Cancelamento de pedido registrado");
        return calculationRepository.save(calculation);
    }

    public OrderFinancialCalculation handleOrderReturn(UUID orderId, BigDecimal shippingLoss,
                                                      BigDecimal packagingLoss, BigDecimal cogsLoss, String reason) {
        UUID tenantId = TenantContext.getTenantId();

        OrderFinancialCalculation calculation = calculationRepository.findByTenantIdAndOrderId(tenantId, orderId)
                .orElseThrow(() -> new BusinessException("CALCULATION_NOT_FOUND", "Cálculo de pedido não encontrado: " + orderId, HttpStatus.NOT_FOUND));

        calculation.markReturned(shippingLoss, packagingLoss, cogsLoss, reason != null ? reason : "Devolução registrada");
        return calculationRepository.save(calculation);
    }

    @Transactional(readOnly = true)
    public Optional<OrderFinancialCalculation> getOrderCalculation(UUID orderId) {
        UUID tenantId = TenantContext.getTenantId();
        return calculationRepository.findByTenantIdAndOrderId(tenantId, orderId);
    }

    private String serializeMemory(CalculationMemory memory) {
        try {
            return objectMapper.writeValueAsString(memory);
        } catch (JsonProcessingException e) {
            return "{\"quality\":\"" + memory.quality() + "\",\"summary\":\"" + memory.summary() + "\"}";
        }
    }
}
