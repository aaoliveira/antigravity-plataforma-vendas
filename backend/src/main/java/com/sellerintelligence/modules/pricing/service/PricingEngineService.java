package com.sellerintelligence.modules.pricing.service;

import com.sellerintelligence.modules.pricing.model.DeductionCategory;
import com.sellerintelligence.modules.pricing.model.FeeResolutionRequest;
import com.sellerintelligence.modules.pricing.model.FeeResolutionResult;
import com.sellerintelligence.modules.pricing.model.FeeSourceType;
import com.sellerintelligence.modules.pricing.model.FeeType;
import com.sellerintelligence.modules.pricing.model.PricingCalculationRequest;
import com.sellerintelligence.modules.pricing.model.PricingCalculationResult;
import com.sellerintelligence.modules.pricing.model.PricingDeductionItem;
import com.sellerintelligence.modules.pricing.model.ProductCostDto;
import com.sellerintelligence.modules.pricing.model.ProductMarginSimulationRequest;
import com.sellerintelligence.modules.pricing.model.ResolvedFeeItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Motor determinístico de cálculo de preço, margem de contribuição e ponto de equilíbrio.
 * REGRAS INVIOLÁVEIS:
 * 1. Proibição irrestrita de chamar LLMs para cálculos monetários.
 * 2. Operações universais em BigDecimal com escala interna 4 e apresentação 2 (RoundingMode.HALF_UP).
 * 3. Divisões por zero e denominadores <= 0 tratadas de forma fail-safe.
 * 4. Margem de contribuição NUNCA é rotulada como lucro líquido.
 */
@Service
public class PricingEngineService {

    public static final int CALCULATION_SCALE = 4;
    public static final int PRESENTATION_SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final String FORMULA_VERSION = "v1.0.0";

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final ProductCostService productCostService;
    private final FeeResolutionService feeResolutionService;

    public PricingEngineService(ProductCostService productCostService, FeeResolutionService feeResolutionService) {
        this.productCostService = productCostService;
        this.feeResolutionService = feeResolutionService;
    }

    /**
     * Calcula determinística e formalmente todas as grandezas de preço e margem de contribuição.
     */
    public PricingCalculationResult calculateMargins(PricingCalculationRequest request) {
        List<String> warnings = new ArrayList<>();
        List<String> assumptions = new ArrayList<>();

        // Premissas formais
        assumptions.add("Margem de contribuição apurada com base na receita líquida de descontos promocionais.");
        assumptions.add("Escala interna de cálculo de 4 casas decimais com arredondamento HALF_UP.");
        assumptions.add("Nomenclatura formal: Margem de Contribuição unitária; não deduz despesas fixas estruturais (não representa Lucro Líquido).");

        // 1. Sanitização de entradas
        BigDecimal grossPrice = sanitize(request.grossSellingPrice());
        BigDecimal discount = sanitize(request.promotionalDiscount());
        BigDecimal netSellingPrice = grossPrice.subtract(discount).max(ZERO);

        BigDecimal acqCost = sanitize(request.acquisitionCost());
        BigDecimal packagingCost = sanitize(request.packagingCost());
        BigDecimal addCost = sanitize(request.additionalCost());
        BigDecimal subsidizedShipping = sanitize(request.subsidizedShippingCost());

        BigDecimal commPercent = sanitize(request.channelCommissionPercent());
        BigDecimal fixedFee = sanitize(request.channelFixedFee());
        BigDecimal logisticsFee = sanitize(request.logisticsFee());
        BigDecimal taxPercent = sanitize(request.taxRatePercent());
        BigDecimal otherVarPercent = sanitize(request.otherVariableExpensesPercent());
        BigDecimal otherFixed = sanitize(request.otherFixedDeductions());

        // 2. Custos Diretos do Produto (CD / CMV)
        BigDecimal totalDirectCosts = acqCost.add(packagingCost).add(addCost);

        // 3. Deduções Fixas da Venda (DF)
        BigDecimal totalFixedDeductions = fixedFee.add(subsidizedShipping).add(logisticsFee).add(otherFixed);

        // 4. Deduções Variáveis Percentuais (DV)
        BigDecimal totalVarPercent = commPercent.add(taxPercent).add(otherVarPercent);

        BigDecimal commissionAmount = calculatePercentAmount(netSellingPrice, commPercent);
        BigDecimal taxAmount = calculatePercentAmount(netSellingPrice, taxPercent);
        BigDecimal otherVarAmount = calculatePercentAmount(netSellingPrice, otherVarPercent);

        BigDecimal totalVariableDeductions = commissionAmount.add(taxAmount).add(otherVarAmount);

        // 5. Total de Deduções
        BigDecimal totalDeductions = totalDirectCosts.add(totalFixedDeductions).add(totalVariableDeductions);

        // 6. Margem de Contribuição em Valor (MC_valor)
        BigDecimal contributionMarginAmount = netSellingPrice.subtract(totalDeductions);

        // 7. Margem de Contribuição Percentual (MC_%)
        BigDecimal contributionMarginPercent;
        if (netSellingPrice.compareTo(ZERO) > 0) {
            contributionMarginPercent = contributionMarginAmount
                    .multiply(ONE_HUNDRED)
                    .divide(netSellingPrice, CALCULATION_SCALE, ROUNDING_MODE);
        } else {
            contributionMarginPercent = ZERO;
            warnings.add("Receita líquida de venda menor ou igual a zero; margem de contribuição percentual fixada em 0.00%.");
        }

        // 8. Preço de Equilíbrio (Break-Even Price)
        // P_eq = (CD + DF) / (1 - DV% / 100) + desconto
        BigDecimal oneMinusVarRate = ONE.subtract(totalVarPercent.divide(ONE_HUNDRED, CALCULATION_SCALE, ROUNDING_MODE));
        BigDecimal breakEvenPrice = null;
        boolean isBreakEvenAttainable;

        if (oneMinusVarRate.compareTo(ZERO) <= 0) {
            isBreakEvenAttainable = false;
            warnings.add("Ponto de equilíbrio inatingível: deduções variáveis percentuais totais ("
                    + totalVarPercent.setScale(PRESENTATION_SCALE, ROUNDING_MODE)
                    + "%) são iguais ou superiores a 100%.");
        } else {
            BigDecimal breakEvenNet = totalDirectCosts.add(totalFixedDeductions)
                    .divide(oneMinusVarRate, CALCULATION_SCALE, ROUNDING_MODE);
            breakEvenPrice = breakEvenNet.add(discount);
            isBreakEvenAttainable = true;
        }

        // 9. Preço Mínimo
        BigDecimal minPrice = null;
        boolean isMinPriceAttainable;

        if (request.minMarginPercent() != null) {
            BigDecimal minMargin = sanitize(request.minMarginPercent());
            BigDecimal rateWithMin = totalVarPercent.add(minMargin);
            BigDecimal denomMin = ONE.subtract(rateWithMin.divide(ONE_HUNDRED, CALCULATION_SCALE, ROUNDING_MODE));

            if (denomMin.compareTo(ZERO) <= 0) {
                isMinPriceAttainable = false;
                warnings.add("Preço mínimo inatingível para a margem mínima informada ("
                        + minMargin.setScale(PRESENTATION_SCALE, ROUNDING_MODE) + "%).");
            } else {
                BigDecimal minPriceNet = totalDirectCosts.add(totalFixedDeductions)
                        .divide(denomMin, CALCULATION_SCALE, ROUNDING_MODE);
                minPrice = minPriceNet.add(discount);
                isMinPriceAttainable = true;
            }
        } else {
            minPrice = breakEvenPrice;
            isMinPriceAttainable = isBreakEvenAttainable;
        }

        // 10. Preço-Alvo
        BigDecimal targetPrice = null;
        boolean isTargetPriceAttainable;

        if (request.targetMarginPercent() != null) {
            BigDecimal targetMargin = sanitize(request.targetMarginPercent());
            BigDecimal rateWithTarget = totalVarPercent.add(targetMargin);
            BigDecimal denomTarget = ONE.subtract(rateWithTarget.divide(ONE_HUNDRED, CALCULATION_SCALE, ROUNDING_MODE));

            if (denomTarget.compareTo(ZERO) <= 0) {
                isTargetPriceAttainable = false;
                warnings.add("Preço-alvo inatingível para a margem alvo informada ("
                        + targetMargin.setScale(PRESENTATION_SCALE, ROUNDING_MODE) + "%).");
            } else {
                BigDecimal targetPriceNet = totalDirectCosts.add(totalFixedDeductions)
                        .divide(denomTarget, CALCULATION_SCALE, ROUNDING_MODE);
                targetPrice = targetPriceNet.add(discount);
                isTargetPriceAttainable = true;
            }
        } else if (request.targetMarginAmount() != null) {
            BigDecimal targetAmount = sanitize(request.targetMarginAmount());
            if (oneMinusVarRate.compareTo(ZERO) <= 0) {
                isTargetPriceAttainable = false;
                warnings.add("Preço-alvo monetário inatingível devido a deduções variáveis >= 100%.");
            } else {
                BigDecimal targetPriceNet = totalDirectCosts.add(totalFixedDeductions).add(targetAmount)
                        .divide(oneMinusVarRate, CALCULATION_SCALE, ROUNDING_MODE);
                targetPrice = targetPriceNet.add(discount);
                isTargetPriceAttainable = true;
            }
        } else {
            targetPrice = minPrice;
            isTargetPriceAttainable = isMinPriceAttainable;
        }

        // 11. Detalhamento de cada parcela deduzida
        List<PricingDeductionItem> deductionItems = new ArrayList<>();
        addDeductionItem(deductionItems, "Custo de Aquisição (CMV)", DeductionCategory.ACQUISITION_COST, acqCost, netSellingPrice, null, false);
        addDeductionItem(deductionItems, "Embalagem & Insumos", DeductionCategory.PACKAGING_COST, packagingCost, netSellingPrice, null, false);
        addDeductionItem(deductionItems, "Custos Adicionais Diretos", DeductionCategory.ADDITIONAL_DIRECT_COST, addCost, netSellingPrice, null, false);
        addDeductionItem(deductionItems, "Comissão do Marketplace", DeductionCategory.MARKETPLACE_COMMISSION, commissionAmount, netSellingPrice, commPercent, true);
        addDeductionItem(deductionItems, "Tarifa Fixa do Canal", DeductionCategory.MARKETPLACE_FIXED_FEE, fixedFee, netSellingPrice, null, false);
        addDeductionItem(deductionItems, "Frete Subsidiado", DeductionCategory.SUBSIDIZED_SHIPPING, subsidizedShipping, netSellingPrice, null, false);
        addDeductionItem(deductionItems, "Taxa Operacional de Logística", DeductionCategory.LOGISTICS_FEE, logisticsFee, netSellingPrice, null, false);
        addDeductionItem(deductionItems, "Impostos Faturados", DeductionCategory.TAX, taxAmount, netSellingPrice, taxPercent, true);
        addDeductionItem(deductionItems, "Outras Despesas Variáveis", DeductionCategory.OTHER_VARIABLE_EXPENSE, otherVarAmount, netSellingPrice, otherVarPercent, true);
        addDeductionItem(deductionItems, "Outras Deduções Fixas", DeductionCategory.OTHER_FIXED_DEDUCTION, otherFixed, netSellingPrice, null, false);

        if (discount.compareTo(ZERO) > 0) {
            addDeductionItem(deductionItems, "Desconto Promocional do Vendedor", DeductionCategory.PROMOTIONAL_DISCOUNT, discount, grossPrice, null, false);
        }

        return new PricingCalculationResult(
                grossPrice.setScale(PRESENTATION_SCALE, ROUNDING_MODE),
                discount.setScale(PRESENTATION_SCALE, ROUNDING_MODE),
                netSellingPrice.setScale(PRESENTATION_SCALE, ROUNDING_MODE),
                totalDirectCosts.setScale(PRESENTATION_SCALE, ROUNDING_MODE),
                totalVariableDeductions.setScale(PRESENTATION_SCALE, ROUNDING_MODE),
                totalFixedDeductions.setScale(PRESENTATION_SCALE, ROUNDING_MODE),
                totalDeductions.setScale(PRESENTATION_SCALE, ROUNDING_MODE),
                contributionMarginAmount.setScale(PRESENTATION_SCALE, ROUNDING_MODE),
                contributionMarginPercent.setScale(PRESENTATION_SCALE, ROUNDING_MODE),
                breakEvenPrice != null ? breakEvenPrice.setScale(PRESENTATION_SCALE, ROUNDING_MODE) : null,
                isBreakEvenAttainable,
                minPrice != null ? minPrice.setScale(PRESENTATION_SCALE, ROUNDING_MODE) : null,
                isMinPriceAttainable,
                targetPrice != null ? targetPrice.setScale(PRESENTATION_SCALE, ROUNDING_MODE) : null,
                isTargetPriceAttainable,
                request.currency(),
                FORMULA_VERSION,
                Instant.now(),
                deductionItems,
                assumptions,
                warnings
        );
    }

    /**
     * Calcula margens integrando o cadastro de custos do produto e as regras vigentes do marketplace.
     */
    public PricingCalculationResult calculateWithEffectiveFees(UUID tenantId, ProductMarginSimulationRequest simRequest) {
        Instant refDate = simRequest.referenceDate() != null ? simRequest.referenceDate() : Instant.now();

        // 1. Busca custo efetivo cadastrado
        Optional<ProductCostDto> costOpt = productCostService.getEffectiveCost(
                tenantId,
                simRequest.productId(),
                simRequest.variantId(),
                simRequest.companyId(),
                refDate
        );

        if (costOpt.isEmpty()) {
            throw new IllegalArgumentException("Nenhum custo cadastrado para o produto "
                    + simRequest.productId() + ". Por favor, cadastre a estrutura de custos antes de simular.");
        }

        ProductCostDto cost = costOpt.get();

        // 2. Resolve regras de taxas ativas para o canal
        FeeResolutionRequest feeRequest = new FeeResolutionRequest(
                simRequest.marketplace(),
                simRequest.listingType() != null ? simRequest.listingType() : "STANDARD",
                simRequest.category(),
                simRequest.grossSellingPrice(),
                refDate
        );

        FeeResolutionResult feeResult = feeResolutionService.resolveFees(tenantId, feeRequest);

        // Mapeia taxas resolvidas
        BigDecimal commissionPercent = ZERO;
        BigDecimal fixedFee = ZERO;
        BigDecimal logisticsFee = ZERO;
        BigDecimal otherFixed = ZERO;
        boolean hasManualFee = false;

        for (ResolvedFeeItem feeItem : feeResult.resolvedFees()) {
            if (feeItem.sourceType() == FeeSourceType.USER_CONFIGURED) {
                hasManualFee = true;
            }

            if (feeItem.feeType() == FeeType.COMMISSION) {
                if (feeItem.percentageRate() != null) {
                    commissionPercent = commissionPercent.add(feeItem.percentageRate());
                }
                if (feeItem.fixedAmount() != null) {
                    fixedFee = fixedFee.add(feeItem.fixedAmount());
                }
            } else if (feeItem.feeType() == FeeType.FIXED_FEE) {
                if (feeItem.fixedAmount() != null) {
                    fixedFee = fixedFee.add(feeItem.fixedAmount());
                }
            } else if (feeItem.feeType() == FeeType.LOGISTICS_FEE) {
                if (feeItem.fixedAmount() != null) {
                    logisticsFee = logisticsFee.add(feeItem.fixedAmount());
                }
            } else {
                if (feeItem.fixedAmount() != null) {
                    otherFixed = otherFixed.add(feeItem.fixedAmount());
                }
            }
        }

        // Monta requisição consolidada
        PricingCalculationRequest calcRequest = new PricingCalculationRequest(
                simRequest.grossSellingPrice(),
                simRequest.promotionalDiscount(),
                cost.acquisitionCost(),
                cost.packagingCost(),
                cost.additionalCost(),
                cost.subsidizedShippingCost(),
                commissionPercent,
                fixedFee,
                logisticsFee,
                cost.taxRatePercent(),
                cost.otherExpensesPercent(),
                otherFixed,
                simRequest.minMarginPercent(),
                simRequest.targetMarginPercent(),
                simRequest.targetMarginAmount(),
                cost.currency()
        );

        PricingCalculationResult result = calculateMargins(calcRequest);

        // Adiciona avisos específicos do marketplace/fontes
        List<String> combinedWarnings = new ArrayList<>(result.warnings());
        if (feeResult.hasMissingFees()) {
            combinedWarnings.add("Aviso: O marketplace " + simRequest.marketplace()
                    + " possui taxas obrigatórias ausentes (" + feeResult.missingFeeTypes()
                    + "). O cálculo considerou apenas regras cadastradas e não inventou percentuais.");
        }
        if (hasManualFee) {
            combinedWarnings.add("Aviso: Uma ou mais taxas aplicadas utilizam regras manuais não oficiais.");
        }

        return new PricingCalculationResult(
                result.grossSellingPrice(),
                result.promotionalDiscount(),
                result.netSellingPrice(),
                result.totalDirectCosts(),
                result.totalVariableDeductions(),
                result.totalFixedDeductions(),
                result.totalDeductions(),
                result.contributionMarginAmount(),
                result.contributionMarginPercent(),
                result.breakEvenPrice(),
                result.isBreakEvenAttainable(),
                result.minPrice(),
                result.isMinPriceAttainable(),
                result.targetPrice(),
                result.isTargetPriceAttainable(),
                result.currency(),
                result.formulaVersion(),
                result.calculatedAt(),
                result.deductionItems(),
                result.assumptions(),
                combinedWarnings
        );
    }

    private BigDecimal sanitize(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private BigDecimal calculatePercentAmount(BigDecimal base, BigDecimal percent) {
        if (base.compareTo(ZERO) <= 0 || percent.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return base.multiply(percent).divide(ONE_HUNDRED, CALCULATION_SCALE, ROUNDING_MODE);
    }

    private void addDeductionItem(
            List<PricingDeductionItem> items,
            String name,
            DeductionCategory category,
            BigDecimal amount,
            BigDecimal baseForPercent,
            BigDecimal appliedRate,
            boolean isPercentage
    ) {
        if (amount.compareTo(ZERO) <= 0 && (appliedRate == null || appliedRate.compareTo(ZERO) <= 0)) {
            return;
        }

        BigDecimal percentageOfRevenue = ZERO;
        if (baseForPercent.compareTo(ZERO) > 0) {
            percentageOfRevenue = amount.multiply(ONE_HUNDRED)
                    .divide(baseForPercent, PRESENTATION_SCALE, ROUNDING_MODE);
        }

        items.add(new PricingDeductionItem(
                name,
                category,
                amount.setScale(PRESENTATION_SCALE, ROUNDING_MODE),
                percentageOfRevenue,
                appliedRate != null ? appliedRate.setScale(PRESENTATION_SCALE, ROUNDING_MODE) : null,
                isPercentage
        ));
    }
}
