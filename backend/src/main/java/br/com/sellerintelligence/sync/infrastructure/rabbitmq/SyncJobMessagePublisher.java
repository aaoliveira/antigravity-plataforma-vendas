package br.com.sellerintelligence.sync.infrastructure.rabbitmq;

public interface SyncJobMessagePublisher {
    void publishSyncJob(SyncJobMessage message);
}
