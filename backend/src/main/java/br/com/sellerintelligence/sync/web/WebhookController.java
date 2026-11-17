package br.com.sellerintelligence.sync.web;

import br.com.sellerintelligence.sync.webhook.WebhookService;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @Value("${webhooks.secret:}")
    private String configuredSecret;

    public WebhookController(WebhookService webhookService, ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{marketplace}")
    public ResponseEntity<WebhookService.WebhookProcessResult> handleWebhook(
            @PathVariable Marketplace marketplace,
            @RequestBody String payload,
            @RequestHeader(value = "x-signature", required = false) String mlSignature,
            @RequestHeader(value = "Authorization", required = false) String shopeeSignature) {

        String signature = mlSignature != null ? mlSignature : shopeeSignature;
        log.info("Received webhook for marketplace: {}", marketplace);

        String eventId = null;
        String topic = "general";
        String resourceId = null;
        String externalAccountId = null;

        try {
            JsonNode root = objectMapper.readTree(payload);
            if (marketplace == Marketplace.MERCADO_LIVRE) {
                eventId = root.path("_id").asText(null);
                if (eventId == null) {
                    eventId = root.path("id").asText(null);
                }
                topic = root.path("topic").asText("orders_v2");
                resourceId = root.path("resource").asText(null);
                externalAccountId = root.path("user_id").asText(null);
            } else if (marketplace == Marketplace.SHOPEE) {
                eventId = root.path("data").path("order_sn").asText(null);
                if (eventId == null) {
                    eventId = String.valueOf(root.path("timestamp").asLong());
                }
                topic = "orders";
                resourceId = eventId;
                externalAccountId = root.path("shop_id").asText(null);
            } else {
                eventId = root.path("event_id").asText(root.path("id").asText("event-" + System.currentTimeMillis()));
                topic = root.path("topic").asText("general");
                resourceId = root.path("resource_id").asText(null);
                externalAccountId = root.path("external_account_id").asText(null);
            }
        } catch (Exception e) {
            log.warn("Could not parse webhook JSON payload, fallback to timestamp ID", e);
            eventId = "raw-" + System.currentTimeMillis();
        }

        if (eventId == null) {
            eventId = "evt-" + System.currentTimeMillis();
        }

        WebhookService.WebhookProcessResult result = webhookService.processWebhook(
                marketplace,
                eventId,
                topic,
                resourceId,
                externalAccountId,
                payload,
                signature,
                configuredSecret
        );

        return ResponseEntity.ok(result);
    }
}
