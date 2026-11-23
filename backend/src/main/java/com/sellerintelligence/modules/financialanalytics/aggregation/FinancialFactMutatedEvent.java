package com.sellerintelligence.modules.financialanalytics.aggregation;

import java.util.UUID;

/**
 * Evento disparado no barramento interno do Spring sempre que um fato financeiro
 * é computado, reprocessado ou reconciliado com extrato externo.
 * Permite que a camada de cache do tenant seja invalidada de forma reativa e seletiva.
 */
public record FinancialFactMutatedEvent(
        UUID tenantId,
        UUID orderId,
        String mutationType
) {}
