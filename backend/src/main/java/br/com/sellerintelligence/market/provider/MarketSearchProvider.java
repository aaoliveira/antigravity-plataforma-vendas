package br.com.sellerintelligence.market.provider;

import br.com.sellerintelligence.tenancy.store.Marketplace;

public interface MarketSearchProvider {
    Marketplace getMarketplace();
    MarketSearchResult search(MarketSearchQuery query);
    MarketProductDetail getItemDetail(String externalItemId);
}
