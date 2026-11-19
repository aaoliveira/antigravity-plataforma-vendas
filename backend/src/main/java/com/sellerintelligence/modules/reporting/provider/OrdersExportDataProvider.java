package com.sellerintelligence.modules.reporting.provider;

import com.sellerintelligence.modules.orderingestion.Order;
import com.sellerintelligence.modules.orderingestion.OrderRepository;
import com.sellerintelligence.modules.orderingestion.model.OrderStatus;
import com.sellerintelligence.modules.reporting.model.ExportDataPage;
import com.sellerintelligence.modules.reporting.model.ExportDataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class OrdersExportDataProvider implements ExportDataProvider {

    private static final List<String> HEADERS = List.of(
            "ID Pedido",
            "Pedido Externo",
            "Marketplace",
            "ID Conta",
            "Status",
            "Data Pedido",
            "Valor Total (BRL)",
            "Moeda",
            "Comprador",
            "Rastreio"
    );

    private final OrderRepository orderRepository;

    public OrdersExportDataProvider(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public ExportDataset supportedDataset() {
        return ExportDataset.ORDERS;
    }

    @Override
    public ExportDataPage fetchTenantData(UUID tenantId, Map<String, Object> filterParams, int maxRows) {
        String marketplace = parseString(filterParams.get("marketplace"));
        UUID sellerAccountId = parseUuid(filterParams.get("sellerAccountId"));
        OrderStatus status = parseEnum(filterParams.get("status"), OrderStatus.class);
        Instant startDate = parseInstant(filterParams.get("startDate"));
        Instant endDate = parseInstant(filterParams.get("endDate"));
        String search = parseString(filterParams.get("search"));

        int limit = Math.max(1, Math.min(maxRows, 50000));
        Page<Order> page = orderRepository.searchOrders(
                tenantId,
                marketplace,
                sellerAccountId,
                status,
                startDate,
                endDate,
                search,
                PageRequest.of(0, limit)
        );

        List<List<Object>> rows = new ArrayList<>();
        for (Order order : page.getContent()) {
            rows.add(List.of(
                    order.getId().toString(),
                    order.getExternalOrderId() != null ? order.getExternalOrderId() : "",
                    order.getMarketplace() != null ? order.getMarketplace() : "",
                    order.getSellerAccountId() != null ? order.getSellerAccountId().toString() : "",
                    order.getStatus() != null ? order.getStatus().name() : "",
                    order.getOrderCreatedAt() != null ? order.getOrderCreatedAt().toString() : "",
                    order.getTotalAmount() != null ? order.getTotalAmount() : "",
                    order.getCurrency() != null ? order.getCurrency() : "BRL",
                    order.getBuyerName() != null ? order.getBuyerName() : "",
                    order.getTrackingNumber() != null ? order.getTrackingNumber() : ""
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
