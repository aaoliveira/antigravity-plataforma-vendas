package br.com.sellerintelligence.monitoring.service;

import br.com.sellerintelligence.monitoring.domain.MonitoringChangeEvent;
import br.com.sellerintelligence.monitoring.domain.MonitoringChangeEventRepository;
import br.com.sellerintelligence.monitoring.domain.MonitoringEventType;
import br.com.sellerintelligence.monitoring.domain.MonitoringSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class MonitoringDiffEngine {

    private static final Logger log = LoggerFactory.getLogger(MonitoringDiffEngine.class);
    private final MonitoringChangeEventRepository changeEventRepository;

    public MonitoringDiffEngine(MonitoringChangeEventRepository changeEventRepository) {
        this.changeEventRepository = changeEventRepository;
    }

    @Transactional
    public List<MonitoringChangeEvent> detectChanges(MonitoringSnapshot previous, MonitoringSnapshot current) {
        List<MonitoringChangeEvent> detected = new ArrayList<>();
        if (current == null) {
            return detected;
        }

        // If no previous snapshot, this is the first observation — no diff to compare
        if (previous == null) {
            return detected;
        }

        // 1. Detect Price Changes (Deterministic BigDecimal)
        if (previous.getPrice() != null && current.getPrice() != null) {
            if (previous.getPrice().compareTo(current.getPrice()) != 0) {
                BigDecimal deltaAmount = current.getPrice().subtract(previous.getPrice()).setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal deltaPercentage = BigDecimal.ZERO;
                if (previous.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                    deltaPercentage = deltaAmount
                            .divide(previous.getPrice(), 6, RoundingMode.HALF_EVEN)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(4, RoundingMode.HALF_EVEN);
                }

                String eventHash = generateEventHash(
                        current.getTenantId().toString(),
                        current.getItemId().toString(),
                        MonitoringEventType.PRICE_CHANGE.name(),
                        previous.getPrice().toPlainString(),
                        current.getPrice().toPlainString(),
                        current.getCollectedAt().toString().substring(0, 13) // hourly bucket for exact dedup
                );

                if (!changeEventRepository.existsByTenantIdAndEventHash(current.getTenantId(), eventHash)) {
                    MonitoringChangeEvent event = new MonitoringChangeEvent(
                            current.getTenantId(),
                            current.getItemId(),
                            current.getId(),
                            MonitoringEventType.PRICE_CHANGE,
                            previous.getPrice().toPlainString(),
                            current.getPrice().toPlainString(),
                            deltaAmount,
                            deltaPercentage,
                            eventHash,
                            current.getCollectedAt()
                    );
                    detected.add(changeEventRepository.save(event));
                    log.info("Detected PRICE_CHANGE for item {}: {} -> {} (delta: {}%)",
                            current.getItemId(), previous.getPrice(), current.getPrice(), deltaPercentage);
                }
            }
        }

        // 2. Detect Status Changes
        if (!Objects.equals(previous.getListingStatus(), current.getListingStatus())) {
            MonitoringEventType statusEventType = MonitoringEventType.STATUS_CHANGE;
            if ("OUT_OF_STOCK".equalsIgnoreCase(current.getListingStatus())) {
                statusEventType = MonitoringEventType.OUT_OF_STOCK;
            } else if ("ACTIVE".equalsIgnoreCase(current.getListingStatus()) && "PAUSED".equalsIgnoreCase(previous.getListingStatus())) {
                statusEventType = MonitoringEventType.REACTIVATED;
            }

            String eventHash = generateEventHash(
                    current.getTenantId().toString(),
                    current.getItemId().toString(),
                    statusEventType.name(),
                    previous.getListingStatus(),
                    current.getListingStatus(),
                    current.getCollectedAt().toString().substring(0, 13)
            );

            if (!changeEventRepository.existsByTenantIdAndEventHash(current.getTenantId(), eventHash)) {
                MonitoringChangeEvent event = new MonitoringChangeEvent(
                        current.getTenantId(),
                        current.getItemId(),
                        current.getId(),
                        statusEventType,
                        previous.getListingStatus(),
                        current.getListingStatus(),
                        null,
                        null,
                        eventHash,
                        current.getCollectedAt()
                );
                detected.add(changeEventRepository.save(event));
                log.info("Detected STATUS_CHANGE for item {}: {} -> {}",
                        current.getItemId(), previous.getListingStatus(), current.getListingStatus());
            }
        }

        // 3. Detect Title Changes
        if (previous.getTitle() != null && current.getTitle() != null
                && !previous.getTitle().trim().equals(current.getTitle().trim())) {
            String eventHash = generateEventHash(
                    current.getTenantId().toString(),
                    current.getItemId().toString(),
                    MonitoringEventType.TITLE_CHANGE.name(),
                    previous.getTitle(),
                    current.getTitle(),
                    current.getCollectedAt().toString().substring(0, 13)
            );

            if (!changeEventRepository.existsByTenantIdAndEventHash(current.getTenantId(), eventHash)) {
                MonitoringChangeEvent event = new MonitoringChangeEvent(
                        current.getTenantId(),
                        current.getItemId(),
                        current.getId(),
                        MonitoringEventType.TITLE_CHANGE,
                        previous.getTitle(),
                        current.getTitle(),
                        null,
                        null,
                        eventHash,
                        current.getCollectedAt()
                );
                detected.add(changeEventRepository.save(event));
                log.info("Detected TITLE_CHANGE for item {}: \"{}\" -> \"{}\"",
                        current.getItemId(), previous.getTitle(), current.getTitle());
            }
        }

        return detected;
    }

    public static String generateEventHash(String tenantId, String itemId, String eventType,
                                           String prevValue, String currValue, String bucket) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = String.join(":",
                    tenantId != null ? tenantId : "",
                    itemId != null ? itemId : "",
                    eventType != null ? eventType : "",
                    prevValue != null ? prevValue : "",
                    currValue != null ? currValue : "",
                    bucket != null ? bucket : "");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
