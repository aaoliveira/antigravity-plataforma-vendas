package com.sellerintelligence.modules.catalog;

import com.sellerintelligence.modules.catalog.model.ListingSyncResult;

/**
 * Exceção lançada quando ocorre falha parcial ou total durante a sincronização de catálogo.
 * Preserva o resultado e o checkpoint para viabilizar a retomada segura da sincronização.
 */
public class CatalogSyncException extends RuntimeException {

    private final ListingSyncResult partialResult;

    public CatalogSyncException(String message, ListingSyncResult partialResult, Throwable cause) {
        super(message, cause);
        this.partialResult = partialResult;
    }

    public ListingSyncResult getPartialResult() {
        return partialResult;
    }
}
