package com.sellerintelligence.modules.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.reporting.model.ExportDataset;
import com.sellerintelligence.modules.reporting.model.ExportFileFormat;
import com.sellerintelligence.modules.reporting.model.ExportJobResponseDto;
import com.sellerintelligence.modules.reporting.model.ExportRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("P021-BE: Testes de Integração REST do ExportJobController (Jobs, Downloads e Auditoria)")
class ExportJobControllerTest {

    private static final UUID TENANT_ALPHA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TENANT_BETA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ALPHA = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID USER_BETA = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String tokenAlpha;
    private String tokenBeta;

    @BeforeEach
    void setUp() {
        tokenAlpha = jwtTokenProvider.generateToken(
                USER_ALPHA, "operator-alpha@sellerintelligence.com", TENANT_ALPHA, List.of("ROLE_ADMIN")
        );
        tokenBeta = jwtTokenProvider.generateToken(
                USER_BETA, "operator-beta@sellerintelligence.com", TENANT_BETA, List.of("ROLE_ADMIN")
        );
    }

    @Test
    void shouldRequestExportSuccessfullyAndReturnAccepted() throws Exception {
        ExportRequestDto request = new ExportRequestDto(
                ExportDataset.FINANCIAL_FACTS,
                ExportFileFormat.CSV,
                "idemp-test-001",
                500,
                Map.of("marketplace", "MERCADO_LIVRE")
        );

        mockMvc.perform(post("/api/v1/reports/exports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.tenantId", is(TENANT_ALPHA.toString())))
                .andExpect(jsonPath("$.dataset", is("FINANCIAL_FACTS")))
                .andExpect(jsonPath("$.fileFormat", is("CSV")))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.downloadUrl", containsString("/download?token=")))
                .andExpect(jsonPath("$.downloadToken", notNullValue()));
    }

    @Test
    void shouldDownloadGeneratedExportSuccessfully() throws Exception {
        // 1. Cria a exportação
        ExportRequestDto request = new ExportRequestDto(
                ExportDataset.ORDERS,
                ExportFileFormat.CSV,
                null,
                100,
                Map.of()
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/reports/exports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        ExportJobResponseDto job = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                ExportJobResponseDto.class
        );

        // 2. Realiza o download
        mockMvc.perform(get("/api/v1/reports/exports/" + job.id() + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .param("token", job.downloadToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    void shouldBlockCrossTenantDownloadWithForbiddenStatus() throws Exception {
        // 1. Tenant Alpha gera o relatório
        ExportRequestDto request = new ExportRequestDto(
                ExportDataset.CATALOG,
                ExportFileFormat.CSV,
                null,
                100,
                Map.of()
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/reports/exports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        ExportJobResponseDto job = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                ExportJobResponseDto.class
        );

        // 2. Tenant Beta tenta baixar o arquivo do Tenant Alpha
        mockMvc.perform(get("/api/v1/reports/exports/" + job.id() + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBeta)
                        .param("token", job.downloadToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Acesso Não Autorizado ao Relatório")));
    }

    @Test
    void shouldRejectDownloadWithInvalidToken() throws Exception {
        ExportRequestDto request = new ExportRequestDto(
                ExportDataset.METRICS,
                ExportFileFormat.CSV,
                null,
                100,
                Map.of()
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/reports/exports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        ExportJobResponseDto job = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                ExportJobResponseDto.class
        );

        // Download com token incorreto
        mockMvc.perform(get("/api/v1/reports/exports/" + job.id() + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .param("token", "token-completamente-invalido"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Acesso Não Autorizado ao Relatório")));
    }

    @Test
    void shouldListAuditLogsForTenant() throws Exception {
        // Gera um relatório para produzir logs de auditoria
        ExportRequestDto request = new ExportRequestDto(
                ExportDataset.FINANCIAL_FACTS,
                ExportFileFormat.XLSX,
                null,
                100,
                Map.of()
        );

        mockMvc.perform(post("/api/v1/reports/exports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        // Consulta logs de auditoria
        mockMvc.perform(get("/api/v1/reports/exports/audit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[0].action", notNullValue()))
                .andExpect(jsonPath("$.content[0].tenantId", is(TENANT_ALPHA.toString())));
    }
}
