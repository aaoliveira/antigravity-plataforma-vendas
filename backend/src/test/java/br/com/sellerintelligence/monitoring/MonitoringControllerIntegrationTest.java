package br.com.sellerintelligence.monitoring;

import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.monitoring.domain.Watchlist;
import br.com.sellerintelligence.monitoring.domain.WatchlistItem;
import br.com.sellerintelligence.monitoring.domain.WatchlistRepository;
import br.com.sellerintelligence.monitoring.domain.WatchlistItemRepository;
import br.com.sellerintelligence.monitoring.domain.WatchlistType;
import br.com.sellerintelligence.monitoring.domain.WatchlistItemType;
import br.com.sellerintelligence.monitoring.domain.WatchlistItemStatus;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.MembershipStatus;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.domain.Role;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class MonitoringControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    private Organization org;
    private User user;

    @BeforeEach
    void setUp() {
        watchlistItemRepository.deleteAll();
        watchlistRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("sub-mon-user", "mon@seller.com", "Mon User"));
        org = organizationRepository.save(new Organization("Mon Org", "mon-org"));
        membershipRepository.save(new Membership(org, user, Role.OWNER, MembershipStatus.ACTIVE));
    }

    @Test
    @DisplayName("Should create watchlist, add competitor item, record snapshots and query timeline")
    void fullMonitoringWorkflow() throws Exception {
        // 1. Create Watchlist
        String watchlistPayload = """
                {
                    "name": "Lista Concorrentes Teclados",
                    "description": "Monitoramento de periféricos",
                    "type": "COMPETITOR_LISTING"
                }
                """;

        String watchlistResponse = mockMvc.perform(post("/api/v1/monitoring/watchlists")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(watchlistPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Lista Concorrentes Teclados"))
                .andReturn().getResponse().getContentAsString();

        String watchlistId = com.jayway.jsonpath.JsonPath.read(watchlistResponse, "$.id");

        // 2. Add Item to Watchlist
        String itemPayload = """
                {
                    "marketplace": "MERCADO_LIVRE",
                    "itemType": "LISTING",
                    "externalId": "MLB123456789",
                    "targetName": "Teclado Mecânico Concorrente RGB",
                    "targetUrl": "https://mercadolivre.com.br/sample",
                    "sellerNickname": "concorrente_oficial",
                    "checkFrequencyMinutes": 360
                }
                """;

        String itemResponse = mockMvc.perform(post("/api/v1/monitoring/watchlists/" + watchlistId + "/items")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.externalId").value("MLB123456789"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        String itemId = com.jayway.jsonpath.JsonPath.read(itemResponse, "$.id");

        // 3. Record Snapshot 1 (Price 199.90)
        String snap1Payload = """
                {
                    "price": 199.90,
                    "originalPrice": 229.90,
                    "listingStatus": "ACTIVE",
                    "title": "Teclado Mecânico Concorrente RGB",
                    "sellerNickname": "concorrente_oficial",
                    "sellerReputation": "Platinum",
                    "freeShipping": true,
                    "source": "MERCADO_LIVRE_PUBLIC_API"
                }
                """;

        mockMvc.perform(post("/api/v1/monitoring/items/" + itemId + "/snapshot")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(snap1Payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.price").value(199.90))
                .andExpect(jsonPath("$.provenance").value("OBSERVED"));

        // 4. Record Snapshot 2 (Price drop to 169.90)
        String snap2Payload = """
                {
                    "price": 169.90,
                    "originalPrice": 229.90,
                    "listingStatus": "ACTIVE",
                    "title": "Teclado Mecânico Concorrente RGB",
                    "sellerNickname": "concorrente_oficial",
                    "sellerReputation": "Platinum",
                    "freeShipping": true,
                    "source": "MERCADO_LIVRE_PUBLIC_API"
                }
                """;

        mockMvc.perform(post("/api/v1/monitoring/items/" + itemId + "/snapshot")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(snap2Payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(169.90));

        // 5. Query Timeline
        mockMvc.perform(get("/api/v1/monitoring/items/" + itemId + "/timeline")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(itemId))
                .andExpect(jsonPath("$.totalObservations").value(2))
                .andExpect(jsonPath("$.coveragePercentage").value(100.0))
                .andExpect(jsonPath("$.points.length()").value(2));

        // 6. Create Alert Rule
        String rulePayload = """
                {
                    "watchlistId": "%s",
                    "name": "Alerta de Queda de Preço",
                    "eventType": "PRICE_CHANGE",
                    "minPriceDeltaPercentage": 5.0,
                    "direction": "DECREASE",
                    "cooldownMinutes": 1440,
                    "channel": "IN_APP"
                }
                """.formatted(watchlistId);

        mockMvc.perform(post("/api/v1/monitoring/rules")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rulePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Alerta de Queda de Preço"));

        // 7. Untrack Item (verifying soft-untrack preserving history)
        mockMvc.perform(delete("/api/v1/monitoring/items/" + itemId)
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isNoContent());

        WatchlistItem untracked = watchlistItemRepository.findById(UUID.fromString(itemId)).orElseThrow();
        assertThat(untracked.getStatus()).isEqualTo(WatchlistItemStatus.UNTRACKED);
    }
}
