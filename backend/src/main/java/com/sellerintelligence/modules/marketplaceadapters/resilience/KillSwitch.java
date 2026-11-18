package com.sellerintelligence.modules.marketplaceadapters.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Kill Switch central e por canal para cancelamento instantâneo de chamadas a marketplaces.
 * Atende ao requisito RF-009 de contenção de incidentes e proteção de contas em tempo de execução.
 */
@Component
public class KillSwitch {

    private static final Logger log = LoggerFactory.getLogger(KillSwitch.class);
    private static final String GLOBAL_KEY = "*GLOBAL*";

    private final ConcurrentMap<String, Boolean> killedStatus = new ConcurrentHashMap<>();

    /**
     * Ativa o Kill Switch globalmente (interrompe todos os adaptadores).
     */
    public void activateGlobal() {
        killedStatus.put(GLOBAL_KEY, true);
        log.warn("KILL SWITCH GLOBAL ATIVADO: Todas as chamadas para qualquer marketplace estão suspensas!");
    }

    /**
     * Desativa o Kill Switch global.
     */
    public void deactivateGlobal() {
        killedStatus.remove(GLOBAL_KEY);
        log.info("Kill Switch global desativado.");
    }

    /**
     * Ativa o Kill Switch para um marketplace específico.
     *
     * @param marketplaceId identificador do canal (ex: "MERCADO_LIVRE", "SHOPEE")
     */
    public void activate(String marketplaceId) {
        if (marketplaceId == null || marketplaceId.isBlank()) return;
        String key = marketplaceId.trim().toUpperCase();
        killedStatus.put(key, true);
        log.warn("KILL SWITCH ATIVADO para o canal: {}", key);
    }

    /**
     * Desativa o Kill Switch para um marketplace específico.
     */
    public void deactivate(String marketplaceId) {
        if (marketplaceId == null || marketplaceId.isBlank()) return;
        String key = marketplaceId.trim().toUpperCase();
        killedStatus.remove(key);
        log.info("Kill Switch desativado para o canal: {}", key);
    }

    /**
     * Verifica se o canal especificado está bloqueado pelo Kill Switch.
     */
    public boolean isKilled(String marketplaceId) {
        if (Boolean.TRUE.equals(killedStatus.get(GLOBAL_KEY))) {
            return true;
        }
        if (marketplaceId == null) {
            return false;
        }
        return Boolean.TRUE.equals(killedStatus.get(marketplaceId.trim().toUpperCase()));
    }

    /**
     * Reseta todos os kill switches (útil para testes).
     */
    public void resetAll() {
        killedStatus.clear();
    }
}
