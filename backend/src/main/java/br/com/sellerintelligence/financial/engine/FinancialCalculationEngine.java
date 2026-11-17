package br.com.sellerintelligence.financial.engine;

import br.com.sellerintelligence.financial.domain.CalculationQuality;
import br.com.sellerintelligence.financial.domain.CalculationStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure deterministic financial calculation engine.
 * Adheres strictly to ADR DEC-005, ADR DEC-015 and anti-hallucination rules:
 * - Uses exclusively BigDecimal with RoundingMode.HALF_EVEN.
 * - ZERO float or double types.
 * - Explicit calculation memory line-by-line.
 * - COMPLETE, PARTIAL or SIMULATED quality flags.
 * - No AI participates in formula evaluation.
 */
@Component
public class FinancialCalculationEngine {

    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    public static final int MONEY_SCALE = 2;
    public static final int PERCENTAGE_SCALE = 4;
    public static final int INTERMEDIATE_SCALE = 6;

    public static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
    public static final BigDecimal ZERO_PERCENT = BigDecimal.ZERO.setScale(PERCENTAGE_SCALE, ROUNDING);
    public static final BigDecimal ONE = BigDecimal.ONE.setScale(INTERMEDIATE_SCALE, ROUNDING);
    public static final BigDecimal ONE_HUNDRED = new BigDecimal("100").setScale(MONEY_SCALE, ROUNDING);

    public ItemCalculationResult calculateItem(ItemCalculationInput input) {
        List<String> missingFields = new ArrayList<>();
        List<CalculationMemoryLine> lines = new ArrayList<>();

        int qty = Math.max(1, input.quantity());
        BigDecimal qtyBd = BigDecimal.valueOf(qty);

        // 1. Receita Bruta
        BigDecimal unitPrice = safeMoney(input.unitPrice());
        BigDecimal totalPrice = unitPrice.multiply(qtyBd).setScale(MONEY_SCALE, ROUNDING);
        lines.add(new CalculationMemoryLine(
                "RECEITA_BRUTA",
                unitPrice,
                qtyBd,
                totalPrice,
                "REVENUE",
                "Preço unitário x quantidade",
                "Receita bruta de venda faturada"
        ));

        // 2. Custo do Produto (COGS)
        BigDecimal unitCogs = input.unitCogs();
        BigDecimal totalCogs;
        if (unitCogs == null) {
            missingFields.add("COGS");
            totalCogs = ZERO_MONEY;
            lines.add(new CalculationMemoryLine(
                    "CUSTO_AQUISICAO_COGS",
                    ZERO_MONEY,
                    qtyBd,
                    ZERO_MONEY,
                    "DEDUCTION",
                    "Custo de SKU não cadastrado",
                    "CAMPO AUSENTE: Custo unitário não informado ou sem vigência"
            ));
        } else {
            totalCogs = unitCogs.multiply(qtyBd).setScale(MONEY_SCALE, ROUNDING);
            lines.add(new CalculationMemoryLine(
                    "CUSTO_AQUISICAO_COGS",
                    unitCogs,
                    qtyBd,
                    totalCogs,
                    "DEDUCTION",
                    "Tabela de custos de SKU",
                    "Custo unitário de aquisição/fabricação"
            ));
        }

        // 3. Comissão Marketplace
        BigDecimal commRate = input.commissionPercentage() != null ? input.commissionPercentage() : BigDecimal.ZERO;
        BigDecimal commFee = totalPrice.multiply(commRate).setScale(MONEY_SCALE, ROUNDING);
        lines.add(new CalculationMemoryLine(
                "COMISSAO_MARKETPLACE",
                totalPrice,
                commRate,
                commFee,
                "DEDUCTION",
                "Regra de Comissão do Canal",
                "Comissão percentual retida pelo marketplace"
        ));

        // 4. Tarifa Fixa Marketplace (cobrada se preço unitário for abaixo do limiar/threshold)
        BigDecimal fixedFee = ZERO_MONEY;
        if (input.fixedFee() != null && input.fixedFee().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal threshold = input.fixedFeeThreshold() != null ? input.fixedFeeThreshold() : new BigDecimal("79.00");
            if (unitPrice.compareTo(threshold) < 0) {
                fixedFee = input.fixedFee().multiply(qtyBd).setScale(MONEY_SCALE, ROUNDING);
                lines.add(new CalculationMemoryLine(
                        "TARIFA_FIXA_MARKETPLACE",
                        unitPrice,
                        input.fixedFee(),
                        fixedFee,
                        "DEDUCTION",
                        "Tarifa Fixa Canal (< R$ " + threshold.setScale(2, ROUNDING) + ")",
                        "Taxa fixa por item para tíquetes baixos"
                ));
            } else {
                lines.add(new CalculationMemoryLine(
                        "TARIFA_FIXA_MARKETPLACE",
                        unitPrice,
                        BigDecimal.ZERO,
                        ZERO_MONEY,
                        "DEDUCTION",
                        "Isento (>= R$ " + threshold.setScale(2, ROUNDING) + ")",
                        "Tíquete unitário acima do limiar da tarifa fixa"
                ));
            }
        }
        BigDecimal totalMarketplaceFees = commFee.add(fixedFee).setScale(MONEY_SCALE, ROUNDING);

        // 5. Frete Suportado pelo Seller
        BigDecimal shippingUnit = input.shippingSeller() != null ? input.shippingSeller() : ZERO_MONEY;
        BigDecimal totalShipping = shippingUnit.multiply(qtyBd).setScale(MONEY_SCALE, ROUNDING);
        lines.add(new CalculationMemoryLine(
                "FRETE_SELLER",
                shippingUnit,
                qtyBd,
                totalShipping,
                "DEDUCTION",
                "Coparticipação de Envio / Frete Grátis",
                "Custo logístico arcado pelo seller"
        ));

        // 6. Custo de Embalagem
        BigDecimal packUnit = input.packagingCost() != null ? input.packagingCost() : ZERO_MONEY;
        BigDecimal totalPackaging = packUnit.multiply(qtyBd).setScale(MONEY_SCALE, ROUNDING);
        lines.add(new CalculationMemoryLine(
                "CUSTO_EMBALAGEM",
                packUnit,
                qtyBd,
                totalPackaging,
                "DEDUCTION",
                "Insumos de Expedição",
                "Caixa, fita, plástico-bolha e etiquetas"
        ));

        // 7. Impostos Parametrizados
        BigDecimal taxRate = input.taxPercentage();
        BigDecimal totalTax;
        if (taxRate == null) {
            missingFields.add("TAX");
            totalTax = ZERO_MONEY;
            lines.add(new CalculationMemoryLine(
                    "IMPOSTOS_PARAMETRIZADOS",
                    totalPrice,
                    BigDecimal.ZERO,
                    ZERO_MONEY,
                    "DEDUCTION",
                    "Alíquota não informada pelo seller",
                    "CAMPO AUSENTE: Alíquota fiscal não configurada. Cálculo gerencial incompleto."
            ));
        } else {
            totalTax = totalPrice.multiply(taxRate).setScale(MONEY_SCALE, ROUNDING);
            lines.add(new CalculationMemoryLine(
                    "IMPOSTOS_PARAMETRIZADOS",
                    totalPrice,
                    taxRate,
                    totalTax,
                    "DEDUCTION",
                    "Alíquota Efetiva Parametrizada",
                    "Tributo sobre a venda faturada conforme alíquota configurada"
            ));
        }

        // 8. Outros Custos Variáveis
        BigDecimal otherPct = input.otherVariablePercentage() != null ? input.otherVariablePercentage() : BigDecimal.ZERO;
        BigDecimal otherFixed = input.otherVariableFixed() != null ? input.otherVariableFixed() : ZERO_MONEY;
        BigDecimal totalOther = totalPrice.multiply(otherPct).add(otherFixed.multiply(qtyBd)).setScale(MONEY_SCALE, ROUNDING);
        lines.add(new CalculationMemoryLine(
                "OUTROS_CUSTOS_VARIAVEIS",
                totalPrice,
                otherPct,
                totalOther,
                "DEDUCTION",
                "Outros Custos Parametrizados",
                "Comissões de equipe, chargebacks ou taxas operacionais adicionais"
        ));

        // 9. Margem de Contribuição
        BigDecimal totalDeductions = totalCogs
                .add(totalMarketplaceFees)
                .add(totalShipping)
                .add(totalPackaging)
                .add(totalTax)
                .add(totalOther)
                .setScale(MONEY_SCALE, ROUNDING);

        BigDecimal marginValue = totalPrice.subtract(totalDeductions).setScale(MONEY_SCALE, ROUNDING);
        BigDecimal marginPct = ZERO_PERCENT;
        if (totalPrice.compareTo(BigDecimal.ZERO) > 0) {
            marginPct = marginValue.divide(totalPrice, INTERMEDIATE_SCALE, ROUNDING)
                    .multiply(ONE_HUNDRED)
                    .setScale(PERCENTAGE_SCALE, ROUNDING);
        }

        lines.add(new CalculationMemoryLine(
                "MARGEM_CONTRIBUICAO_LIQUIDA",
                totalPrice,
                marginPct,
                marginValue,
                "RESULT",
                "Receita Bruta - Total Deduções",
                "Margem de contribuição operacional antes de custos fixos"
        ));

        CalculationQuality quality = missingFields.isEmpty() ? CalculationQuality.COMPLETE : CalculationQuality.PARTIAL;
        String assumptions = missingFields.isEmpty() ?
                "Todas as premissas e componentes foram informados." :
                "Cálculo gerencial preliminar. Não representa lucro líquido real nem apuração fiscal oficial. Campos ausentes: " + String.join(", ", missingFields);

        CalculationMemory memory = new CalculationMemory(
                quality,
                lines,
                missingFields,
                assumptions,
                String.format("Margem: R$ %s (%s%%)", marginValue, marginPct.setScale(2, ROUNDING))
        );

        return new ItemCalculationResult(
                input.orderItemId(),
                input.sku(),
                unitPrice,
                qty,
                totalPrice,
                unitCogs,
                totalCogs,
                totalMarketplaceFees,
                totalShipping,
                totalTax,
                totalPackaging,
                totalOther,
                marginValue,
                marginPct,
                quality,
                memory
        );
    }

    public OrderCalculationResult calculateOrder(OrderCalculationInput input) {
        if (input.status() == CalculationStatus.CANCELLED) {
            return new OrderCalculationResult(
                    input.orderId(),
                    input.ruleSetId(),
                    input.ruleVersion(),
                    CalculationStatus.CANCELLED,
                    CalculationQuality.COMPLETE,
                    ZERO_MONEY,
                    ZERO_MONEY,
                    ZERO_MONEY,
                    ZERO_MONEY,
                    ZERO_MONEY,
                    ZERO_MONEY,
                    ZERO_MONEY,
                    ZERO_MONEY,
                    ZERO_MONEY,
                    ZERO_PERCENT,
                    List.of(),
                    "Pedido cancelado. Receita e custos revertidos conforme política de reconhecimento.",
                    List.of()
            );
        }

        List<ItemCalculationResult> itemResults = new ArrayList<>();
        List<String> combinedMissing = new ArrayList<>();

        BigDecimal grossRevenue = ZERO_MONEY;
        BigDecimal totalCogs = ZERO_MONEY;
        BigDecimal totalFees = ZERO_MONEY;
        BigDecimal totalShipping = input.orderLevelShippingSeller() != null ? input.orderLevelShippingSeller() : ZERO_MONEY;
        BigDecimal totalPackaging = input.orderLevelPackagingCost() != null ? input.orderLevelPackagingCost() : ZERO_MONEY;
        BigDecimal totalTaxes = ZERO_MONEY;
        BigDecimal totalOther = input.orderLevelOtherCosts() != null ? input.orderLevelOtherCosts() : ZERO_MONEY;

        for (ItemCalculationInput itemInput : input.items()) {
            ItemCalculationResult res = calculateItem(itemInput);
            itemResults.add(res);

            grossRevenue = grossRevenue.add(res.totalPrice());
            totalCogs = totalCogs.add(res.totalCogs());
            totalFees = totalFees.add(res.marketplaceFee());
            totalShipping = totalShipping.add(res.shippingSeller());
            totalPackaging = totalPackaging.add(res.packagingCost());
            totalTaxes = totalTaxes.add(res.taxValue());
            totalOther = totalOther.add(res.otherCosts());

            for (String mf : res.memory().missingFields()) {
                if (!combinedMissing.contains(mf)) {
                    combinedMissing.add(mf);
                }
            }
        }

        BigDecimal netRevenue = grossRevenue.subtract(totalTaxes).setScale(MONEY_SCALE, ROUNDING);
        BigDecimal totalDeductions = totalCogs
                .add(totalFees)
                .add(totalShipping)
                .add(totalPackaging)
                .add(totalTaxes)
                .add(totalOther)
                .setScale(MONEY_SCALE, ROUNDING);

        BigDecimal marginValue = grossRevenue.subtract(totalDeductions).setScale(MONEY_SCALE, ROUNDING);
        BigDecimal marginPct = ZERO_PERCENT;
        if (grossRevenue.compareTo(BigDecimal.ZERO) > 0) {
            marginPct = marginValue.divide(grossRevenue, INTERMEDIATE_SCALE, ROUNDING)
                    .multiply(ONE_HUNDRED)
                    .setScale(PERCENTAGE_SCALE, ROUNDING);
        }

        CalculationQuality quality = combinedMissing.isEmpty() ? CalculationQuality.COMPLETE : CalculationQuality.PARTIAL;
        String assumptions = combinedMissing.isEmpty() ?
                "Cálculo consolidado com todas as variáveis preenchidas." :
                "Cálculo consolidado parcial. Campos ausentes nos itens: " + String.join(", ", combinedMissing);

        return new OrderCalculationResult(
                input.orderId(),
                input.ruleSetId(),
                input.ruleVersion(),
                input.status() != null ? input.status() : CalculationStatus.CLOSED,
                quality,
                grossRevenue.setScale(MONEY_SCALE, ROUNDING),
                netRevenue.setScale(MONEY_SCALE, ROUNDING),
                totalCogs.setScale(MONEY_SCALE, ROUNDING),
                totalFees.setScale(MONEY_SCALE, ROUNDING),
                totalShipping.setScale(MONEY_SCALE, ROUNDING),
                totalTaxes.setScale(MONEY_SCALE, ROUNDING),
                totalPackaging.setScale(MONEY_SCALE, ROUNDING),
                totalOther.setScale(MONEY_SCALE, ROUNDING),
                marginValue.setScale(MONEY_SCALE, ROUNDING),
                marginPct.setScale(PERCENTAGE_SCALE, ROUNDING),
                combinedMissing,
                assumptions,
                itemResults
        );
    }

    public PriceSimulationResult simulatePrice(PriceSimulationInput input, BigDecimal trialPrice) {
        List<String> missingFields = new ArrayList<>();
        List<CalculationMemoryLine> lines = new ArrayList<>();

        BigDecimal cogs = input.unitCogs();
        if (cogs == null) {
            missingFields.add("COGS");
            cogs = ZERO_MONEY;
        }

        BigDecimal pack = input.packagingCost() != null ? input.packagingCost() : ZERO_MONEY;
        BigDecimal shipping = input.shippingSeller() != null ? input.shippingSeller() : ZERO_MONEY;
        BigDecimal fixedFee = input.fixedFee() != null ? input.fixedFee() : ZERO_MONEY;
        BigDecimal fixedThreshold = input.fixedFeeThreshold() != null ? input.fixedFeeThreshold() : new BigDecimal("79.00");

        BigDecimal commRate = input.commissionPercentage() != null ? input.commissionPercentage() : BigDecimal.ZERO;
        BigDecimal taxRate = input.taxPercentage();
        if (taxRate == null) {
            missingFields.add("TAX");
            taxRate = BigDecimal.ZERO;
        }
        BigDecimal otherPct = input.otherVariablePercentage() != null ? input.otherVariablePercentage() : BigDecimal.ZERO;
        BigDecimal otherFixed = input.otherVariableFixed() != null ? input.otherVariableFixed() : ZERO_MONEY;

        BigDecimal totalPctDeductions = commRate.add(taxRate).add(otherPct).setScale(INTERMEDIATE_SCALE, ROUNDING);
        BigDecimal targetMarginPct = input.targetMarginPercentage() != null ?
                input.targetMarginPercentage().divide(ONE_HUNDRED, INTERMEDIATE_SCALE, ROUNDING) : null;
        BigDecimal targetMarginAmt = input.targetMarginAmount() != null ? input.targetMarginAmount() : null;

        // 1. Cálculo de Preço Mínimo (Break-even):
        // P_min = (COGS + Packaging + Shipping + OtherFixed + FixedFee?) / (1 - TotalPct)
        BigDecimal minimumPrice = calculateBreakevenPrice(cogs, pack, shipping, otherFixed, fixedFee, fixedThreshold, totalPctDeductions);

        // 2. Preço Alvo
        BigDecimal targetPrice = minimumPrice;
        if (targetMarginPct != null) {
            BigDecimal denominator = ONE.subtract(totalPctDeductions).subtract(targetMarginPct);
            if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                // Determine if fixed fee applies
                BigDecimal fixedCostsWithoutFee = cogs.add(pack).add(shipping).add(otherFixed);
                BigDecimal fixedCostsWithFee = fixedCostsWithoutFee.add(fixedFee);

                BigDecimal trialWith = fixedCostsWithFee.divide(denominator, MONEY_SCALE, ROUNDING);
                if (trialWith.compareTo(fixedThreshold) < 0) {
                    targetPrice = trialWith;
                } else {
                    targetPrice = fixedCostsWithoutFee.divide(denominator, MONEY_SCALE, ROUNDING);
                }
            }
        } else if (targetMarginAmt != null) {
            BigDecimal denominator = ONE.subtract(totalPctDeductions);
            if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal fixedCostsWithoutFee = cogs.add(pack).add(shipping).add(otherFixed).add(targetMarginAmt);
                BigDecimal fixedCostsWithFee = fixedCostsWithoutFee.add(fixedFee);

                BigDecimal trialWith = fixedCostsWithFee.divide(denominator, MONEY_SCALE, ROUNDING);
                if (trialWith.compareTo(fixedThreshold) < 0) {
                    targetPrice = trialWith;
                } else {
                    targetPrice = fixedCostsWithoutFee.divide(denominator, MONEY_SCALE, ROUNDING);
                }
            }
        }

        // 3. Simulação com o preço sugerido/testado
        BigDecimal effectiveSimulatedPrice = (trialPrice != null && trialPrice.compareTo(BigDecimal.ZERO) > 0) ?
                trialPrice : targetPrice;

        ItemCalculationInput simItemInput = new ItemCalculationInput(
                null,
                "SIMULATED_SKU",
                effectiveSimulatedPrice,
                1,
                input.unitCogs(),
                input.commissionPercentage(),
                input.fixedFee(),
                input.fixedFeeThreshold(),
                input.shippingSeller(),
                input.packagingCost(),
                input.taxPercentage(),
                input.otherVariablePercentage(),
                input.otherVariableFixed()
        );

        ItemCalculationResult simResult = calculateItem(simItemInput);

        CalculationQuality quality = missingFields.isEmpty() ? CalculationQuality.SIMULATED : CalculationQuality.PARTIAL;
        String assumptions = missingFields.isEmpty() ?
                "Simulação determinística baseada nas taxas fornecidas." :
                "Simulação parcial com dados ausentes: " + String.join(", ", missingFields);

        return new PriceSimulationResult(
                minimumPrice.setScale(MONEY_SCALE, ROUNDING),
                targetPrice.setScale(MONEY_SCALE, ROUNDING),
                effectiveSimulatedPrice.setScale(MONEY_SCALE, ROUNDING),
                simResult.contributionMarginValue(),
                simResult.contributionMarginPercentage(),
                quality,
                missingFields,
                assumptions,
                simResult.memory()
        );
    }

    private BigDecimal calculateBreakevenPrice(
            BigDecimal cogs, BigDecimal pack, BigDecimal shipping, BigDecimal otherFixed,
            BigDecimal fixedFee, BigDecimal fixedThreshold, BigDecimal totalPctDeductions
    ) {
        BigDecimal denominator = ONE.subtract(totalPctDeductions);
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            // Deduções percentuais atingem ou excedem 100%
            return cogs.add(pack).add(shipping).add(fixedFee).add(otherFixed).setScale(MONEY_SCALE, ROUNDING);
        }

        BigDecimal fixedWithoutFee = cogs.add(pack).add(shipping).add(otherFixed);
        BigDecimal fixedWithFee = fixedWithoutFee.add(fixedFee);

        BigDecimal priceWith = fixedWithFee.divide(denominator, MONEY_SCALE, ROUNDING);
        if (priceWith.compareTo(fixedThreshold) < 0) {
            return priceWith;
        } else {
            return fixedWithoutFee.divide(denominator, MONEY_SCALE, ROUNDING);
        }
    }

    private BigDecimal safeMoney(BigDecimal val) {
        return val != null ? val.setScale(MONEY_SCALE, ROUNDING) : ZERO_MONEY;
    }
}
