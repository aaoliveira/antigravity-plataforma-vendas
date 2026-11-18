package com.sellerintelligence.modules.orderingestion.operations;

import com.sellerintelligence.modules.marketplaceadapters.model.ExecutionContext;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceOperation;
import com.sellerintelligence.modules.orderingestion.model.OrderPageRequest;
import com.sellerintelligence.modules.orderingestion.model.OrderPageResponse;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Operação tipada para leitura de pedidos próprios através do MarketplaceAdapter.
 * Exige expressamente a capacidade oficial READ_OWN_ORDERS.
 */
public class ReadOwnOrdersOperation implements MarketplaceOperation<OrderPageRequest, OrderPageResponse> {

    private final BiFunction<OrderPageRequest, ExecutionContext, OrderPageResponse> executor;

    public ReadOwnOrdersOperation(BiFunction<OrderPageRequest, ExecutionContext, OrderPageResponse> executor) {
        this.executor = Objects.requireNonNull(executor, "executor não pode ser nulo");
    }

    @Override
    public MarketplaceCapability getRequiredCapability() {
        return MarketplaceCapability.READ_OWN_ORDERS;
    }

    @Override
    public String getOperationName() {
        return "SYNC_OWN_ORDERS";
    }

    @Override
    public OrderPageResponse execute(OrderPageRequest input, ExecutionContext context) throws Exception {
        return executor.apply(input, context);
    }
}
