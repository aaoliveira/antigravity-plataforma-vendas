package br.com.sellerintelligence.monitoring.web;

import br.com.sellerintelligence.monitoring.domain.WatchlistItemType;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddWatchlistItemRequest(
        @NotNull(message = "O marketplace é obrigatório")
        Marketplace marketplace,

        @NotNull(message = "O tipo do item é obrigatório (LISTING ou SELLER)")
        WatchlistItemType itemType,

        @NotBlank(message = "O identificador externo é obrigatório")
        @Size(max = 150)
        String externalId,

        @NotBlank(message = "O nome/título é obrigatório")
        @Size(max = 500)
        String targetName,

        String targetUrl,

        String sellerNickname,

        Integer checkFrequencyMinutes
) {}
