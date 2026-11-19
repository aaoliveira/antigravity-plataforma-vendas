package com.sellerintelligence.modules.aiassistant.draft.catalog;

import com.sellerintelligence.modules.aiassistant.draft.model.MarketplaceRuleLimits;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo de regras oficiais e limites de caracteres por marketplace validado (P020-BE).
 * Restringe geração e edição estritamente aos limites comprovados por documentação oficial de cada canal.
 */
@Component
public class MarketplaceLimitCatalog {

    private static final Map<String, MarketplaceRuleLimits> OFFICIAL_LIMITS = Map.of(
            "MERCADO_LIVRE", new MarketplaceRuleLimits(
                    "MERCADO_LIVRE",
                    1,
                    60,
                    1,
                    25000,
                    true,
                    "Mercado Livre Developers Items API: title máx 60 caracteres; descrição plain text máx 25.000 caracteres"
            ),
            "SHOPEE", new MarketplaceRuleLimits(
                    "SHOPEE",
                    20,
                    120,
                    20,
                    3000,
                    true,
                    "Shopee Open Platform OpenAPI v2: item_name [20-120] caracteres; description [20-3000] caracteres plain text"
            )
    );

    public Optional<MarketplaceRuleLimits> getLimits(String marketplace) {
        if (marketplace == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(OFFICIAL_LIMITS.get(marketplace.toUpperCase(Locale.ROOT)));
    }

    public List<MarketplaceRuleLimits> listAllValidatedLimits() {
        return List.copyOf(OFFICIAL_LIMITS.values());
    }

    public void validateLimits(String marketplace, String title, String description) {
        String normalized = marketplace != null ? marketplace.toUpperCase(Locale.ROOT) : "";
        MarketplaceRuleLimits limits = getLimits(normalized).orElseThrow(() ->
                new IllegalArgumentException("Marketplace '" + marketplace + "' não possui regras oficiais de limites de caracteres validadas no MVP"));

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("O título não pode ser vazio");
        }
        int titleLen = title.trim().length();
        if (titleLen < limits.minTitleLength() || titleLen > limits.maxTitleLength()) {
            throw new IllegalArgumentException(String.format(
                    "Título possui %d caracteres, fora do limite oficial de [%d-%d] caracteres para %s",
                    titleLen, limits.minTitleLength(), limits.maxTitleLength(), normalized
            ));
        }

        if (description != null) {
            int descLen = description.trim().length();
            if (descLen < limits.minDescriptionLength() || descLen > limits.maxDescriptionLength()) {
                throw new IllegalArgumentException(String.format(
                        "Descrição possui %d caracteres, fora do limite oficial de [%d-%d] caracteres para %s",
                        descLen, limits.minDescriptionLength(), limits.maxDescriptionLength(), normalized
                ));
            }
        }
    }
}
