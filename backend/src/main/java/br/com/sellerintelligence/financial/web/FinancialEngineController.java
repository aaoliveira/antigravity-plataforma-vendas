package br.com.sellerintelligence.financial.web;

import br.com.sellerintelligence.financial.domain.FinancialRuleSet;
import br.com.sellerintelligence.financial.domain.OrderFinancialCalculation;
import br.com.sellerintelligence.financial.domain.SkuCost;
import br.com.sellerintelligence.financial.engine.FinancialCalculationEngine;
import br.com.sellerintelligence.financial.engine.PriceSimulationInput;
import br.com.sellerintelligence.financial.engine.PriceSimulationResult;
import br.com.sellerintelligence.financial.service.FinancialCalculationService;
import br.com.sellerintelligence.financial.service.FinancialRuleService;
import br.com.sellerintelligence.financial.service.SkuCostService;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/financial")
public class FinancialEngineController {

    private final FinancialCalculationEngine calculationEngine;
    private final SkuCostService skuCostService;
    private final FinancialRuleService financialRuleService;
    private final FinancialCalculationService calculationService;

    public FinancialEngineController(
            FinancialCalculationEngine calculationEngine,
            SkuCostService skuCostService,
            FinancialRuleService financialRuleService,
            FinancialCalculationService calculationService
    ) {
        this.calculationEngine = calculationEngine;
        this.skuCostService = skuCostService;
        this.financialRuleService = financialRuleService;
        this.calculationService = calculationService;
    }

    @PostMapping("/simulate")
    public ResponseEntity<PriceSimulationResponse> simulatePrice(@Valid @RequestBody SimulatePriceRequest request) {
        PriceSimulationInput input = new PriceSimulationInput(
                request.unitCogs(),
                request.packagingCost(),
                request.shippingSeller(),
                request.fixedFee(),
                request.fixedFeeThreshold(),
                request.commissionPercentage(),
                request.taxPercentage(),
                request.otherVariablePercentage(),
                request.otherVariableFixed(),
                request.targetMarginPercentage(),
                request.targetMarginAmount()
        );

        PriceSimulationResult result = calculationEngine.simulatePrice(input, request.trialPrice());
        return ResponseEntity.ok(PriceSimulationResponse.fromResult(result));
    }

    @PostMapping("/costs")
    public ResponseEntity<SkuCostResponse> registerCost(@Valid @RequestBody CreateSkuCostRequest request) {
        SkuCost saved = skuCostService.registerCost(
                request.sku(),
                request.costPrice(),
                request.currency(),
                request.unit(),
                request.origin(),
                request.effectiveFrom(),
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SkuCostResponse.fromDomain(saved));
    }

    @GetMapping("/costs/{sku}")
    public ResponseEntity<List<SkuCostResponse>> getCostHistory(@PathVariable String sku) {
        List<SkuCost> history = skuCostService.getCostHistory(sku);
        return ResponseEntity.ok(history.stream().map(SkuCostResponse::fromDomain).toList());
    }

    @PostMapping("/rules")
    public ResponseEntity<FinancialRuleSetResponse> createOrVersionRuleSet(@Valid @RequestBody CreateFinancialRuleSetRequest request) {
        List<FinancialRuleService.ComponentConfig> components = request.components() != null ?
                request.components().stream()
                        .map(c -> new FinancialRuleService.ComponentConfig(
                                c.type(),
                                c.value(),
                                c.thresholdAmount(),
                                c.appliesTo(),
                                c.description()
                        ))
                        .toList() : List.of();

        FinancialRuleSet ruleSet = financialRuleService.createOrVersionRuleSet(
                request.companyId(),
                request.storeId(),
                request.marketplace(),
                request.name(),
                request.effectiveFrom(),
                components
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(FinancialRuleSetResponse.fromDomain(ruleSet));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<FinancialRuleSetResponse>> listRules() {
        List<FinancialRuleSet> rules = financialRuleService.listRules();
        return ResponseEntity.ok(rules.stream().map(FinancialRuleSetResponse::fromDomain).toList());
    }

    @PostMapping("/orders/{orderId}/calculate")
    public ResponseEntity<OrderCalculationResponse> calculateOrder(
            @PathVariable UUID orderId,
            @RequestParam(required = false) UUID ruleSetId
    ) {
        OrderFinancialCalculation calculation = calculationService.calculateAndSaveOrder(orderId, ruleSetId);
        return ResponseEntity.ok(OrderCalculationResponse.fromDomain(calculation));
    }

    @GetMapping("/orders/{orderId}/calculation")
    public ResponseEntity<OrderCalculationResponse> getOrderCalculation(@PathVariable UUID orderId) {
        OrderFinancialCalculation calculation = calculationService.getOrderCalculation(orderId)
                .orElseThrow(() -> new BusinessException("CALCULATION_NOT_FOUND", "Cálculo financeiro não encontrado para o pedido: " + orderId, HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(OrderCalculationResponse.fromDomain(calculation));
    }

    @PostMapping("/orders/{orderId}/recalculate")
    public ResponseEntity<OrderCalculationResponse> recalculateOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody RecalculateOrderRequest request
    ) {
        OrderFinancialCalculation calculation = calculationService.recalculateHistoricalOrder(
                orderId,
                request.ruleSetId(),
                request.auditReason()
        );
        return ResponseEntity.ok(OrderCalculationResponse.fromDomain(calculation));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<OrderCalculationResponse> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        OrderFinancialCalculation calculation = calculationService.handleOrderCancellation(orderId, request.reason());
        return ResponseEntity.ok(OrderCalculationResponse.fromDomain(calculation));
    }

    @PostMapping("/orders/{orderId}/return")
    public ResponseEntity<OrderCalculationResponse> returnOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody ReturnOrderRequest request
    ) {
        OrderFinancialCalculation calculation = calculationService.handleOrderReturn(
                orderId,
                request.shippingLoss(),
                request.packagingLoss(),
                request.cogsLoss(),
                request.reason()
        );
        return ResponseEntity.ok(OrderCalculationResponse.fromDomain(calculation));
    }
}
