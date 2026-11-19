package com.sellerintelligence.modules.reporting.provider;

import com.sellerintelligence.modules.financialanalytics.entity.OrderFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.model.ReconciliationStatus;
import com.sellerintelligence.modules.financialanalytics.repository.OrderFinancialFactRepository;
import com.sellerintelligence.modules.reporting.model.ExportDataPage;
import com.sellerintelligence.modules.reporting.model.ExportDataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class FinancialFactsExportDataProvider implements ExportDataProvider {

    private static final List<String> HEADERS = List.of(
            "ID Fato",
            "ID Pedido",
            "Pedido Externo",
            "Marketplace",
            "Status Pedido",
            "Data Pedido",
            "Receita Bruta (BRL)",
            "Descontos (BRL)",
            "Cancelamentos (BRL)",
            "Devoluções (BRL)",
            "Comissão (BRL)",
            "Tarifa Fixa (BRL)",
            "Frete (BRL)",
            "Custo Produto (BRL)",
            "Imposto Informado (BRL)",
            "Margem Contribuição (BRL)",
            "Status Reconciliação",
            "Liquidação Externa",
            "Versão Cálculo"
    );

    private final OrderFinancialFactRepository repository;

    public FinancialFactsExportDataProvider(OrderFinancialFactRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExportDataset supportedDataset() {
        return ExportDataset.FINANCIAL_FACTS;
    }

    @Override
    public ExportDataPage fetchTenantData(UUID tenantId, Map<String, Object> filterParams, int maxRows) {
        UUID companyId = parseUuid(filterParams.get("companyId"));
        String marketplace = parseString(filterParams.get("marketplace"));
        ReconciliationStatus status = parseEnum(filterParams.get("status"), ReconciliationStatus.class);
        Instant startDate = parseInstant(filterParams.get("startDate"));
        Instant endDate = parseInstant(filterParams.get("endDate"));

        int limit = Math.max(1, Math.min(maxRows, 50000));
        Page<OrderFinancialFactRecord> page = repository.searchFacts(
                tenantId,
                companyId,
                marketplace,
                status,
                startDate,
                endDate,
                PageRequest.of(0, limit)
        );

        List<List<Object>> rows = new ArrayList<>();
        for (OrderFinancialFactRecord fact : page.getContent()) {
            rows.add(List.of(
                    fact.getId().toString(),
                    fact.getOrderId().toString(),
                    fact.getExternalOrderId() != null ? fact.getExternalOrderId() : "",
                    fact.getMarketplace() != null ? fact.getMarketplace() : "",
                    fact.getOrderStatus() != null ? fact.getOrderStatus() : "",
                    fact.getOrderDate() != null ? fact.getOrderDate().toString() : "",
                    fact.getGrossRevenue() != null ? fact.getGrossRevenue() : "",
                    fact.getDiscountAmount() != null ? fact.getDiscountAmount() : "",
                    fact.getCancellationAmount() != null ? fact.getCancellationAmount() : "",
                    fact.getReturnAmount() != null ? fact.getReturnAmount() : "",
                    fact.getMarketplaceFee() != null ? fact.getMarketplaceFee() : "",
                    fact.getFixedFee() != null ? fact.getFixedFee() : "",
                    fact.getShippingCost() != null ? fact.getShippingCost() : "",
                    fact.getProductCost() != null ? fact.getProductCost() : "",
                    fact.getTaxAmount() != null ? fact.getTaxAmount() : "",
                    fact.getContributionMarginAmount() != null ? fact.getContributionMarginAmount() : "",
                    fact.getReconciliationStatus() != null ? fact.getReconciliationStatus().name() : "",
                    fact.getSettlementExternalId() != null ? fact.getSettlementExternalId() : "",
                    fact.getCalculationVersion() != null ? fact.getCalculationVersion() : ""
            ));
        }

        boolean isTruncated = page.getTotalElements() > limit;
        return new ExportDataPage(HEADERS, rows, (int) page.getTotalElements(), isTruncated);
    }

    private UUID parseUuid(Object val) {
        if (val instanceof UUID u) return u;
        if (val instanceof String s && !s.isBlank()) {
            try { return UUID.fromString(s); } catch (Exception ignored) {}
        }
        return null;
    }

    private String parseString(Object val) {
        if (val instanceof String s && !s.isBlank()) return s;
        return null;
    }

    private Instant parseInstant(Object val) {
        if (val instanceof Instant i) return i;
        if (val instanceof String s && !s.isBlank()) {
            try { return Instant.parse(s); } catch (Exception ignored) {}
        }
        return null;
    }

    private <E extends Enum<E>> E parseEnum(Object val, Class<E> enumClass) {
        if (val == null) return null;
        if (enumClass.isInstance(val)) return enumClass.cast(val);
        if (val instanceof String s && !s.isBlank()) {
            try { return Enum.valueOf(enumClass, s); } catch (Exception ignored) {}
        }
        return null;
    }
}
