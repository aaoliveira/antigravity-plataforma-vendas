package br.com.sellerintelligence.action.service;

import br.com.sellerintelligence.action.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class ActionAlertService {

    private static final Logger log = LoggerFactory.getLogger(ActionAlertService.class);

    private final ActionAlertRepository alertRepository;
    private final ActionNotificationService notificationService;

    public ActionAlertService(ActionAlertRepository alertRepository,
                              ActionNotificationService notificationService) {
        this.alertRepository = alertRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ActionAlert createAlertIfAbsent(UUID tenantId, UUID ruleId, String title, String description,
                                          ActionAlertSeverity severity, ActionAlertDomain domainType,
                                          AlertScopeType scopeType, String scopeId, String provenance,
                                          String evidenceContext, int cooldownMinutes) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(title, "title cannot be null");

        String dedupKey = calculateDedupKey(tenantId, ruleId, domainType, scopeType, scopeId, cooldownMinutes);

        Optional<ActionAlert> existing = alertRepository.findByTenantIdAndDedupKey(tenantId, dedupKey);
        if (existing.isPresent()) {
            log.info("Suppressed duplicate alert within cooldown ({}m): tenant={}, dedupKey={}",
                    cooldownMinutes, tenantId, dedupKey);
            return existing.get();
        }

        ActionAlert alert = new ActionAlert(
                tenantId, ruleId, title, description, severity, domainType,
                scopeType, scopeId, provenance, evidenceContext, dedupKey
        );
        ActionAlert saved = alertRepository.save(alert);
        log.info("Created operational alert: id={}, title='{}', severity={}, tenantId={}", saved.getId(), title, severity, tenantId);

        // Dispatch notifications according to opt-in preferences and frequency limits
        try {
            notificationService.dispatchAlertNotifications(saved);
        } catch (Exception ex) {
            log.error("Error dispatching notifications for alert {}: {}", saved.getId(), ex.getMessage());
        }

        return saved;
    }

    @Transactional
    public ActionAlert updateStatus(UUID tenantId, UUID alertId, ActionAlertStatus status) {
        ActionAlert alert = getAlert(tenantId, alertId);
        alert.updateStatus(status);
        log.info("Updated alert status: alertId={}, newStatus={}, tenantId={}", alertId, status, tenantId);
        return alertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public ActionAlert getAlert(UUID tenantId, UUID alertId) {
        return alertRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Alerta não encontrado: " + alertId));
    }

    @Transactional(readOnly = true)
    public List<ActionAlert> getAlerts(UUID tenantId, ActionAlertStatus status, ActionAlertDomain domain) {
        if (status != null) {
            return alertRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
        }
        if (domain != null) {
            return alertRepository.findByTenantIdAndDomainTypeOrderByCreatedAtDesc(tenantId, domain);
        }
        return alertRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    private String calculateDedupKey(UUID tenantId, UUID ruleId, ActionAlertDomain domain,
                                     AlertScopeType scopeType, String scopeId, int cooldownMinutes) {
        int window = cooldownMinutes > 0 ? cooldownMinutes : 60;
        long timeBucket = System.currentTimeMillis() / (window * 60_000L);
        String raw = String.format("%s:%s:%s:%s:%s:%d",
                tenantId,
                ruleId != null ? ruleId.toString() : domain.name(),
                domain != null ? domain.name() : "GENERAL",
                scopeType != null ? scopeType.name() : "ORGANIZATION",
                scopeId != null ? scopeId : "ALL",
                timeBucket
        );
        return sha256Hex(raw);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
