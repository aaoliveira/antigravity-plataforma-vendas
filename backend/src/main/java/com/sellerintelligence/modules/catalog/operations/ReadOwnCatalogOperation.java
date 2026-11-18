package com.sellerintelligence.modules.catalog.operations;

import com.sellerintelligence.modules.catalog.model.ListingPageRequest;
import com.sellerintelligence.modules.catalog.model.ListingPageResponse;
import com.sellerintelligence.modules.marketplaceadapters.model.ExecutionContext;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceOperation;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Operação tipada para leitura do catálogo de anúncios próprios através do MarketplaceAdapter.
 * Exige expressamente a capacidade oficial READ_OWN_CATALOG.
 */
public class ReadOwnCatalogOperation implements MarketplaceOperation<ListingPageRequest, ListingPageResponse> {

    private final BiFunction<ListingPageRequest, ExecutionContext, ListingPageResponse> executor;

    public ReadOwnCatalogOperation(BiFunction<ListingPageRequest, ExecutionContext, ListingPageResponse> executor) {
        this.executor = Objects.requireNonNull(executor, "executor não pode ser nulo");
    }

    @Override
    public MarketplaceCapability getRequiredCapability() {
        return MarketplaceCapability.READ_OWN_CATALOG;
    }

    @Override
    public String getOperationName() {
        return "SYNC_OWN_LISTINGS";
    }

    @Override
    public ListingPageResponse execute(ListingPageRequest input, ExecutionContext context) throws Exception {
        return executor.apply(input, context);
    }
}
