package com.sellerintelligence.modules.reporting.provider;

import com.sellerintelligence.modules.listinganalytics.entity.AccountMetricRecord;
import com.sellerintelligence.modules.listinganalytics.model.MetricAvailabilityStatus;
import com.sellerintelligence.modules.listinganalytics.model.MetricScope;
import com.sellerintelligence.modules.listinganalytics.repository.AccountMetricRepository;
import com.sellerintelligence.modules.reporting.model.ExportDataPage;
import com.sellerintelligence.modules.reporting.model.ExportDataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class MetricsExportDataProvider implements ExportDataProvider {

    private static final List<String> HEADERS = List.of(
            "ID Métrica",
            "Marketplace",
            "ID Conta",
            "Anúncio Externo",
            "Escopo",
            "Chave Métrica",
            "Granularidade",
            "Início Janela",
            "Fim Janela",
            "Valor Numérico",
            "Status Disponibilidade",
            "Origem Oficial"
    );

    private final AccountMetricRepository repository;

    public MetricsExportDataProvider(AccountMetricRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExportDataset supportedDataset() {
        return ExportDataset.METRICS;
    }

    @Override
    public ExportDataPage fetchTenantData(UUID tenantId, Map<String, Object> filterParams, int maxRows) {
        UUID sellerAccountId = parseUuid(filterParams.get("sellerAccountId"));
        String marketplace = parseString(filterParams.get("marketplace"));
        String externalListingId = parseString(filterParams.get("externalListingId"));
        MetricScope scope = parseEnum(filterParams.get("metricScope"), MetricScope.class);
        String metricKey = parseString(filterParams.get("metricKey"));
        MetricAvailabilityStatus status = parseEnum(filterParams.get("availabilityStatus"), MetricAvailabilityStatus.class);
        Instant startDate = parseInstant(filterParams.get("startDate"));
        Instant endDate = parseInstant(filterParams.get("endDate"));

        int limit = Math.max(1, Math.min(maxRows, 50000));
        Page<AccountMetricRecord> page = repository.searchMetrics(
                tenantId,
                sellerAccountId,
                marketplace,
                externalListingId,
                scope,
                metricKey,
                status,
                startDate,
                endDate,
                PageRequest.of(0, limit)
        );

        List<List<Object>> rows = new ArrayList<>();
        for (AccountMetricRecord metric : page.getContent()) {
            rows.add(List.of(
                    metric.getId().toString(),
                    metric.getMarketplace() != null ? metric.getMarketplace() : "",
                    metric.getSellerAccountId() != null ? metric.getSellerAccountId().toString() : "",
                    metric.getExternalListingId() != null ? metric.getExternalListingId() : "",
                    metric.getMetricScope() != null ? metric.getMetricScope().name() : "",
                    metric.getMetricKey() != null ? metric.getMetricKey() : "",
                    metric.getGranularity() != null ? metric.getGranularity().name() : "",
                    metric.getWindowStart() != null ? metric.getWindowStart().toString() : "",
                    metric.getWindowEnd() != null ? metric.getWindowEnd().toString() : "",
                    metric.getMetricValue() != null ? metric.getMetricValue() : "",
                    metric.getAvailabilityStatus() != null ? metric.getAvailabilityStatus().name() : "",
                    metric.getSourceEndpoint() != null ? metric.getSourceEndpoint() : ""
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
