package br.com.sellerintelligence.monitoring.service;

import br.com.sellerintelligence.monitoring.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class MonitoringAlertService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringAlertService.class);

    private final MonitoringAlertRuleRepository ruleRepository;
    private final MonitoringAlertRepository alertRepository;

    public MonitoringAlertService(MonitoringAlertRuleRepository ruleRepository,
                                  MonitoringAlertRepository alertRepository) {
        this.ruleRepository = ruleRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional
    public MonitoringAlertRule createRule(UUID tenantId, UUID watchlistId, String name,
                                          MonitoringEventType eventType, BigDecimal minPriceDeltaPercentage,
                                          AlertDirection direction, int cooldownMinutes, AlertChannel channel) {
        MonitoringAlertRule rule = new MonitoringAlertRule(
                tenantId, watchlistId, name, eventType,
                minPriceDeltaPercentage, direction, cooldownMinutes, channel
        );
        return ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<MonitoringAlertRule> listRules(UUID tenantId) {
        return ruleRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<MonitoringAlert> listAlerts(UUID tenantId) {
        return alertRepository.findByTenantIdOrderByTriggeredAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<MonitoringAlert> listUnreadAlerts(UUID tenantId) {
        return alertRepository.findByTenantIdAndReadFalseOrderByTriggeredAtDesc(tenantId);
    }

    @Transactional
    public boolean markAlertAsRead(UUID alertId, UUID tenantId) {
        Optional<MonitoringAlert> alertOpt = alertRepository.findByIdAndTenantId(alertId, tenantId);
        if (alertOpt.isPresent()) {
            alertOpt.get().markAsRead();
            alertRepository.save(alertOpt.get());
            return true;
        }
        return false;
    }

    @Transactional
    public List<MonitoringAlert> evaluateEvent(MonitoringChangeEvent event, WatchlistItem item) {
        List<MonitoringAlert> generatedAlerts = new ArrayList<>();
        if (event == null || item == null) {
            return generatedAlerts;
        }

        List<MonitoringAlertRule> rules = ruleRepository.findByTenantIdAndStatus(event.getTenantId(), "ACTIVE");
        for (MonitoringAlertRule rule : rules) {
            // Watchlist match (if rule is scoped to a specific watchlist)
            if (rule.getWatchlistId() != null && !rule.getWatchlistId().equals(item.getWatchlistId())) {
                continue;
            }

            // Event type match
            if (rule.getEventType() != MonitoringEventType.ANY && rule.getEventType() != event.getEventType()) {
                continue;
            }

            // Price change criteria match
            if (event.getEventType() == MonitoringEventType.PRICE_CHANGE) {
                if (rule.getMinPriceDeltaPercentage() != null && event.getPriceDeltaPercentage() != null) {
                    BigDecimal absDelta = event.getPriceDeltaPercentage().abs();
                    if (absDelta.compareTo(rule.getMinPriceDeltaPercentage()) < 0) {
                        continue; // Did not reach threshold
                    }
                }

                if (rule.getDirection() == AlertDirection.INCREASE && event.getPriceDeltaAmount() != null
                        && event.getPriceDeltaAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                if (rule.getDirection() == AlertDirection.DECREASE && event.getPriceDeltaAmount() != null
                        && event.getPriceDeltaAmount().compareTo(BigDecimal.ZERO) >= 0) {
                    continue;
                }
            }

            // Cooldown deduplication key calculation
            long cooldownSeconds = (long) rule.getCooldownMinutes() * 60;
            if (cooldownSeconds <= 0) {
                cooldownSeconds = 3600;
            }
            long epochBucket = event.getDetectedAt().getEpochSecond() / cooldownSeconds;
            String dedupKey = rule.getId() + ":" + item.getId() + ":" + event.getEventType().name() + ":" + epochBucket;

            if (alertRepository.existsByTenantIdAndDedupKey(event.getTenantId(), dedupKey)) {
                log.info("Monitoring alert suppressed by cooldown window: dedupKey={}", dedupKey);
                continue;
            }

            // Formulate Alert
            String title = formatAlertTitle(event, item);
            String message = formatAlertMessage(event, item, rule);
            AlertSeverity severity = determineSeverity(event);

            MonitoringAlert alert = new MonitoringAlert(
                    event.getTenantId(),
                    rule.getId(),
                    item.getId(),
                    event.getId(),
                    title,
                    message,
                    severity,
                    dedupKey,
                    event.getDetectedAt()
            );

            generatedAlerts.add(alertRepository.save(alert));
            log.info("Triggered monitoring alert: id={}, title=\"{}\"", alert.getId(), title);
        }

        return generatedAlerts;
    }

    private String formatAlertTitle(MonitoringChangeEvent event, WatchlistItem item) {
        return switch (event.getEventType()) {
            case PRICE_CHANGE -> {
                boolean isDrop = event.getPriceDeltaAmount() != null && event.getPriceDeltaAmount().compareTo(BigDecimal.ZERO) < 0;
                yield (isDrop ? "Queda de Preço Detectada: " : "Aumento de Preço Detectado: ") + item.getTargetName();
            }
            case OUT_OF_STOCK -> "Produto Esgotado / Pausado: " + item.getTargetName();
            case REACTIVATED -> "Produto Reativado: " + item.getTargetName();
            case STATUS_CHANGE -> "Mudança de Status no Anúncio: " + item.getTargetName();
            case TITLE_CHANGE -> "Título Alterado: " + item.getTargetName();
            default -> "Alerta de Monitoramento: " + item.getTargetName();
        };
    }

    private String formatAlertMessage(MonitoringChangeEvent event, WatchlistItem item, MonitoringAlertRule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append("Item: ").append(item.getTargetName()).append(" (").append(item.getExternalId()).append(")\n");
        sb.append("Marketplace: ").append(item.getMarketplace()).append("\n");
        if (event.getEventType() == MonitoringEventType.PRICE_CHANGE) {
            sb.append("Preço Anterior: R$ ").append(event.getPreviousValue()).append("\n");
            sb.append("Novo Preço: R$ ").append(event.getCurrentValue()).append("\n");
            sb.append("Variação: ").append(event.getPriceDeltaPercentage()).append("% (R$ ").append(event.getPriceDeltaAmount()).append(")\n");
        } else {
            sb.append("Status Anterior: ").append(event.getPreviousValue()).append("\n");
            sb.append("Status Atual: ").append(event.getCurrentValue()).append("\n");
        }
        sb.append("Regra: ").append(rule.getName());
        return sb.toString();
    }

    private AlertSeverity determineSeverity(MonitoringChangeEvent event) {
        if (event.getEventType() == MonitoringEventType.PRICE_CHANGE && event.getPriceDeltaPercentage() != null) {
            BigDecimal absPct = event.getPriceDeltaPercentage().abs();
            if (absPct.compareTo(BigDecimal.valueOf(20)) >= 0) {
                return AlertSeverity.CRITICAL;
            } else if (absPct.compareTo(BigDecimal.valueOf(10)) >= 0) {
                return AlertSeverity.WARNING;
            }
        } else if (event.getEventType() == MonitoringEventType.OUT_OF_STOCK) {
            return AlertSeverity.WARNING;
        }
        return AlertSeverity.INFO;
    }
}
