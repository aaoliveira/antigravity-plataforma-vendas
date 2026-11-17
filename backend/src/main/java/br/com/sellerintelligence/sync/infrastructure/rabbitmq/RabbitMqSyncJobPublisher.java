package br.com.sellerintelligence.sync.infrastructure.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@ConditionalOnBean(RabbitTemplate.class)
public class RabbitMqSyncJobPublisher implements SyncJobMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqSyncJobPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqSyncJobPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishSyncJob(SyncJobMessage message) {
        log.info("Publishing sync job message to RabbitMQ: jobId={}, tenantId={}, attempt={}",
                message.jobId(), message.tenantId(), message.attempt());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SYNC_EXCHANGE,
                RabbitMQConfig.SYNC_ROUTING_KEY,
                message
        );
    }
}
