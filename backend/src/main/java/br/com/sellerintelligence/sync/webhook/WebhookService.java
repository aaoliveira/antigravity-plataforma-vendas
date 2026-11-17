package br.com.sellerintelligence.sync.webhook;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.sync.domain.*;
import br.com.sellerintelligence.sync.service.SyncJobService;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookEventRepository webhookEventRepository;
    private final StoreRepository storeRepository;
    private final SyncJobService syncJobService;

    public WebhookService(
            WebhookEventRepository webhookEventRepository,
            StoreRepository storeRepository,
            SyncJobService syncJobService) {
        this.webhookEventRepository = webhookEventRepository;
        this.storeRepository = storeRepository;
        this.syncJobService = syncJobService;
    }

    public boolean verifySignature(String secret, String payload, String signature) {
        if (secret == null || secret.isBlank() || signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Error verifying webhook signature", e);
            return false;
        }
    }

    @Transactional
    public WebhookProcessResult processWebhook(Marketplace marketplace, String externalEventId,
                                              String topic, String resourceId, String externalAccountId,
                                              String payload, String signature, String secret) {
        if (externalEventId == null || externalEventId.isBlank()) {
            throw new BusinessException("INVALID_WEBHOOK_EVENT", "Webhook event ID cannot be null or blank", HttpStatus.BAD_REQUEST);
        }

        // Deduplication check
        if (webhookEventRepository.existsByMarketplaceAndExternalEventId(marketplace, externalEventId)) {
            log.info("Duplicate webhook event ignored: marketplace={}, eventId={}", marketplace, externalEventId);
            return new WebhookProcessResult(externalEventId, WebhookStatus.DUPLICATE_IGNORED, "Event already received and processed");
        }

        // Signature check if secret is configured
        if (secret != null && !secret.isBlank() && !verifySignature(secret, payload != null ? payload : "", signature)) {
            log.warn("Invalid webhook signature for marketplace={}, eventId={}", marketplace, externalEventId);
            throw new BusinessException("INVALID_SIGNATURE", "Webhook signature verification failed", HttpStatus.UNAUTHORIZED);
        }

        // Find store by marketplace and external account ID
        Store store = storeRepository.findByMarketplaceAndExternalAccountId(marketplace, externalAccountId)
                .orElse(null);

        UUID tenantId = store != null ? store.getTenantId() : null;

        WebhookEvent event = new WebhookEvent(tenantId, marketplace, externalEventId, topic, resourceId);
        event = webhookEventRepository.save(event);

        if (store != null) {
            SyncResourceType resourceType = determineResourceType(topic);
            if (resourceType != null) {
                try {
                    syncJobService.triggerSyncForStore(store, resourceType, SyncTriggerType.WEBHOOK);
                    event.markProcessed();
                } catch (Exception e) {
                    log.error("Failed to trigger sync from webhook event {}", externalEventId, e);
                    event.markFailed(e.getMessage());
                }
            } else {
                event.markProcessed();
            }
        } else {
            log.warn("Store not found for webhook: marketplace={}, externalAccountId={}", marketplace, externalAccountId);
            event.markProcessed();
        }

        webhookEventRepository.save(event);
        return new WebhookProcessResult(externalEventId, event.getStatus(), "Processed");
    }

    private SyncResourceType determineResourceType(String topic) {
        if (topic == null) return null;
        String lower = topic.toLowerCase();
        if (lower.contains("order")) {
            return SyncResourceType.ORDERS;
        }
        if (lower.contains("item") || lower.contains("product") || lower.contains("catalog")) {
            return SyncResourceType.LISTINGS;
        }
        return null;
    }

    public record WebhookProcessResult(
            String eventId,
            WebhookStatus status,
            String message
    ) {}
}
