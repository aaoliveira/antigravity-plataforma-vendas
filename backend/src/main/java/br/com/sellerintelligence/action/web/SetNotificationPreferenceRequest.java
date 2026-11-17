package br.com.sellerintelligence.action.web;

import br.com.sellerintelligence.action.domain.NotificationChannel;
import jakarta.validation.constraints.NotNull;

public record SetNotificationPreferenceRequest(
        @NotNull(message = "Canal de notificação é obrigatório")
        NotificationChannel channel,

        boolean optedIn,
        String targetAddress,
        int frequencyLimitPerHour
) {}
