package br.com.sellerintelligence.market.provider;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ShopeeMarketSearchProvider implements MarketSearchProvider {

    @Override
    public Marketplace getMarketplace() {
        return Marketplace.SHOPEE;
    }

    @Override
    public MarketSearchResult search(MarketSearchQuery query) {
        // Strict adherence to anti-hallucination and capability matrix (docs/architecture/marketplace-capability-matrix.md):
        // Shopee Open Platform API v2 does not provide a public unauthenticated keyword search endpoint.
        // Scraping não oficial é expressamente proibido pelas diretrizes de engenharia e termos de serviço.
        return new MarketSearchResult(
                query.query(),
                0,
                query.offset(),
                query.limit(),
                List.of(),
                Instant.now(),
                DataProvenance.UNAVAILABLE
        );
    }

    @Override
    public MarketProductDetail getItemDetail(String externalItemId) {
        throw new BusinessException(
                "UNSUPPORTED_CAPABILITY",
                "Consulta pública de item avulso não está disponível na API oficial pública da Shopee sem loja autenticada vinculada.",
                HttpStatus.NOT_IMPLEMENTED
        );
    }
}
