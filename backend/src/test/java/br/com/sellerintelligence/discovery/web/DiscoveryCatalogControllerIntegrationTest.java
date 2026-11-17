package br.com.sellerintelligence.discovery.web;

import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.discovery.domain.*;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.MembershipStatus;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.domain.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class DiscoveryCatalogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private DiscoveryCatalogRepository catalogRepository;

    @Autowired
    private DiscoveryCatalogItemRepository itemRepository;

    @Autowired
    private DiscoveryProductMatchRepository matchRepository;

    @Autowired
    private DiscoveryVisualSearchRepository visualSearchRepository;

    private Organization org;
    private User user;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAll();
        itemRepository.deleteAll();
        catalogRepository.deleteAll();
        visualSearchRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("sub-disc-user", "disc@seller.com", "Discovery User"));
        org = organizationRepository.save(new Organization("Discovery Org", "disc-org"));
        membershipRepository.save(new Membership(org, user, Role.OWNER, MembershipStatus.ACTIVE));
    }

    private byte[] createSamplePdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("Teclado Mecanico RGB Switch Blue SKU: TEC-RGB-01 EAN: 7891234567895 Preco: R$ 89,90");
                stream.endText();
            }
            doc.save(baos);
        }
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Should execute full Discovery workflow: Upload PDF, list items, review item, generate matches, confirm match and visual search")
    void fullDiscoveryWorkflow() throws Exception {
        // 1. Upload valid PDF catalog
        byte[] pdfBytes = createSamplePdf();
        String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
        UploadCatalogRequest uploadReq = new UploadCatalogRequest("catalogo_tech.pdf", pdfBase64, "Distribuidora Tech Ltda");

        String uploadJson = mockMvc.perform(post("/api/v1/discovery/catalogs/upload")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.filename").value("catalogo_tech.pdf"))
                .andExpect(jsonPath("$.supplierName").value("Distribuidora Tech Ltda"))
                .andExpect(jsonPath("$.status").value("PARSED"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andReturn().getResponse().getContentAsString();

        UUID catalogId = UUID.fromString(objectMapper.readTree(uploadJson).get("id").asText());

        // 2. List catalogs
        mockMvc.perform(get("/api/v1/discovery/catalogs")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(catalogId.toString()));

        // 3. List catalog items (initial state: PENDING_REVIEW)
        String itemsJson = mockMvc.perform(get("/api/v1/discovery/catalogs/" + catalogId + "/items")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Teclado Mecanico RGB Switch Blue"))
                .andExpect(jsonPath("$[0].reviewStatus").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();

        UUID itemId = UUID.fromString(objectMapper.readTree(itemsJson).get(0).get("id").asText());

        // 4. Human Review of the extracted item (Confirming with corrected fields)
        ReviewItemRequest reviewReq = new ReviewItemRequest(
                "Teclado Mecânico RGB Switch Blue ABNT2",
                "TEC-RGB-01-REV",
                "7891234567895",
                "Teclado mecânico gamer ABNT2 com iluminação RGB e switch blue audível.",
                new BigDecimal("85.00"),
                new BigDecimal("0.850"),
                "35x12x4 cm"
        );

        mockMvc.perform(put("/api/v1/discovery/items/" + itemId + "/review")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Teclado Mecânico RGB Switch Blue ABNT2"))
                .andExpect(jsonPath("$.supplierSku").value("TEC-RGB-01-REV"))
                .andExpect(jsonPath("$.reviewStatus").value("REVIEWED"));

        // 5. Generate similarity matches
        String matchesJson = mockMvc.perform(post("/api/v1/discovery/items/" + itemId + "/matches")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("CANDIDATE"))
                .andExpect(jsonPath("$[0].similarityScore").isNotEmpty())
                .andExpect(jsonPath("$[0].estimatedGrossMargin").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        UUID matchId = UUID.fromString(objectMapper.readTree(matchesJson).get(0).get("id").asText());

        // 6. Confirm similarity match
        mockMvc.perform(post("/api/v1/discovery/matches/" + matchId + "/confirm")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 7. Visual Search
        byte[] pngBytes = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
        };
        String pngBase64 = Base64.getEncoder().encodeToString(pngBytes);
        UploadCatalogRequest imageReq = new UploadCatalogRequest("busca_produto.png", pngBase64, null);

        mockMvc.perform(post("/api/v1/discovery/visual-search")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(imageReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchId").isNotEmpty())
                .andExpect(jsonPath("$.mimeType").value("image/png"))
                .andExpect(jsonPath("$.candidates.length()").value(2));

        // 8. Delete Catalog
        mockMvc.perform(delete("/api/v1/discovery/catalogs/" + catalogId)
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isNoContent());

        assertThat(catalogRepository.findById(catalogId)).isEmpty();
        assertThat(itemRepository.findByCatalogIdAndTenantId(catalogId, org.getId())).isEmpty();
    }
}
