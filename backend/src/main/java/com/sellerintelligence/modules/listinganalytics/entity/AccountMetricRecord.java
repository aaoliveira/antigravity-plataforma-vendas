package com.sellerintelligence.modules.listinganalytics.entity;

import com.sellerintelligence.modules.listinganalytics.model.MetricAvailabilityStatus;
import com.sellerintelligence.modules.listinganalytics.model.MetricGranularity;
import com.sellerintelligence.modules.listinganalytics.model.MetricProvenance;
import com.sellerintelligence.modules.listinganalytics.model.MetricScope;
import com.sellerintelligence.modules.listinganalytics.model.MetricUnit;
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
 * Entidade JPA persistindo métricas de contas conectadas e anúncios.
 */
@Entity
@Table(name = "connected_account_metrics")
public class AccountMetricRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "seller_account_id", nullable = false)
    private UUID sellerAccountId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "listing_id")
    private UUID listingId;

    @Column(name = "external_listing_id", length = 100)
    private String externalListingId;

    @Column(name = "target_identifier", nullable = false, length = 100)
    private String targetIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_scope", nullable = false, length = 30)
    private MetricScope metricScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "granularity", nullable = false, length = 30)
    private MetricGranularity granularity;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;

    @Column(name = "metric_key", nullable = false, length = 100)
    private String metricKey;

    @Column(name = "metric_value", precision = 15, scale = 4)
    private BigDecimal metricValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_unit", nullable = false, length = 30)
    private MetricUnit metricUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "provenance", nullable = false, length = 30)
    private MetricProvenance provenance;

    @Column(name = "formula_definition", length = 500)
    private String formulaDefinition;

    @Column(name = "denominator_metric", length = 100)
    private String denominatorMetric;

    @Column(name = "denominator_value", precision = 15, scale = 4)
    private BigDecimal denominatorValue;

    @Column(name = "numerator_metric", length = 100)
    private String numeratorMetric;

    @Column(name = "numerator_value", precision = 15, scale = 4)
    private BigDecimal numeratorValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 30)
    private MetricAvailabilityStatus availabilityStatus;

    @Column(name = "unavailability_reason", length = 500)
    private String unavailabilityReason;

    @Column(name = "source_endpoint", nullable = false, length = 255)
    private String sourceEndpoint;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountMetricRecord() {
    }

    public AccountMetricRecord(
            UUID id,
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace,
            UUID listingId,
            String externalListingId,
            String targetIdentifier,
            MetricScope metricScope,
            MetricGranularity granularity,
            Instant windowStart,
            Instant windowEnd,
            String metricKey,
            BigDecimal metricValue,
            MetricUnit metricUnit,
            MetricProvenance provenance,
            String formulaDefinition,
            String denominatorMetric,
            BigDecimal denominatorValue,
            String numeratorMetric,
            BigDecimal numeratorValue,
            MetricAvailabilityStatus availabilityStatus,
            String unavailabilityReason,
            String sourceEndpoint,
            Instant ingestedAt,
            String metadataJson,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        this.sellerAccountId = Objects.requireNonNull(sellerAccountId, "sellerAccountId é obrigatório");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace é obrigatório");
        this.listingId = listingId;
        this.externalListingId = externalListingId;
        this.targetIdentifier = targetIdentifier != null && !targetIdentifier.isBlank()
                ? targetIdentifier
                : (externalListingId != null && !externalListingId.isBlank() ? externalListingId : "__ACCOUNT__");
        this.metricScope = Objects.requireNonNull(metricScope, "metricScope é obrigatório");
        this.granularity = Objects.requireNonNull(granularity, "granularity é obrigatória");
        this.windowStart = Objects.requireNonNull(windowStart, "windowStart é obrigatório");
        this.windowEnd = Objects.requireNonNull(windowEnd, "windowEnd é obrigatório");
        this.metricKey = Objects.requireNonNull(metricKey, "metricKey é obrigatório");
        this.metricValue = metricValue;
        this.metricUnit = Objects.requireNonNull(metricUnit, "metricUnit é obrigatória");
        this.provenance = Objects.requireNonNull(provenance, "provenance é obrigatória");
        this.formulaDefinition = formulaDefinition;
        this.denominatorMetric = denominatorMetric;
        this.denominatorValue = denominatorValue;
        this.numeratorMetric = numeratorMetric;
        this.numeratorValue = numeratorValue;
        this.availabilityStatus = Objects.requireNonNull(availabilityStatus, "availabilityStatus é obrigatório");
        this.unavailabilityReason = unavailabilityReason;
        this.sourceEndpoint = Objects.requireNonNull(sourceEndpoint, "sourceEndpoint é obrigatório");
        this.ingestedAt = ingestedAt != null ? ingestedAt : Instant.now();
        this.metadataJson = metadataJson;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
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

    public UUID getListingId() {
        return listingId;
    }

    public void setListingId(UUID listingId) {
        this.listingId = listingId;
    }

    public String getExternalListingId() {
        return externalListingId;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public MetricScope getMetricScope() {
        return metricScope;
    }

    public MetricGranularity getGranularity() {
        return granularity;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public Instant getWindowEnd() {
        return windowEnd;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(BigDecimal metricValue) {
        this.metricValue = metricValue;
    }

    public MetricUnit getMetricUnit() {
        return metricUnit;
    }

    public MetricProvenance getProvenance() {
        return provenance;
    }

    public void setProvenance(MetricProvenance provenance) {
        this.provenance = provenance;
    }

    public String getFormulaDefinition() {
        return formulaDefinition;
    }

    public String getDenominatorMetric() {
        return denominatorMetric;
    }

    public BigDecimal getDenominatorValue() {
        return denominatorValue;
    }

    public String getNumeratorMetric() {
        return numeratorMetric;
    }

    public BigDecimal getNumeratorValue() {
        return numeratorValue;
    }

    public MetricAvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(MetricAvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getUnavailabilityReason() {
        return unavailabilityReason;
    }

    public void setUnavailabilityReason(String unavailabilityReason) {
        this.unavailabilityReason = unavailabilityReason;
    }

    public String getSourceEndpoint() {
        return sourceEndpoint;
    }

    public void setSourceEndpoint(String sourceEndpoint) {
        this.sourceEndpoint = sourceEndpoint;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(Instant ingestedAt) {
        this.ingestedAt = ingestedAt;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
