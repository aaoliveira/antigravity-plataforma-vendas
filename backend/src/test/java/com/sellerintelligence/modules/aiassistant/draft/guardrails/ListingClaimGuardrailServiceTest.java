package com.sellerintelligence.modules.aiassistant.draft.guardrails;

import com.sellerintelligence.modules.aiassistant.draft.model.ClaimValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ListingClaimGuardrailService - Testes de Restrição de Claims e Anti-Alucinação (P020-BE)")
class ListingClaimGuardrailServiceTest {

    private ListingClaimGuardrailService service;

    @BeforeEach
    void setUp() {
        service = new ListingClaimGuardrailService();
    }

    @Test
    @DisplayName("Deve aprovar anúncio que utiliza estritamente atributos confirmados")
    void shouldApproveWhenOnlyConfirmedAttributesUsed() {
        Map<String, Object> confirmedAttributes = Map.of(
                "brand", "Tramontina",
                "category", "Panela",
                "model", "Solar",
                "materials", List.of("Aço Inox"),
                "warranty", "90 dias de garantia legal"
        );
        List<String> keywords = List.of("cozinha", "resistente");

        String title = "Panela Tramontina Solar Aço Inox Cozinha";
        String description = "Panela Tramontina Solar em Aço Inox original. Garantia legal de 90 dias conforme CDC. Produto resistente para cozinha.";

        ClaimValidationResult result = service.validateAndTraceClaims(title, description, confirmedAttributes, keywords);

        assertThat(result.valid()).isTrue();
        assertThat(result.unprovenClaimsDetected()).isEmpty();
        assertThat(result.traceableAttributes()).containsKeys("brand", "category", "model", "warranty");
    }

    @Test
    @DisplayName("Deve detectar e bloquear certificação não confirmada (ex: Inmetro, Anvisa, Anatel)")
    void shouldDetectUnprovenCertification() {
        Map<String, Object> confirmedAttributes = Map.of(
                "brand", "Genérica",
                "category", "Cabo USB"
        );

        String title = "Cabo USB Tipo C Certificado pelo Inmetro";
        String description = "Produto com selo Procel e certificação Anatel.";

        ClaimValidationResult result = service.validateAndTraceClaims(title, description, confirmedAttributes, List.of());

        assertThat(result.valid()).isFalse();
        assertThat(result.unprovenClaimsDetected())
                .anyMatch(claim -> claim.contains("Inmetro"))
                .anyMatch(claim -> claim.contains("Anatel"))
                .anyMatch(claim -> claim.contains("Procel"));
    }

    @Test
    @DisplayName("Deve permitir certificação quando explicitamente confirmada nos atributos")
    void shouldAllowCertificationWhenExplicitlyConfirmed() {
        Map<String, Object> confirmedAttributes = Map.of(
                "brand", "Intelbras",
                "category", "Roteador",
                "technicalSpecifications", Map.of("homologacao", "Certificado Anatel 12345/2026")
        );

        String title = "Roteador Intelbras com Homologação Anatel";
        String description = "Aparelho homologado pela Anatel sob código oficial.";

        ClaimValidationResult result = service.validateAndTraceClaims(title, description, confirmedAttributes, List.of());

        assertThat(result.valid()).isTrue();
        assertThat(result.unprovenClaimsDetected()).isEmpty();
    }

    @Test
    @DisplayName("Deve detectar e bloquear compatibilidade com terceiros não comprovada")
    void shouldDetectUnprovenCompatibility() {
        Map<String, Object> confirmedAttributes = Map.of(
                "brand", "Baseus",
                "category", "Carregador"
        );

        String title = "Carregador Baseus Compatível com iPhone 15 Pro Max";
        String description = "Serve para Samsung Galaxy S24 e funciona com iPad.";

        ClaimValidationResult result = service.validateAndTraceClaims(title, description, confirmedAttributes, List.of());

        assertThat(result.valid()).isFalse();
        assertThat(result.unprovenClaimsDetected())
                .anyMatch(claim -> claim.contains("Compatibilidade não confirmada"));
    }

    @Test
    @DisplayName("Deve detectar e bloquear materiais nobres não comprovados")
    void shouldDetectUnprovenNobleMaterials() {
        Map<String, Object> confirmedAttributes = Map.of(
                "brand", "Fashion",
                "category", "Relógio",
                "materials", List.of("Silicone")
        );

        String title = "Relógio Fashion de Ouro 18k e Titânio";
        String description = "Acabamento em couro legítimo e pulseira blindada.";

        ClaimValidationResult result = service.validateAndTraceClaims(title, description, confirmedAttributes, List.of());

        assertThat(result.valid()).isFalse();
        assertThat(result.unprovenClaimsDetected())
                .anyMatch(claim -> claim.contains("Ouro"))
                .anyMatch(claim -> claim.contains("Titânio"))
                .anyMatch(claim -> claim.contains("couro legítimo"));
    }

    @Test
    @DisplayName("Deve detectar e bloquear garantias abusivas não confirmadas")
    void shouldDetectUnprovenWarranties() {
        Map<String, Object> confirmedAttributes = Map.of(
                "brand", "Tramontina",
                "warranty", "90 dias legal"
        );

        String title = "Faca Tramontina com Garantia Vitalícia";
        String description = "Garantia de 5 anos direto com o fabricante.";

        ClaimValidationResult result = service.validateAndTraceClaims(title, description, confirmedAttributes, List.of());

        assertThat(result.valid()).isFalse();
        assertThat(result.unprovenClaimsDetected())
                .anyMatch(claim -> claim.contains("Garantia não confirmada"));
    }

    @Test
    @DisplayName("Deve detectar e bloquear superlativos enganosos e alegações absolutas")
    void shouldDetectDeceptiveSuperlatives() {
        Map<String, Object> confirmedAttributes = Map.of(
                "brand", "TopHealth",
                "category", "Suplemento"
        );

        String title = "Suplemento TopHealth O Melhor do Brasil";
        String description = "Cura calvície e fórmula milagrosa 100% eficaz sem defeitos.";

        ClaimValidationResult result = service.validateAndTraceClaims(title, description, confirmedAttributes, List.of());

        assertThat(result.valid()).isFalse();
        assertThat(result.unprovenClaimsDetected())
                .anyMatch(claim -> claim.contains("Superlativo ou alegação enganosa"));
    }

    @Test
    @DisplayName("Deve detectar dimensões numéricas não comprovadas")
    void shouldDetectUnprovenNumericDimensions() {
        Map<String, Object> confirmedAttributes = Map.of(
                "brand", "Decor",
                "category", "Mesa"
        );

        String title = "Mesa Decor 120cm de Madeira";
        String description = "Pesa 45kg e possui 80cm de altura.";

        ClaimValidationResult result = service.validateAndTraceClaims(title, description, confirmedAttributes, List.of());

        assertThat(result.valid()).isFalse();
        assertThat(result.unprovenClaimsDetected())
                .anyMatch(claim -> claim.contains("Dimensão ou peso numérico não comprovado"));
    }
}
