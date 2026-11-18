package com.sellerintelligence.modules.marketplaceadapters.api;

import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityRegistry;
import com.sellerintelligence.modules.marketplaceadapters.model.ExecutionContext;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceOperation;

/**
 * Contrato unificado para todos os conectores de marketplaces.
 * Governa a execução de chamadas externas sob estritas salvaguardas de resiliência e políticas do PRD.
 */
public interface MarketplaceAdapter {

    /**
     * Identificador padronizado do canal (ex: "MERCADO_LIVRE", "SHOPEE", "AMAZON").
     */
    String getMarketplaceId();

    /**
     * Registro de capacidades homologadas e suas respectivas evidências documentais.
     */
    CapabilityRegistry getCapabilityRegistry();

    /**
     * Executa uma operação através do adaptador com toda a esteira de resiliência ativa.
     *
     * @param operation operação tipada a ser executada
     * @param input dados de entrada
     * @param context contexto autenticado do tenant
     * @return resultado da operação
     * @throws Exception caso a operação ou a esteira de resiliência falhe
     */
    <T, R> R execute(MarketplaceOperation<T, R> operation, T input, ExecutionContext context) throws Exception;

    /**
     * Verifica o estado de conectividade e integridade do conector.
     */
    boolean isHealthy();
}
