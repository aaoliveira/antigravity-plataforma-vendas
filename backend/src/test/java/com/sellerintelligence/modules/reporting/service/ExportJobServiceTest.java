package com.sellerintelligence.modules.reporting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.modules.reporting.entity.ExportAuditLog;
import com.sellerintelligence.modules.reporting.entity.ExportJob;
import com.sellerintelligence.modules.reporting.generator.CsvExportGenerator;
import com.sellerintelligence.modules.reporting.generator.XlsxExportGenerator;
import com.sellerintelligence.modules.reporting.model.*;
import com.sellerintelligence.modules.reporting.provider.ExportDataProvider;
import com.sellerintelligence.modules.reporting.provider.ExportDataProviderRegistry;
import com.sellerintelligence.modules.reporting.repository.ExportAuditLogRepository;
import com.sellerintelligence.modules.reporting.repository.ExportJobRepository;
import com.sellerintelligence.modules.reporting.security.ExportAccessDeniedException;
import com.sellerintelligence.modules.reporting.security.ExportExpiredException;
import com.sellerintelligence.modules.reporting.storage.TenantStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("P021-BE: Testes do ExportJobService (Ciclo de Vida, Idempotência, Segurança e Auditoria)")
class ExportJobServiceTest {

    @TempDir
    Path tempDir;

    private ExportJobRepository jobRepository;
    private ExportAuditLogRepository auditRepository;
    private ExportDataProviderRegistry providerRegistry;
    private ExportDataProvider mockProvider;
    private TenantStorageService storageService;
    private ExportAuditService auditService;
    private ExportJobService jobService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID tenantAlpha = UUID.randomUUID();
    private final UUID tenantBeta = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jobRepository = mock(ExportJobRepository.class);
        auditRepository = mock(ExportAuditLogRepository.class);
        storageService = new TenantStorageService(tempDir.toString());
        auditService = new ExportAuditService(auditRepository, objectMapper);

        mockProvider = mock(ExportDataProvider.class);
        when(mockProvider.supportedDataset()).thenReturn(ExportDataset.FINANCIAL_FACTS);
        providerRegistry = new ExportDataProviderRegistry(List.of(mockProvider));

        jobService = new ExportJobService(
                jobRepository,
                providerRegistry,
                new CsvExportGenerator(),
                new XlsxExportGenerator(),
                storageService,
                auditService,
                objectMapper
        );

        when(jobRepository.save(any(ExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.saveAndFlush(any(ExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any(ExportAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldRequestAndCompleteExportJobSuccessfully() {
        ExportRequestDto request = new ExportRequestDto(
                ExportDataset.FINANCIAL_FACTS,
                ExportFileFormat.CSV,
                "idemp-123",
                100,
                Map.of("marketplace", "MERCADO_LIVRE")
        );

        ExportDataPage mockPage = new ExportDataPage(
                List.of("ID", "Marketplace", "Valor"),
                List.of(List.of("F1", "MERCADO_LIVRE", "150.00")),
                1,
                false
        );
        when(mockProvider.fetchTenantData(eq(tenantAlpha), any(), eq(100))).thenReturn(mockPage);

        ExportJobResponseDto response = jobService.requestExport(
                tenantAlpha,
                request,
                "user-1",
                "127.0.0.1",
                "Mozilla/5.0"
        );

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ExportJobStatus.COMPLETED);
        assertThat(response.totalRows()).isEqualTo(1);
        assertThat(response.downloadUrl()).contains("/api/reports/exports/");
        assertThat(response.downloadToken()).isNotEmpty();

        // Verifica auditoria de REQUESTED e COMPLETED
        ArgumentCaptor<ExportAuditLog> auditCaptor = ArgumentCaptor.forClass(ExportAuditLog.class);
        verify(auditRepository, atLeast(2)).save(auditCaptor.capture());
        List<ExportAuditAction> recordedActions = auditCaptor.getAllValues().stream()
                .map(ExportAuditLog::getAction)
                .toList();
        assertThat(recordedActions).contains(ExportAuditAction.EXPORT_REQUESTED, ExportAuditAction.EXPORT_COMPLETED);
    }

    @Test
    void shouldReuseExistingJobWhenIdempotencyKeyMatches() {
        ExportJob existingJob = new ExportJob(
                UUID.randomUUID(),
                tenantAlpha,
                "user-1",
                "idemp-key-dup",
                ExportDataset.FINANCIAL_FACTS,
                ExportFileFormat.CSV,
                "{}",
                500
        );
        existingJob.markCompleted("export.csv", "tenants/" + tenantAlpha + "/exports/export.csv", 100L, "sha256", "token123", 10, false, Instant.now().plusSeconds(3600));

        when(jobRepository.findByTenantIdAndIdempotencyKey(tenantAlpha, "idemp-key-dup"))
                .thenReturn(Optional.of(existingJob));

        ExportRequestDto request = new ExportRequestDto(
                ExportDataset.FINANCIAL_FACTS,
                ExportFileFormat.CSV,
                "idemp-key-dup",
                500,
                Collections.emptyMap()
        );

        ExportJobResponseDto response = jobService.requestExport(
                tenantAlpha,
                request,
                "user-1",
                "127.0.0.1",
                "Mozilla/5.0"
        );

        // Deve retornar o job existente sem reprocessar
        assertThat(response.id()).isEqualTo(existingJob.getId());
        verify(mockProvider, never()).fetchTenantData(any(), any(), anyInt());
    }

    @Test
    void shouldDownloadExportSuccessfullyWhenTokenAndTenantMatch() throws IOException {
        UUID jobId = UUID.randomUUID();
        String downloadToken = "valid-secret-token";
        byte[] fileBytes = "CSV DATA".getBytes();

        TenantStorageService.StorageResult storageResult = storageService.store(tenantAlpha, "test.csv", fileBytes);

        ExportJob job = new ExportJob(
                jobId,
                tenantAlpha,
                "user-1",
                null,
                ExportDataset.FINANCIAL_FACTS,
                ExportFileFormat.CSV,
                "{}",
                100
        );
        job.markCompleted("test.csv", storageResult.relativePath(), (long) fileBytes.length, storageResult.checksumSha256(), downloadToken, 5, false, Instant.now().plusSeconds(3600));

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ExportJobService.DownloadResult downloadResult = jobService.downloadExport(
                jobId,
                downloadToken,
                tenantAlpha,
                "user-1",
                "127.0.0.1",
                "Mozilla/5.0"
        );

        assertThat(downloadResult).isNotNull();
        assertThat(downloadResult.fileName()).isEqualTo("test.csv");
        assertThat(downloadResult.resource().getContentAsByteArray()).isEqualTo(fileBytes);

        // Verifica auditoria de DOWNLOADED
        ArgumentCaptor<ExportAuditLog> auditCaptor = ArgumentCaptor.forClass(ExportAuditLog.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(ExportAuditAction.EXPORT_DOWNLOADED);
    }

    @Test
    void shouldRejectCrossTenantDownloadAttempt() {
        UUID jobId = UUID.randomUUID();
        String downloadToken = "token-alpha";

        ExportJob job = new ExportJob(
                jobId,
                tenantAlpha,
                "user-alpha",
                null,
                ExportDataset.FINANCIAL_FACTS,
                ExportFileFormat.CSV,
                "{}",
                100
        );
        job.markCompleted("alpha.csv", "path", 100L, "sha", downloadToken, 1, false, Instant.now().plusSeconds(3600));

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        // Tenant Beta tenta baixar arquivo do Tenant Alpha
        assertThatThrownBy(() -> jobService.downloadExport(
                jobId,
                downloadToken,
                tenantBeta,
                "user-beta",
                "10.0.0.1",
                "Mozilla/5.0"
        )).isInstanceOf(ExportAccessDeniedException.class)
          .hasMessageContaining("pertence a outro tenant");

        // Verifica auditoria de CROSS_TENANT_ACCESS_BLOCKED
        ArgumentCaptor<ExportAuditLog> auditCaptor = ArgumentCaptor.forClass(ExportAuditLog.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(ExportAuditAction.EXPORT_CROSS_TENANT_ACCESS_BLOCKED);
    }

    @Test
    void shouldRejectDownloadWithInvalidToken() {
        UUID jobId = UUID.randomUUID();

        ExportJob job = new ExportJob(
                jobId,
                tenantAlpha,
                "user-alpha",
                null,
                ExportDataset.FINANCIAL_FACTS,
                ExportFileFormat.CSV,
                "{}",
                100
        );
        job.markCompleted("alpha.csv", "path", 100L, "sha", "correct-token", 1, false, Instant.now().plusSeconds(3600));

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.downloadExport(
                jobId,
                "wrong-token",
                tenantAlpha,
                "user-alpha",
                "127.0.0.1",
                "Mozilla/5.0"
        )).isInstanceOf(ExportAccessDeniedException.class)
          .hasMessageContaining("Token de download inválido");
    }

    @Test
    void shouldRejectDownloadWhenExpired() {
        UUID jobId = UUID.randomUUID();
        String downloadToken = "token-123";

        ExportJob job = new ExportJob(
                jobId,
                tenantAlpha,
                "user-alpha",
                null,
                ExportDataset.FINANCIAL_FACTS,
                ExportFileFormat.CSV,
                "{}",
                100
        );
        // Expirou há 1 hora
        job.markCompleted("alpha.csv", "path", 100L, "sha", downloadToken, 1, false, Instant.now().minusSeconds(3600));

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.downloadExport(
                jobId,
                downloadToken,
                tenantAlpha,
                "user-alpha",
                "127.0.0.1",
                "Mozilla/5.0"
        )).isInstanceOf(ExportExpiredException.class)
          .hasMessageContaining("expirou");

        // Verifica auditoria de EXPIRED_ACCESS_ATTEMPT
        ArgumentCaptor<ExportAuditLog> auditCaptor = ArgumentCaptor.forClass(ExportAuditLog.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(ExportAuditAction.EXPORT_EXPIRED_ACCESS_ATTEMPT);
    }
}
