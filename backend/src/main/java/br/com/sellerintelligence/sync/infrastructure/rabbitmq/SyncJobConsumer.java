package br.com.sellerintelligence.sync.infrastructure.rabbitmq;

import br.com.sellerintelligence.sync.service.SyncJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.annotation.RabbitListener")
public class SyncJobConsumer {

    private static final Logger log = LoggerFactory.getLogger(SyncJobConsumer.class);

    private final SyncJobService syncJobService;

    public SyncJobConsumer(SyncJobService syncJobService) {
        this.syncJobService = syncJobService;
    }

    @RabbitListener(queues = RabbitMQConfig.SYNC_QUEUE)
    public void consumeSyncJob(SyncJobMessage message) {
        log.info("Received sync job message from queue: jobId={}, tenantId={}, attempt={}",
                message.jobId(), message.tenantId(), message.attempt());
        syncJobService.processJob(message.jobId());
    }
}
