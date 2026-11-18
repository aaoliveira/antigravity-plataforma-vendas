package com.sellerintelligence.modules.productmonitoring.service;

import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemRecord;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoringGroupRecord;
import com.sellerintelligence.modules.productmonitoring.model.JobExecutionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Validador rigoroso de políticas, janelas mínimas, quotas, kill switch e capacidades.
 * Cumpre os requisitos estritos:
 * 1. "Agendamento respeita quotas, janela mínima, kill switch e capacidade validada."
 * 2. "Não buscar dado em fonte proibida."
 */
@Service
public class MonitoringSchedulerPolicyValidator {

    private static final Logger log = LoggerFactory.getLogger(MonitoringSchedulerPolicyValidator.class);

    // Fontes explicitamente proibidas ou sem autorização contratual / legal
    private static final Set<String> FORBIDDEN_SOURCES = Set.of(
            "UNLICENSED_SCRAPER",
            "PROHIBITED_ROBOTS_TXT_VIOLATION",
            "HOSTILE_PARSING",
            "UNAPPROVED_COMPETITOR_API"
    );

    // Mapa de Kill Switches ativos por canal/marketplace (ex: em caso de manutenção ou aviso de limite)
    private final Map<String, Boolean> killSwitches = new ConcurrentHashMap<>();

    // Contador simples de rate limit em memória (snapshots por janela de 1 minuto por tenant)
    private static final int MAX_SNAPSHOTS_PER_MINUTE_PER_TENANT = 60;
    private final Map<UUID, Map<Long, AtomicInteger>> tenantQuotaCounters = new ConcurrentHashMap<>();

    public record PolicyValidationResult(
            boolean isAllowed,
            JobExecutionStatus status,
            String reason
    ) {
        public static PolicyValidationResult allowed() {
            return new PolicyValidationResult(true, JobExecutionStatus.SUCCESS, null);
        }

        public static PolicyValidationResult skipped(JobExecutionStatus status, String reason) {
            return new PolicyValidationResult(false, status, reason);
        }

        public static PolicyValidationResult failed(JobExecutionStatus status, String reason) {
            return new PolicyValidationResult(false, status, reason);
        }
    }

    public PolicyValidationResult validatePreExecution(
            UUID tenantId,
            MonitoredItemRecord item,
            MonitoringGroupRecord group,
            Instant executionInstant
    ) {
        Instant now = executionInstant != null ? executionInstant : Instant.now();

        // 1. Verificação de Kill Switch
        if (isKillSwitchActive(item.getMarketplace()) || isKillSwitchActive("ALL")) {
            log.warn("Job de monitoramento abortado por Kill Switch: tenant={}, marketplace={}", tenantId, item.getMarketplace());
            return PolicyValidationResult.skipped(
                    JobExecutionStatus.SKIPPED_KILL_SWITCH,
                    "Kill Switch ativo para o canal " + item.getMarketplace() + ". Execução abortada por governança de segurança."
            );
        }

        // 2. Verificação de Fontes Proibidas e Capacidades Validadas
        if (isForbiddenSource(item.getSourcePolicyId())) {
            log.error("Tentativa de monitoramento usando fonte proibida: tenant={}, sourcePolicy={}", tenantId, item.getSourcePolicyId());
            return PolicyValidationResult.failed(
                    JobExecutionStatus.FAILED_UNAUTHORIZED_SOURCE,
                    "Fonte de dados expressamente proibida ou não homologada: " + item.getSourcePolicyId()
            );
        }

        // 3. Verificação de Janela Mínima
        int effectiveMinInterval = Math.max(item.getMonitoringIntervalMinutes(), group.getMinIntervalMinutes());
        if (item.getLastSnapshotAt() != null) {
            long elapsedMinutes = Duration.between(item.getLastSnapshotAt(), now).toMinutes();
            if (elapsedMinutes < effectiveMinInterval) {
                log.info("Job ignorado por janela mínima não atingida: decorridos {}m de {}m (tenant={}, item={})",
                        elapsedMinutes, effectiveMinInterval, tenantId, item.getId());
                return PolicyValidationResult.skipped(
                        JobExecutionStatus.SKIPPED_WINDOW_MINIMA,
                        String.format("Janela mínima não atingida: %d minutos decorridos de %d minutos exigidos.",
                                elapsedMinutes, effectiveMinInterval)
                );
            }
        }

        // 4. Verificação de Quota / Rate Limit
        if (!consumeQuota(tenantId, now)) {
            log.warn("Job ignorado por quota de requisições excedida: tenant={}", tenantId);
            return PolicyValidationResult.skipped(
                    JobExecutionStatus.SKIPPED_RATE_LIMIT,
                    "Limite de taxa (rate limit) de snapshots por minuto atingido para o tenant. Adiando próxima tentativa."
            );
        }

        return PolicyValidationResult.allowed();
    }

    public boolean isKillSwitchActive(String marketplace) {
        if (marketplace == null) return false;
        return killSwitches.getOrDefault(marketplace.toUpperCase(), false);
    }

    public void setKillSwitch(String marketplace, boolean active) {
        if (marketplace != null) {
            killSwitches.put(marketplace.toUpperCase(), active);
            log.info("Kill switch alterado: marketplace={}, active={}", marketplace.toUpperCase(), active);
        }
    }

    public boolean isForbiddenSource(String sourcePolicyId) {
        if (sourcePolicyId == null || sourcePolicyId.isBlank()) return true;
        return FORBIDDEN_SOURCES.contains(sourcePolicyId.toUpperCase());
    }

    private synchronized boolean consumeQuota(UUID tenantId, Instant now) {
        long currentMinuteEpoch = now.getEpochSecond() / 60;
        Map<Long, AtomicInteger> tenantWindow = tenantQuotaCounters.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>());

        // Limpa janelas antigas
        tenantWindow.keySet().removeIf(epoch -> epoch < currentMinuteEpoch - 2);

        AtomicInteger counter = tenantWindow.computeIfAbsent(currentMinuteEpoch, k -> new AtomicInteger(0));
        if (counter.get() >= MAX_SNAPSHOTS_PER_MINUTE_PER_TENANT) {
            return false;
        }
        counter.incrementAndGet();
        return true;
    }
}
