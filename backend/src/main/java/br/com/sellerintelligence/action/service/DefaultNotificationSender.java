package br.com.sellerintelligence.action.service;

import br.com.sellerintelligence.action.domain.ActionAlert;
import br.com.sellerintelligence.action.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationSender.class);

    @Override
    public boolean send(ActionAlert alert, NotificationChannel channel, String recipient) {
        log.info("Dispatching notification via [{}]: recipient={}, alertId={}, title='{}', severity={}",
                channel, recipient, alert.getId(), alert.getTitle(), alert.getSeverity());
        // Simulates external dispatch (e.g. SMTP or Webhook)
        return true;
    }
}
