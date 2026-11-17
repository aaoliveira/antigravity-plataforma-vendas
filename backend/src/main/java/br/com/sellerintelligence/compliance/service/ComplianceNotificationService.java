package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.compliance.domain.ComplianceRiskLevel;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for dispatching administrative compliance alerts with deduplication and cooldown.
 */
@Service
public class ComplianceNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceNotificationService.class);

    private static final long COOLDOWN_SECONDS = 300L; // 5 minutes deduplication cooldown

    private final Map<String, Instant> lastSentByDedupKey = new ConcurrentHashMap<>();

    public boolean sendAlert(Marketplace marketplace, ComplianceRiskLevel severity, String title, String message) {
        String dedupKey = marketplace + ":" + severity + ":" + title;
        Instant now = Instant.now();

        Instant lastSent = lastSentByDedupKey.get(dedupKey);
        if (lastSent != null && now.isBefore(lastSent.plusSeconds(COOLDOWN_SECONDS))) {
            log.info("ComplianceNotification: Suppressed duplicate alert [{}] (cooldown active)", dedupKey);
            return false;
        }

        lastSentByDedupKey.put(dedupKey, now);

        // Format administrative notification
        log.warn("ADMIN_COMPLIANCE_ALERT: [Marketplace: {}] [Severity: {}] - {} | Details: {}",
                marketplace, severity, title, message);

        return true;
    }

    public void clearCooldowns() {
        lastSentByDedupKey.clear();
    }
}
