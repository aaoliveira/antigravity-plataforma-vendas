package com.sellerintelligence.modules.orderingestion;

import com.sellerintelligence.modules.orderingestion.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade central representando um pedido de venda próprio no marketplace.
 * Escopada estritamente por tenant_id e protegida por RLS.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "seller_account_id", nullable = false)
    private UUID sellerAccountId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "shipping_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal shippingCost;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "buyer_name", length = 255)
    private String buyerName;

    @Column(name = "buyer_document", length = 50)
    private String buyerDocument;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "raw_payload_json", columnDefinition = "TEXT")
    private String rawPayloadJson;

    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion;

    @Column(name = "origin", nullable = false, length = 100)
    private String origin;

    @Column(name = "order_created_at", nullable = false)
    private Instant orderCreatedAt;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
        // JPA requirement
    }

    public Order(
            UUID id,
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace,
            String externalOrderId,
            OrderStatus status,
            BigDecimal subtotal,
            BigDecimal shippingCost,
            BigDecimal totalAmount,
            String currency,
            String buyerName,
            String buyerDocument,
            String trackingNumber,
            String cancellationReason,
            Instant cancelledAt,
            Instant returnedAt,
            String rawPayloadJson,
            String schemaVersion,
            String origin,
            Instant orderCreatedAt,
            Instant syncedAt
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.sellerAccountId = Objects.requireNonNull(sellerAccountId, "sellerAccountId não pode ser nulo");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        this.externalOrderId = Objects.requireNonNull(externalOrderId, "externalOrderId não pode ser nulo");
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
        this.shippingCost = shippingCost != null ? shippingCost : BigDecimal.ZERO;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.currency = currency != null ? currency : "BRL";
        this.buyerName = buyerName;
        this.buyerDocument = buyerDocument;
        this.trackingNumber = trackingNumber;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = cancelledAt;
        this.returnedAt = returnedAt;
        this.rawPayloadJson = rawPayloadJson;
        this.schemaVersion = schemaVersion != null ? schemaVersion : "v1";
        this.origin = Objects.requireNonNull(origin, "origin não pode ser nulo");
        this.orderCreatedAt = orderCreatedAt != null ? orderCreatedAt : Instant.now();
        this.syncedAt = syncedAt != null ? syncedAt : Instant.now();
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void reconcile(
            OrderStatus newStatus,
            BigDecimal newSubtotal,
            BigDecimal newShippingCost,
            BigDecimal newTotalAmount,
            String newTrackingNumber,
            String newCancellationReason,
            Instant newCancelledAt,
            Instant newReturnedAt,
            String updatedRawPayload
    ) {
        this.status = Objects.requireNonNull(newStatus, "newStatus não pode ser nulo");
        if (newSubtotal != null) this.subtotal = newSubtotal;
        if (newShippingCost != null) this.shippingCost = newShippingCost;
        if (newTotalAmount != null) this.totalAmount = newTotalAmount;
        if (newTrackingNumber != null) this.trackingNumber = newTrackingNumber;
        if (newCancellationReason != null) this.cancellationReason = newCancellationReason;
        if (newCancelledAt != null) this.cancelledAt = newCancelledAt;
        if (newReturnedAt != null) this.returnedAt = newReturnedAt;
        if (updatedRawPayload != null) this.rawPayloadJson = updatedRawPayload;
        this.syncedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markCancelled(String reason, Instant cancelledAt) {
        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = cancelledAt != null ? cancelledAt : Instant.now();
        this.updatedAt = Instant.now();
        this.syncedAt = Instant.now();
    }

    public void markReturned(Instant returnedAt) {
        this.status = OrderStatus.RETURNED;
        this.returnedAt = returnedAt != null ? returnedAt : Instant.now();
        this.updatedAt = Instant.now();
        this.syncedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getSellerAccountId() {
        return sellerAccountId;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getBuyerDocument() {
        return buyerDocument;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public String getRawPayloadJson() {
        return rawPayloadJson;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getOrigin() {
        return origin;
    }

    public Instant getOrderCreatedAt() {
        return orderCreatedAt;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
