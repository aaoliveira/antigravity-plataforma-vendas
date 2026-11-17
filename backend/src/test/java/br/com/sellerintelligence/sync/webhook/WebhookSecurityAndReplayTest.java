package br.com.sellerintelligence.sync.webhook;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.sync.domain.WebhookEvent;
import br.com.sellerintelligence.sync.domain.WebhookEventRepository;
import br.com.sellerintelligence.sync.domain.WebhookStatus;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WebhookSecurityAndReplayTest {

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    private UUID tenantId;
    private Store store;
    private final String webhookSecret = "secret-test-webhook-key-123456";

    @BeforeEach
    void setUp() {
        Organization org = new Organization("Webhook Org", "webhook-org");
        org = organizationRepository.save(org);
        tenantId = org.getId();

        Company company = new Company(tenantId, "11222333000144", "Web Razao", "Web Fantasia", TaxRegime.SIMPLES_NACIONAL);
        company = companyRepository.save(company);

        store = new Store(tenantId, company, Marketplace.MERCADO_LIVRE, "Loja ML Webhook");
        store.setExternalAccountId("123456789");
        store = storeRepository.save(store);
    }

    private String calculateHmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should successfully process valid webhook with verified signature")
    void shouldProcessValidWebhookWithSignature() {
        String payload = "{\"resource\":\"/orders/200001\",\"user_id\":\"123456789\",\"topic\":\"orders_v2\"}";
        String signature = calculateHmac(payload, webhookSecret);
        String eventId = "EVT-" + UUID.randomUUID();

        WebhookService.WebhookProcessResult result = webhookService.processWebhook(
                Marketplace.MERCADO_LIVRE, eventId, "orders_v2", "/orders/200001",
                "123456789", payload, signature, webhookSecret
        );

        assertThat(result.eventId()).isEqualTo(eventId);
        assertThat(result.status()).isEqualTo(WebhookStatus.PROCESSED);

        WebhookEvent event = webhookEventRepository.findByMarketplaceAndExternalEventId(Marketplace.MERCADO_LIVRE, eventId).orElseThrow();
        assertThat(event.getStatus()).isEqualTo(WebhookStatus.PROCESSED);
        assertThat(event.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should reject webhook with invalid signature with 401 UNAUTHORIZED")
    void shouldRejectWebhookWithInvalidSignature() {
        String payload = "{\"resource\":\"/orders/200001\"}";
        String invalidSignature = "deadbeef00112233445566778899aabbccddeeff";
        String eventId = "EVT-BAD-" + UUID.randomUUID();

        assertThatThrownBy(() -> webhookService.processWebhook(
                Marketplace.MERCADO_LIVRE, eventId, "orders_v2", "/orders/200001",
                "123456789", payload, invalidSignature, webhookSecret
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("signature verification failed");
    }

    @Test
    @DisplayName("Should detect replay/duplicate webhook and ignore with DUPLICATE_IGNORED")
    void shouldIgnoreDuplicateWebhook() {
        String payload = "{\"resource\":\"/items/MLB123\"}";
        String signature = calculateHmac(payload, webhookSecret);
        String eventId = "EVT-DUPLICATE-CHECK";

        // First execution
        WebhookService.WebhookProcessResult result1 = webhookService.processWebhook(
                Marketplace.MERCADO_LIVRE, eventId, "items", "MLB123",
                "123456789", payload, signature, webhookSecret
        );
        assertThat(result1.status()).isEqualTo(WebhookStatus.PROCESSED);

        // Replay of same event
        WebhookService.WebhookProcessResult result2 = webhookService.processWebhook(
                Marketplace.MERCADO_LIVRE, eventId, "items", "MLB123",
                "123456789", payload, signature, webhookSecret
        );
        assertThat(result2.status()).isEqualTo(WebhookStatus.DUPLICATE_IGNORED);
        assertThat(result2.message()).contains("already received and processed");
    }
}
