package com.sellerintelligence.modules.productmonitoring.service;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.BusinessRuleException;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.core.exception.SourceUnavailableException;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemRecord;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemSnapshotRecord;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemSnapshotDto;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço responsável pela gravação idempotente e consulta de snapshots históricos.
 * Cumpre as regras de ouro:
 * 1. "Snapshots idempotentes de preço e atributos disponíveis": duplicatas evitadas determinísticamente.
 * 2. "Detectar alteração por comparação com snapshot anterior": cálculo de delta e percentual.
 * 3. "Não criar snapshots se a fonte estiver indisponível": fail-closed estrito.
 * 4. "Histórico começa no primeiro snapshot efetivo após inclusão": sem dados retroativos.
 */
@Service
public class MonitoredItemSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(MonitoredItemSnapshotService.class);

    private final MonitoredItemRepository itemRepository;
    private final MonitoredItemSnapshotRepository snapshotRepository;
    private final AuditPublisher auditPublisher;

    public MonitoredItemSnapshotService(
            MonitoredItemRepository itemRepository,
            MonitoredItemSnapshotRepository snapshotRepository,
            AuditPublisher auditPublisher
    ) {
        this.itemRepository = itemRepository;
        this.snapshotRepository = snapshotRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public MonitoredItemSnapshotDto recordSnapshot(
            UUID tenantId,
            UUID itemId,
            BigDecimal price,
            BigDecimal shippingCost,
            Integer availableQuantity,
            String itemStatus,
            Instant observedAt
    ) {
        return recordSnapshot(
                tenantId,
                itemId,
                price,
                shippingCost,
                availableQuantity,
                itemStatus,
                observedAt,
                null,
                null,
                null,
                null,
                false
        );
    }

    @Transactional
    public MonitoredItemSnapshotDto recordSnapshot(
            UUID tenantId,
            UUID itemId,
            BigDecimal price,
            BigDecimal shippingCost,
            Integer availableQuantity,
            String itemStatus,
            Instant observedAt,
            String idempotencyKey,
            String sourceUrl,
            String sourceType,
            Instant validUntil,
            Boolean simulateSourceUnavailable
    ) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(itemId, "itemId cannot be null");

        MonitoredItemRecord item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .filter(i -> i.getStatus() != MonitoredItemStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Item monitorado não encontrado: " + itemId));

        // 1. FAIL-CLOSED: Não criar snapshots se a fonte estiver indisponível
        if (Boolean.TRUE.equals(simulateSourceUnavailable)) {
            log.warn("Tentativa de gravação de snapshot abortada para item {}: fonte '{}' indisponível",
                    itemId, item.getSourcePolicyId());
            throw new SourceUnavailableException(
                    item.getSourcePolicyId(),
                    "Fonte de dados externa indisponível: endpoint inalcançável ou bloqueado"
            );
        }

        Objects.requireNonNull(price, "price cannot be null");
        Instant actualObservedAt = observedAt != null ? observedAt : Instant.now();

        // 2. REGRA ANTI-ALUCINAÇÃO: Não retroagir snapshot para antes da data de inclusão do item
        if (actualObservedAt.isBefore(item.getCreatedAt().minusSeconds(5))) {
            throw new BusinessRuleException(
                    "Violação de integridade histórica: o snapshot (" + actualObservedAt +
                    ") não pode ser anterior à data de inclusão do item no monitoramento (" + item.getCreatedAt() + ")."
            );
        }

        // 3. IDEMPOTÊNCIA DETERMINÍSTICA: Chave única evita duplicatas
        String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey.trim()
                : computeDeterministicKey(tenantId, itemId, actualObservedAt, price, availableQuantity);

        Optional<MonitoredItemSnapshotRecord> existing = snapshotRepository.findByTenantIdAndIdempotencyKey(tenantId, effectiveKey);
        if (existing.isPresent()) {
            log.info("Snapshot idempotente detectado para tenant={}, item={}, key={}. Retornando registro existente.",
                    tenantId, itemId, effectiveKey);
            return toDto(existing.get());
        }

        // 4. DETECÇÃO DE ALTERAÇÕES EM RELAÇÃO AO SNAPSHOT ANTERIOR
        Optional<MonitoredItemSnapshotRecord> prevOpt = snapshotRepository
                .findTopByTenantIdAndItemIdAndObservedAtLessThanEqualOrderByObservedAtDesc(tenantId, itemId, actualObservedAt);

        UUID previousSnapshotId = null;
        boolean hasPriceChange = false;
        BigDecimal priceChangeAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal priceChangePercent = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        boolean hasAvailabilityChange = false;
        boolean hasAttributeChange = false;
        String changeType = "INITIAL";

        String effectiveStatus = itemStatus != null ? itemStatus : "ACTIVE";
        String shippingStatus = shippingCost != null
                ? (shippingCost.compareTo(BigDecimal.ZERO) == 0 ? "FREE" : "KNOWN")
                : "UNKNOWN";

        if (prevOpt.isPresent()) {
            MonitoredItemSnapshotRecord prev = prevOpt.get();
            previousSnapshotId = prev.getId();

            // Delta de preço
            priceChangeAmount = price.subtract(prev.getPrice()).setScale(4, RoundingMode.HALF_UP);
            hasPriceChange = priceChangeAmount.compareTo(BigDecimal.ZERO) != 0;

            if (prev.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                priceChangePercent = priceChangeAmount
                        .divide(prev.getPrice(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(4, RoundingMode.HALF_UP);
            }

            // Delta de disponibilidade
            hasAvailabilityChange = !Objects.equals(availableQuantity, prev.getAvailableQuantity());

            // Delta de atributos / status
            hasAttributeChange = !Objects.equals(effectiveStatus, prev.getStatus())
                    || !Objects.equals(shippingStatus, prev.getShippingStatus());

            // Classificação do change_type
            if (priceChangeAmount.compareTo(BigDecimal.ZERO) > 0) {
                changeType = "PRICE_INCREASE";
            } else if (priceChangeAmount.compareTo(BigDecimal.ZERO) < 0) {
                changeType = "PRICE_DECREASE";
            } else if (hasAvailabilityChange) {
                changeType = "AVAILABILITY_CHANGE";
            } else if (hasAttributeChange) {
                changeType = "STATUS_CHANGE";
            } else {
                changeType = "NO_CHANGE";
            }
        }

        // 5. METADADOS DE VALIDADE E ORIGEM
        int intervalMinutes = item.getMonitoringIntervalMinutes() > 0 ? item.getMonitoringIntervalMinutes() : 60;
        Instant effectiveValidUntil = validUntil != null ? validUntil : actualObservedAt.plus(Duration.ofMinutes(intervalMinutes));
        String effectiveSourceUrl = sourceUrl != null ? sourceUrl : item.getSourceUrl();
        String effectiveSourceType = sourceType != null ? sourceType : "OFFICIAL_API";

        MonitoredItemSnapshotRecord snapshot = new MonitoredItemSnapshotRecord(
                UUID.randomUUID(),
                tenantId,
                item.getId(),
                item.getGroupId(),
                item.getMarketplace(),
                item.getExternalListingId(),
                price,
                item.getCurrency(),
                shippingCost,
                shippingStatus,
                availableQuantity,
                effectiveStatus,
                "OBSERVED",
                Instant.now(),
                actualObservedAt,
                effectiveValidUntil,
                true,
                effectiveKey,
                effectiveSourceUrl,
                effectiveSourceType,
                "SUCCESS",
                previousSnapshotId,
                hasPriceChange,
                priceChangeAmount,
                priceChangePercent,
                hasAvailabilityChange,
                hasAttributeChange,
                changeType,
                Instant.now()
        );

        MonitoredItemSnapshotRecord saved = snapshotRepository.save(snapshot);

        // Atualiza metadados do item monitorado
        if (item.getFirstSnapshotAt() == null) {
            item.setFirstSnapshotAt(actualObservedAt);
            log.info("Primeiro snapshot efetivo registrado para o item: id={}, observedAt={}", itemId, actualObservedAt);
        }

        item.setCurrentPrice(price);
        item.setLastSnapshotAt(actualObservedAt);
        item.setNextRunAt(actualObservedAt.plus(Duration.ofMinutes(item.getMonitoringIntervalMinutes())));
        itemRepository.save(item);

        auditPublisher.publish(
                "MONITORED_ITEM_SNAPSHOT_RECORDED",
                null,
                "system",
                tenantId,
                null,
                null,
                null,
                String.format("Snapshot registrado para item %s: preço=%s, changeType=%s, status=%s",
                        item.getTitle(), price, changeType, effectiveStatus)
        );

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MonitoredItemSnapshotDto> getItemSnapshots(UUID tenantId, UUID itemId) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(itemId, "itemId cannot be null");

        MonitoredItemRecord item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .filter(i -> i.getStatus() != MonitoredItemStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Item monitorado não encontrado: " + itemId));

        // Filtra estritamente snapshots ocorridos a partir da data de inclusão do item (ou firstSnapshotAt)
        Instant inclusionInstant = item.getFirstSnapshotAt() != null ? item.getFirstSnapshotAt() : item.getCreatedAt();
        List<MonitoredItemSnapshotRecord> snapshots = snapshotRepository.findSnapshotsSinceInclusion(
                tenantId, itemId, inclusionInstant.minusSeconds(1)
        );

        return snapshots.stream().map(this::toDto).toList();
    }

    public static String computeDeterministicKey(UUID tenantId, UUID itemId, Instant observedAt, BigDecimal price, Integer availableQuantity) {
        long epochMinute = observedAt != null ? observedAt.getEpochSecond() / 60 : 0;
        String priceStr = price != null ? price.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
        String raw = tenantId + ":" + itemId + ":" + epochMinute + ":" + priceStr + ":" + (availableQuantity != null ? availableQuantity : 0);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível", e);
        }
    }

    public MonitoredItemSnapshotDto toDto(MonitoredItemSnapshotRecord r) {
        return new MonitoredItemSnapshotDto(
                r.getId(),
                r.getItemId(),
                r.getGroupId(),
                r.getMarketplace(),
                r.getExternalListingId(),
                r.getPrice(),
                r.getCurrency(),
                r.getShippingCost(),
                r.getShippingStatus(),
                r.getAvailableQuantity(),
                r.getStatus(),
                r.getProvenanceType(),
                r.getCollectedAt(),
                r.getObservedAt(),
                r.getValidUntil(),
                r.isEffectiveSnapshot(),
                r.getIdempotencyKey(),
                r.getSourceUrl(),
                r.getSourceType(),
                r.getCollectionStatus(),
                r.getPreviousSnapshotId(),
                r.isHasPriceChange(),
                r.getPriceChangeAmount(),
                r.getPriceChangePercent(),
                r.isHasAvailabilityChange(),
                r.isHasAttributeChange(),
                r.getChangeType()
        );
    }
}
