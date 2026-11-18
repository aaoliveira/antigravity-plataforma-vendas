package com.sellerintelligence.modules.marketplaceadapters.model;

/**
 * Operação tipada executada através de um MarketplaceAdapter.
 * Vincula explicitamente a ação a uma MarketplaceCapability exigida para verificação prévia.
 *
 * @param <T> Tipo do input
 * @param <R> Tipo do output
 */
public interface MarketplaceOperation<T, R> {

    /**
     * Capacidade exigida para a execução desta operação.
     */
    MarketplaceCapability getRequiredCapability();

    /**
     * Nome descritivo da operação para logs e telemetria (ex: "GET_ORDERS", "SEARCH_PUBLIC_CATALOG").
     */
    String getOperationName();

    /**
     * Lógica operacional a ser executada no adaptador após passar por toda a esteira de resiliência.
     */
    R execute(T input, ExecutionContext context) throws Exception;

    /**
     * Chave de idempotência para evitar reprocessamento ou requisições duplicadas.
     * Retorna null se a operação não requerer controle de idempotência.
     */
    default String getIdempotencyKey(T input) {
        return null;
    }
}
