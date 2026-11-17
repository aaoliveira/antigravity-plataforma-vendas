package br.com.sellerintelligence.action.service;

import br.com.sellerintelligence.action.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionAlertRuleServiceTest {

    @Mock
    private ActionAlertRuleRepository ruleRepository;

    @Mock
    private ActionAlertRepository alertRepository;

    private ActionAlertRuleService ruleService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ruleService = new ActionAlertRuleService(ruleRepository, alertRepository);
    }

    @Test
    @DisplayName("Should create alert rule with valid scope, threshold and severity")
    void shouldCreateAlertRule() {
        when(ruleRepository.save(any(ActionAlertRule.class))).thenAnswer(inv -> inv.getArgument(0));

        ActionAlertRule rule = ruleService.createRule(
                tenantId, "Margem Mínima Catálogo", AlertRuleType.MARGIN_THRESHOLD,
                AlertScopeType.ORGANIZATION, null,
                new BigDecimal("15.00"), ThresholdOperator.LESS_THAN,
                60, 120, ActionAlertSeverity.CRITICAL
        );

        assertThat(rule).isNotNull();
        assertThat(rule.getName()).isEqualTo("Margem Mínima Catálogo");
        assertThat(rule.getRuleType()).isEqualTo(AlertRuleType.MARGIN_THRESHOLD);
        assertThat(rule.getThresholdValue()).isEqualByComparingTo("15.00");
        assertThat(rule.getThresholdOperator()).isEqualTo(ThresholdOperator.LESS_THAN);
        assertThat(rule.getSeverity()).isEqualTo(ActionAlertSeverity.CRITICAL);
        assertThat(rule.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should simulate rule and trigger alert when observed value violates threshold")
    void shouldSimulateRuleAndTrigger() {
        ActionAlertRule rule = new ActionAlertRule(
                tenantId, "Ruptura de Estoque", AlertRuleType.STOCK_DEPLETION,
                AlertScopeType.STORE, "store-123",
                new BigDecimal("5.00"), ThresholdOperator.LESS_THAN,
                60, 60, ActionAlertSeverity.WARNING
        );
        when(ruleRepository.findByIdAndTenantId(rule.getId(), tenantId)).thenReturn(Optional.of(rule));
        when(alertRepository.save(any(ActionAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        RuleSimulationResult result = ruleService.simulateRule(
                tenantId, rule.getId(), new BigDecimal("2.00"), "SKU-ABC", true
        );

        assertThat(result.triggered()).isTrue();
        assertThat(result.observedValue()).isEqualByComparingTo("2.00");
        assertThat(result.thresholdValue()).isEqualByComparingTo("5.00");
        assertThat(result.explanation()).contains("Simulação DISPAROU");
        assertThat(result.generatedAlertId()).isNotNull();

        verify(alertRepository, times(1)).save(any(ActionAlert.class));
    }

    @Test
    @DisplayName("Should simulate rule and not trigger when observed value is within bounds")
    void shouldSimulateRuleAndNotTrigger() {
        ActionAlertRule rule = new ActionAlertRule(
                tenantId, "Margem Mínima", AlertRuleType.MARGIN_THRESHOLD,
                AlertScopeType.ORGANIZATION, null,
                new BigDecimal("12.00"), ThresholdOperator.LESS_THAN,
                60, 120, ActionAlertSeverity.CRITICAL
        );
        when(ruleRepository.findByIdAndTenantId(rule.getId(), tenantId)).thenReturn(Optional.of(rule));

        RuleSimulationResult result = ruleService.simulateRule(
                tenantId, rule.getId(), new BigDecimal("18.50"), "SKU-XYZ", false
        );

        assertThat(result.triggered()).isFalse();
        assertThat(result.explanation()).contains("Simulação NÃO disparou");
        assertThat(result.generatedAlertId()).isNull();

        verify(alertRepository, never()).save(any(ActionAlert.class));
    }
}
