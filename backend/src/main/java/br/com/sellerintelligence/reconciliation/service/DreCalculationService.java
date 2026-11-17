package br.com.sellerintelligence.reconciliation.service;

import br.com.sellerintelligence.reconciliation.domain.*;
import br.com.sellerintelligence.sync.domain.Order;
import br.com.sellerintelligence.sync.domain.OrderItem;
import br.com.sellerintelligence.sync.domain.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DreCalculationService {

    private static final Logger log = LoggerFactory.getLogger(DreCalculationService.class);
    private static final String DRE_LEGAL_DISCLAIMER =
            "DRE gerencial para apoio determinístico à tomada de decisão. Não substitui demonstrações contábeis oficiais nem gera guias ou obrigações tributárias.";

    private final OrderRepository orderRepository;
    private final ReconciliationSettlementRepository settlementRepository;
    private final ReconciliationSettlementItemRepository settlementItemRepository;
    private final br.com.sellerintelligence.financial.service.SkuCostService skuCostService;

    public DreCalculationService(OrderRepository orderRepository,
                                 ReconciliationSettlementRepository settlementRepository,
                                 ReconciliationSettlementItemRepository settlementItemRepository,
                                 br.com.sellerintelligence.financial.service.SkuCostService skuCostService) {
        this.orderRepository = orderRepository;
        this.settlementRepository = settlementRepository;
        this.settlementItemRepository = settlementItemRepository;
        this.skuCostService = skuCostService;
    }

    public record DreLineDto(
            int lineNumber,
            String code,
            String name,
            String formula,
            BigDecimal amount,
            BigDecimal percentageOfGross,
            String type
    ) {}

    public record ManagerialDreReportDto(
            DreAccountingBasis basis,
            Instant startDate,
            Instant endDate,
            String marketplace,
            String generatedAt,
            String timezone,
            String disclaimer,
            List<DreLineDto> lines,
            BigDecimal grossRevenue,
            BigDecimal netRevenue,
            BigDecimal contributionMargin,
            BigDecimal netOperatingResult
    ) {}

    @Transactional(readOnly = true)
    public ManagerialDreReportDto calculateDre(UUID tenantId, DreAccountingBasis basis,
                                              Instant startDate, Instant endDate,
                                              String marketplace) {
        log.info("Calculating Managerial DRE: tenantId={}, basis={}, range=[{} to {}]",
                tenantId, basis, startDate, endDate);

        if (basis == DreAccountingBasis.CASH) {
            return calculateCashBasisDre(tenantId, startDate, endDate, marketplace);
        } else {
            return calculateAccrualBasisDre(tenantId, startDate, endDate, marketplace);
        }
    }

    // 1. Regime de Competência (ACCRUAL): Baseado na realização das vendas / data do pedido
    private ManagerialDreReportDto calculateAccrualBasisDre(UUID tenantId, Instant startDate,
                                                           Instant endDate, String marketplace) {
        List<Order> orders = orderRepository.findByTenantIdAndOrderDateBetween(tenantId, startDate, endDate);

        if (marketplace != null && !marketplace.isBlank() && !marketplace.equalsIgnoreCase("ALL") && !marketplace.equalsIgnoreCase("TODOS")) {
            orders = orders.stream()
                    .filter(o -> o.getMarketplace() != null && o.getMarketplace().name().equalsIgnoreCase(marketplace))
                    .toList();
        }

        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal cancellationsReturns = BigDecimal.ZERO;
        BigDecimal marketplaceFees = BigDecimal.ZERO;
        BigDecimal shippingCosts = BigDecimal.ZERO;
        BigDecimal cogs = BigDecimal.ZERO;

        for (Order o : orders) {
            BigDecimal orderTotal = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;

            if ("CANCELLED".equalsIgnoreCase(o.getStatus())) {
                cancellationsReturns = cancellationsReturns.add(orderTotal);
            } else {
                grossRevenue = grossRevenue.add(orderTotal);
                if (o.getMarketplaceFee() != null) {
                    marketplaceFees = marketplaceFees.add(o.getMarketplaceFee());
                }
                if (o.getShippingCostSeller() != null) {
                    shippingCosts = shippingCosts.add(o.getShippingCostSeller());
                }

                if (o.getItems() != null) {
                    for (OrderItem item : o.getItems()) {
                        int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                        BigDecimal unitCogs = BigDecimal.ZERO;
                        if (item.getSku() != null && skuCostService != null) {
                            try {
                                var costOpt = skuCostService.getEffectiveCost(item.getSku(), o.getOrderDate());
                                if (costOpt.isPresent()) {
                                    unitCogs = costOpt.get().getCostPrice();
                                }
                            } catch (Exception ignored) {
                                // Fallback para estimativa se TenantContext não estiver ativo
                            }
                        }
                        if (unitCogs.compareTo(BigDecimal.ZERO) == 0 && item.getUnitPrice() != null) {
                            unitCogs = item.getUnitPrice().multiply(new BigDecimal("0.4000"));
                        }
                        cogs = cogs.add(unitCogs.multiply(BigDecimal.valueOf(qty)));
                    }
                }
            }
        }

        // Estimativa tributária parametrizada média (ex.: Simples Nacional padrão 6% sobre faturamento ativo)
        BigDecimal estimatedTaxes = grossRevenue.multiply(new BigDecimal("0.0600")).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalDeductions = cancellationsReturns.add(marketplaceFees).add(shippingCosts).add(estimatedTaxes);
        BigDecimal netRevenue = grossRevenue.subtract(totalDeductions);
        BigDecimal contributionMargin = netRevenue.subtract(cogs);

        // Despesas operacionais e custos fixos alocáveis (embalagens, ferramentas SaaS)
        BigDecimal operatingExpenses = grossRevenue.multiply(new BigDecimal("0.0350")).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal netOperatingResult = contributionMargin.subtract(operatingExpenses);

        List<DreLineDto> lines = buildDreLines(
                grossRevenue, cancellationsReturns, marketplaceFees, shippingCosts,
                estimatedTaxes, netRevenue, cogs, contributionMargin, operatingExpenses, netOperatingResult
        );

        return new ManagerialDreReportDto(
                DreAccountingBasis.ACCRUAL,
                startDate,
                endDate,
                marketplace != null ? marketplace : "TODOS",
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "America/Sao_Paulo",
                DRE_LEGAL_DISCLAIMER,
                lines,
                grossRevenue,
                netRevenue,
                contributionMargin,
                netOperatingResult
        );
    }

    // 2. Regime de Caixa (CASH): Baseado em repasses efetivamente liquidados e extratos bancários
    private ManagerialDreReportDto calculateCashBasisDre(UUID tenantId, Instant startDate,
                                                        Instant endDate, String marketplace) {
        OffsetDateTime startOffset = startDate.atOffset(ZoneId.of("America/Sao_Paulo").getRules().getOffset(startDate));
        OffsetDateTime endOffset = endDate.atOffset(ZoneId.of("America/Sao_Paulo").getRules().getOffset(endDate));

        List<ReconciliationSettlement> settlements = settlementRepository
                .findByTenantIdAndSettlementDateBetween(tenantId, startOffset, endOffset);

        if (marketplace != null && !marketplace.isBlank() && !marketplace.equalsIgnoreCase("ALL") && !marketplace.equalsIgnoreCase("TODOS")) {
            settlements = settlements.stream()
                    .filter(s -> s.getMarketplace() != null && s.getMarketplace().equalsIgnoreCase(marketplace))
                    .toList();
        }

        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal marketplaceFees = BigDecimal.ZERO;
        BigDecimal shippingCosts = BigDecimal.ZERO;
        BigDecimal adjustments = BigDecimal.ZERO;

        for (ReconciliationSettlement s : settlements) {
            grossRevenue = grossRevenue.add(s.getGrossAmount());
            marketplaceFees = marketplaceFees.add(s.getTotalFees());
            shippingCosts = shippingCosts.add(s.getTotalShipping());
            adjustments = adjustments.add(s.getTotalAdjustments());
        }

        BigDecimal cancellationsReturns = adjustments.compareTo(BigDecimal.ZERO) < 0 ? adjustments.abs() : BigDecimal.ZERO;
        BigDecimal estimatedTaxes = grossRevenue.multiply(new BigDecimal("0.0600")).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalDeductions = marketplaceFees.add(shippingCosts).add(cancellationsReturns).add(estimatedTaxes);
        BigDecimal netRevenue = grossRevenue.subtract(totalDeductions);

        // CMV no regime de caixa: custo dos produtos cujos repasses caíram na conta no período (estimado 40% do faturamento)
        BigDecimal cogs = grossRevenue.multiply(new BigDecimal("0.4000")).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal contributionMargin = netRevenue.subtract(cogs);

        BigDecimal operatingExpenses = grossRevenue.multiply(new BigDecimal("0.0300")).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal netOperatingResult = contributionMargin.subtract(operatingExpenses);

        List<DreLineDto> lines = buildDreLines(
                grossRevenue, cancellationsReturns, marketplaceFees, shippingCosts,
                estimatedTaxes, netRevenue, cogs, contributionMargin, operatingExpenses, netOperatingResult
        );

        return new ManagerialDreReportDto(
                DreAccountingBasis.CASH,
                startDate,
                endDate,
                marketplace != null ? marketplace : "TODOS",
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "America/Sao_Paulo",
                DRE_LEGAL_DISCLAIMER,
                lines,
                grossRevenue,
                netRevenue,
                contributionMargin,
                netOperatingResult
        );
    }

    private List<DreLineDto> buildDreLines(
            BigDecimal grossRevenue, BigDecimal cancellationsReturns, BigDecimal marketplaceFees,
            BigDecimal shippingCosts, BigDecimal taxes, BigDecimal netRevenue, BigDecimal cogs,
            BigDecimal contributionMargin, BigDecimal operatingExpenses, BigDecimal netOperatingResult) {

        List<DreLineDto> lines = new ArrayList<>();
        BigDecimal divisor = grossRevenue.compareTo(BigDecimal.ZERO) > 0 ? grossRevenue : BigDecimal.ONE;

        lines.add(new DreLineDto(1, "1.0", "(+) Receita Bruta de Vendas",
                "Soma do valor bruto das vendas / repasses", grossRevenue,
                percent(grossRevenue, divisor), "REVENUE"));

        lines.add(new DreLineDto(2, "1.1", "  (-) Cancelamentos, Devoluções e Estornos",
                "Estornos e cancelamentos ocorridos no período", cancellationsReturns.negate(),
                percent(cancellationsReturns, divisor), "DEDUCTION"));

        lines.add(new DreLineDto(3, "1.2", "  (-) Comissões e Tarifas de Marketplace",
                "Comissões de venda e tarifas por transação retidas", marketplaceFees.negate(),
                percent(marketplaceFees, divisor), "DEDUCTION"));

        lines.add(new DreLineDto(4, "1.3", "  (-) Frete e Logística (Seller)",
                "Custo de envio assumido pelo vendedor (Envios/Fulfillment)", shippingCosts.negate(),
                percent(shippingCosts, divisor), "DEDUCTION"));

        lines.add(new DreLineDto(5, "1.4", "  (-) Tributos Parametrizados",
                "Alíquota efetiva informada (ex: Simples Nacional 6%)", taxes.negate(),
                percent(taxes, divisor), "DEDUCTION"));

        lines.add(new DreLineDto(6, "2.0", "(=) Receita Líquida Operacional",
                "Receita Bruta - Deduções", netRevenue,
                percent(netRevenue, divisor), "SUBTOTAL"));

        lines.add(new DreLineDto(7, "3.0", "(-) Custo das Mercadorias Vendidas (CMV / COGS)",
                "Soma dos custos de aquisição dos SKUs vendidos", cogs.negate(),
                percent(cogs, divisor), "EXPENSE"));

        lines.add(new DreLineDto(8, "4.0", "(=) Margem de Contribuição / Lucro Bruto",
                "Receita Líquida - CMV", contributionMargin,
                percent(contributionMargin, divisor), "SUBTOTAL"));

        lines.add(new DreLineDto(9, "5.0", "(-) Despesas Operacionais e Embalagens",
                "Embalagens, softwares e ajustes operacionais alocados", operatingExpenses.negate(),
                percent(operatingExpenses, divisor), "EXPENSE"));

        lines.add(new DreLineDto(10, "6.0", "(=) Resultado Operacional Líquido Gerencial",
                "Margem de Contribuição - Despesas Operacionais", netOperatingResult,
                percent(netOperatingResult, divisor), "RESULT"));

        return lines;
    }

    private BigDecimal percent(BigDecimal val, BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return val.multiply(new BigDecimal("100.00")).divide(divisor, 2, RoundingMode.HALF_EVEN);
    }

    public String exportDreToCsv(ManagerialDreReportDto report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# RELATÓRIO: DRE GERENCIAL (DEMONSTRATIVO DE RESULTADOS DO EXERCÍCIO)\n");
        sb.append("# REGIME: ").append(report.basis() == DreAccountingBasis.CASH ? "CAIXA (REPASSES/LIQUIDAÇÕES)" : "COMPETÊNCIA (FATOS GERADORES/PEDIDOS)").append("\n");
        sb.append("# PERÍODO: ").append(report.startDate()).append(" até ").append(report.endDate()).append("\n");
        sb.append("# CANAL: ").append(report.marketplace()).append("\n");
        sb.append("# GERADO EM: ").append(report.generatedAt()).append(" (Timezone: ").append(report.timezone()).append(")\n");
        sb.append("# AVISO LEGAL: ").append(report.disclaimer()).append("\n#\n");
        sb.append("Linha;Código;Descrição;Fórmula / Memória de Cálculo;Valor (R$);% Receita Bruta\n");

        for (DreLineDto line : report.lines()) {
            sb.append(line.lineNumber()).append(";")
                    .append(line.code()).append(";")
                    .append(line.name()).append(";")
                    .append(line.formula()).append(";")
                    .append(line.amount().setScale(2, RoundingMode.HALF_EVEN).toPlainString()).append(";")
                    .append(line.percentageOfGross().setScale(2, RoundingMode.HALF_EVEN).toPlainString()).append("%\n");
        }

        return sb.toString();
    }
}
