package br.com.sellerintelligence.market.provider;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class MarketSearchRegistry {

    private final Map<Marketplace, MarketSearchProvider> providers = new EnumMap<>(Marketplace.class);

    public MarketSearchRegistry(List<MarketSearchProvider> providerList) {
        for (MarketSearchProvider p : providerList) {
            providers.put(p.getMarketplace(), p);
        }
    }

    public MarketSearchProvider getProvider(Marketplace marketplace) {
        Marketplace mkt = marketplace != null ? marketplace : Marketplace.MERCADO_LIVRE;
        MarketSearchProvider provider = providers.get(mkt);
        if (provider == null) {
            throw new BusinessException(
                    "MARKETPLACE_NOT_SUPPORTED_FOR_SEARCH",
                    "Pesquisa de mercado não suportada para o marketplace: " + mkt,
                    HttpStatus.BAD_REQUEST
            );
        }
        return provider;
    }
}
