package com.sellerintelligence.modules.marketplaceadapters.model;

/**
 * Estados padronizados de capacidade de integração para proteção contra scraping e chamadas indevidas.
 * Conforme estabelecido no PRD (RF-005, RF-006) e em MARKETPLACE_POLICIES.md.
 */
public enum CapabilityStatus {

    /**
     * Capacidade oficialmente suportada pela API do marketplace, com escopos homologados
     * e limites de taxa respeitados.
     */
    SUPPORTED,

    /**
     * Capacidade não disponibilizada oficialmente pelo marketplace.
     * Proibido tentar obter esse dado por scraping hostil ou engenharia reversa.
     */
    UNSUPPORTED,

    /**
     * Capacidade tecnicamente possível, porém sujeita a termos rígidos, aprovação prévia
     * do canal ou desabilitada no MVP (ex: operações de escrita).
     */
    RESTRICTED,

    /**
     * Capacidade ainda não validada formalmente com credenciais de desenvolvedor ou
     * documentação oficial atualizada. Permanece bloqueada até comprovação documental.
     */
    UNVERIFIED
}
