package br.com.sellerintelligence.action.service;

import br.com.sellerintelligence.action.domain.ActionAlert;
import br.com.sellerintelligence.action.domain.NotificationChannel;

public interface NotificationSender {

    boolean send(ActionAlert alert, NotificationChannel channel, String recipient);
}
