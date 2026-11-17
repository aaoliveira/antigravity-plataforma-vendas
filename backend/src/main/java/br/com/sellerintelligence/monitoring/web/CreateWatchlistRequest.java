package br.com.sellerintelligence.monitoring.web;

import br.com.sellerintelligence.monitoring.domain.WatchlistType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWatchlistRequest(
        @NotBlank(message = "O nome da watchlist é obrigatório")
        @Size(max = 255, message = "O nome não pode exceder 255 caracteres")
        String name,

        String description,

        WatchlistType type
) {}
