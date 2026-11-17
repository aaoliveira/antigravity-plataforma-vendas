package br.com.sellerintelligence.sync.infrastructure.rabbitmq;

import br.com.sellerintelligence.sync.service.SyncJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class DirectSyncJobPublisher implements SyncJobMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(DirectSyncJobPublisher.class);

    private final SyncJobService syncJobService;

    public DirectSyncJobPublisher(@Lazy SyncJobService syncJobService) {
        this.syncJobService = syncJobService;
    }

    @Override
    public void publishSyncJob(SyncJobMessage message) {
        log.info("DirectSyncJobPublisher executing sync job directly (test profile): jobId={}", message.jobId());
        syncJobService.processJob(message.jobId());
    }
}
