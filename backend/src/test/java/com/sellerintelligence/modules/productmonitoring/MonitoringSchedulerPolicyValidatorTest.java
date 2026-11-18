package com.sellerintelligence.modules.productmonitoring;

import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemRecord;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoringGroupRecord;
import com.sellerintelligence.modules.productmonitoring.model.JobExecutionStatus;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupStatus;
import com.sellerintelligence.modules.productmonitoring.service.MonitoringSchedulerPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MonitoringSchedulerPolicyValidatorTest {

    private MonitoringSchedulerPolicyValidator validator;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        validator = new MonitoringSchedulerPolicyValidator();
    }

    @Test
    @DisplayName("Deve permitir execução quando todos os critérios de conformidade forem satisfeitos")
    void shouldAllowWhenAllCriteriaMet() {
        Instant now = Instant.now();
        Instant lastRun = now.minusSeconds(3600); // Executado há 60 min

        MonitoringGroupRecord group = new MonitoringGroupRecord(
                groupId, tenantId, "Smartphones", null, "#3B82F6", 30, 90,
                MonitoringGroupStatus.ACTIVE, null, now.minusSeconds(7200), now.minusSeconds(7200)
        );

        MonitoredItemRecord item = new MonitoredItemRecord(
                itemId, tenantId, groupId, "MERCADO_LIVRE", "POLICY_MELI_OFFICIAL", "MLB-101",
                "Smartphone", new BigDecimal("3000.00"), new BigDecimal("3000.00"), "BRL",
                null, null, "NEW", MonitoredItemStatus.ACTIVE, 30, lastRun, now,
                lastRun, null, now.minusSeconds(7200), now.minusSeconds(7200)
        );

        var result = validator.validatePreExecution(tenantId, item, group, now);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.status()).isEqualTo(JobExecutionStatus.SUCCESS);
    }

    @Test
    @DisplayName("Deve abortar execução com SKIPPED_KILL_SWITCH quando kill switch estiver ativo para o canal")
    void shouldSkipWhenKillSwitchActive() {
        validator.setKillSwitch("MERCADO_LIVRE", true);

        Instant now = Instant.now();
        MonitoringGroupRecord group = new MonitoringGroupRecord(
                groupId, tenantId, "Smartphones", null, "#3B82F6", 30, 90,
                MonitoringGroupStatus.ACTIVE, null, now, now
        );

        MonitoredItemRecord item = new MonitoredItemRecord(
                itemId, tenantId, groupId, "MERCADO_LIVRE", "POLICY_MELI_OFFICIAL", "MLB-101",
                "Smartphone", new BigDecimal("3000.00"), new BigDecimal("3000.00"), "BRL",
                null, null, "NEW", MonitoredItemStatus.ACTIVE, 30, null, now,
                null, null, now, now
        );

        var result = validator.validatePreExecution(tenantId, item, group, now);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.status()).isEqualTo(JobExecutionStatus.SKIPPED_KILL_SWITCH);
        assertThat(result.reason()).contains("Kill Switch ativo para o canal MERCADO_LIVRE");
    }

    @Test
    @DisplayName("Deve ignorar job com SKIPPED_WINDOW_MINIMA se intervalo mínimo não foi decorrido")
    void shouldSkipWhenMinIntervalNotElapsed() {
        Instant now = Instant.now();
        Instant lastRun = now.minusSeconds(600); // 10 minutos decorridos (mínimo exigido = 60 min)

        MonitoringGroupRecord group = new MonitoringGroupRecord(
                groupId, tenantId, "Smartphones", null, "#3B82F6", 60, 90,
                MonitoringGroupStatus.ACTIVE, null, now, now
        );

        MonitoredItemRecord item = new MonitoredItemRecord(
                itemId, tenantId, groupId, "MERCADO_LIVRE", "POLICY_MELI_OFFICIAL", "MLB-101",
                "Smartphone", new BigDecimal("3000.00"), new BigDecimal("3000.00"), "BRL",
                null, null, "NEW", MonitoredItemStatus.ACTIVE, 60, lastRun, now,
                lastRun, null, now, now
        );

        var result = validator.validatePreExecution(tenantId, item, group, now);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.status()).isEqualTo(JobExecutionStatus.SKIPPED_WINDOW_MINIMA);
        assertThat(result.reason()).contains("Janela mínima não atingida");
    }

    @Test
    @DisplayName("Deve falhar com FAILED_UNAUTHORIZED_SOURCE se o item tentar usar fonte proibida")
    void shouldFailWhenSourceIsForbidden() {
        Instant now = Instant.now();
        MonitoringGroupRecord group = new MonitoringGroupRecord(
                groupId, tenantId, "Smartphones", null, "#3B82F6", 30, 90,
                MonitoringGroupStatus.ACTIVE, null, now, now
        );

        MonitoredItemRecord item = new MonitoredItemRecord(
                itemId, tenantId, groupId, "MERCADO_LIVRE", "UNLICENSED_SCRAPER", "MLB-101",
                "Smartphone", new BigDecimal("3000.00"), new BigDecimal("3000.00"), "BRL",
                null, null, "NEW", MonitoredItemStatus.ACTIVE, 30, null, now,
                null, null, now, now
        );

        var result = validator.validatePreExecution(tenantId, item, group, now);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.status()).isEqualTo(JobExecutionStatus.FAILED_UNAUTHORIZED_SOURCE);
        assertThat(result.reason()).contains("Fonte de dados expressamente proibida");
    }

    @Test
    @DisplayName("Deve ignorar job com SKIPPED_RATE_LIMIT se o limite de taxa do tenant for ultrapassado")
    void shouldSkipWhenRateLimitExceeded() {
        Instant now = Instant.now();
        MonitoringGroupRecord group = new MonitoringGroupRecord(
                groupId, tenantId, "Smartphones", null, "#3B82F6", 15, 90,
                MonitoringGroupStatus.ACTIVE, null, now, now
        );

        MonitoredItemRecord item = new MonitoredItemRecord(
                itemId, tenantId, groupId, "MERCADO_LIVRE", "POLICY_MELI_OFFICIAL", "MLB-101",
                "Smartphone", new BigDecimal("3000.00"), new BigDecimal("3000.00"), "BRL",
                null, null, "NEW", MonitoredItemStatus.ACTIVE, 15, null, now,
                null, null, now, now
        );

        // Esgota a quota de 60 chamadas por minuto
        for (int i = 0; i < 60; i++) {
            validator.validatePreExecution(tenantId, item, group, now);
        }

        // A 61ª chamada deve ser rejeitada por rate limit
        var result = validator.validatePreExecution(tenantId, item, group, now);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.status()).isEqualTo(JobExecutionStatus.SKIPPED_RATE_LIMIT);
        assertThat(result.reason()).contains("Limite de taxa (rate limit)");
    }
}
