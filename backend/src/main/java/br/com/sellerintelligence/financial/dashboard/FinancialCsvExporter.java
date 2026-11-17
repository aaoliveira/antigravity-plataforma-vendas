package br.com.sellerintelligence.financial.dashboard;

import br.com.sellerintelligence.financial.domain.OrderFinancialCalculation;
import br.com.sellerintelligence.sync.domain.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class FinancialCsvExporter {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public String generateCsv(
            UUID tenantId,
            FinancialDashboardFilter filter,
            ZoneId zoneId,
            List<OrderFinancialCalculation> calculations,
            Map<UUID, Order> ordersMap,
            Map<UUID, String> storeNamesMap
    ) {
        StringBuilder sb = new StringBuilder();
        ZoneId effectiveZone = zoneId != null ? zoneId : ZoneId.of("America/Sao_Paulo");
        String generatedAt = ISO_FORMATTER.format(Instant.now().atZone(effectiveZone));

        // Metadata Header
        sb.append("# Relatório Financeiro e Margem de Contribuição — Seller Intelligence Platform\n");
        sb.append("# Data de Geração: ").append(generatedAt).append(" | Fuso Horário: ").append(effectiveZone.getId()).append("\n");
        sb.append("# Tenant ID: ").append(tenantId).append("\n");
        sb.append("# Filtros: De=").append(filter.from() != null ? filter.from().toString() : "Inicio")
                .append(" Ate=").append(filter.to() != null ? filter.to().toString() : "Fim")
                .append(" | Loja=").append(filter.storeId() != null ? filter.storeId().toString() : "TODAS")
                .append(" | Canal=").append(filter.marketplace() != null ? filter.marketplace().name() : "TODOS")
                .append("\n");
        sb.append("# Proveniência dos Dados: IMPORTED (Cálculos determinísticos em regime de competência)\n");
        sb.append("# Aviso Legal: Relatório gerencial. Não substitui escrituração contábil nem apuração fiscal oficial.\n");

        // CSV Header
        sb.append("ID Pedido,Data Pedido,Canal,Loja,Status Calculo,Qualidade,Receita Bruta (BRL),COGS (BRL),Taxas Canal (BRL),Frete Seller (BRL),Impostos (BRL),Embalagem (BRL),Outros (BRL),Margem Contribuicao (BRL),Margem (%)\n");

        for (OrderFinancialCalculation calc : calculations) {
            Order order = ordersMap.get(calc.getOrderId());
            String externalId = order != null ? order.getExternalOrderId() : calc.getOrderId().toString();
            String orderDateStr = order != null ? ISO_FORMATTER.format(order.getOrderDate().atZone(effectiveZone)) : "";
            String marketplaceStr = order != null && order.getMarketplace() != null ? order.getMarketplace().name() : "";
            String storeName = order != null ? storeNamesMap.getOrDefault(order.getStoreId(), "Loja") : "Loja";

            sb.append(escapeCsv(externalId)).append(",")
                    .append(escapeCsv(orderDateStr)).append(",")
                    .append(escapeCsv(marketplaceStr)).append(",")
                    .append(escapeCsv(storeName)).append(",")
                    .append(calc.getCalculationStatus().name()).append(",")
                    .append(calc.getCalculationQuality().name()).append(",")
                    .append(calc.getGrossRevenue().toPlainString()).append(",")
                    .append(calc.getTotalCogs().toPlainString()).append(",")
                    .append(calc.getTotalMarketplaceFees().toPlainString()).append(",")
                    .append(calc.getTotalShippingSeller().toPlainString()).append(",")
                    .append(calc.getTotalTaxes().toPlainString()).append(",")
                    .append(calc.getTotalPackaging().toPlainString()).append(",")
                    .append(calc.getTotalOtherCosts().toPlainString()).append(",")
                    .append(calc.getContributionMarginValue().toPlainString()).append(",")
                    .append(calc.getContributionMarginPercentage().toPlainString()).append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
