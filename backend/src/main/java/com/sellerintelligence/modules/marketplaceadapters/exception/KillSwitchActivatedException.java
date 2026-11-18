package com.sellerintelligence.modules.marketplaceadapters.exception;

/**
 * Lançada quando uma chamada a marketplace é tentada enquanto o Kill Switch está ativado.
 */
public class KillSwitchActivatedException extends MarketplaceAdapterException {

    public KillSwitchActivatedException(String marketplaceId) {
        super(marketplaceId, String.format(
                "O Kill Switch para o canal '%s' está ATIVADO. Todas as chamadas para este marketplace estão categoricamente bloqueadas.",
                marketplaceId
        ));
    }
}
