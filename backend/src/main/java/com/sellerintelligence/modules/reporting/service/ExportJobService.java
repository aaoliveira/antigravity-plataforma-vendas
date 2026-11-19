package com.sellerintelligence.modules.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.modules.reporting.entity.ExportJob;
import com.sellerintelligence.modules.reporting.generator.CsvExportGenerator;
import com.sellerintelligence.modules.reporting.generator.XlsxExportGenerator;
import com.sellerintelligence.modules.reporting.model.*;
import com.sellerintelligence.modules.reporting.provider.ExportDataProvider;
import com.sellerintelligence.modules.reporting.provider.ExportDataProviderRegistry;
import com.sellerintelligence.modules.reporting.repository.ExportJobRepository;
import com.sellerintelligence.modules.reporting.security.ExportAccessDeniedException;
import com.sellerintelligence.modules.reporting.security.ExportExpiredException;
import com.sellerintelligence.modules.reporting.security.ExportNotFoundException;
import com.sellerintelligence.modules.reporting.storage.TenantStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class ExportJobService {

    private static final Logger log = LoggerFactory.getLogger(ExportJobService.class);
    private static final Duration DEFAULT_EXPIRATION_DURATION = Duration.ofHours(24);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ExportJobRepository repository;
    private final ExportDataProviderRegistry providerRegistry;
    private final CsvExportGenerator csvGenerator;
    private final XlsxExportGenerator xlsxGenerator;
    private final TenantStorageService storageService;
    private final ExportAuditService auditService;
    private final ObjectMapper objectMapper;

    public ExportJobService(
            ExportJobRepository repository,
            ExportDataProviderRegistry providerRegistry,
            CsvExportGenerator csvGenerator,
            XlsxExportGenerator xlsxGenerator,
            TenantStorageService storageService,
            ExportAuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.providerRegistry = providerRegistry;
        this.csvGenerator = csvGenerator;
        this.xlsxGenerator = xlsxGenerator;
        this.storageService = storageService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Solicita um job de exportação com suporte a idempotência e filtros persistidos.
     */
    @Transactional
    public ExportJobResponseDto requestExport(
            UUID tenantId,
            ExportRequestDto request,
            String userId,
            String clientIp,
            String userAgent
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(request, "request não pode ser nulo");

        String idempotencyKey = request.idempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<ExportJob> existing = repository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey.trim());
            if (existing.isPresent()) {
                ExportJob existingJob = existing.get();
                if (!existingJob.isExpired() && (existingJob.getStatus() == ExportJobStatus.COMPLETED ||
                        existingJob.getStatus() == ExportJobStatus.PENDING ||
                        existingJob.getStatus() == ExportJobStatus.PROCESSING)) {
                    log.info("Job de exportação idempotente reutilizado: tenant_id={} job_id={} key={}",
                            tenantId, existingJob.getId(), idempotencyKey);
                    return toResponseDto(existingJob);
                }
            }
        }

        String filterJson = "{}";
        try {
            filterJson = objectMapper.writeValueAsString(request.resolvedFilterParams());
        } catch (JsonProcessingException e) {
            log.warn("Erro ao serializar filterParamsJson: {}", e.getMessage());
        }

        UUID jobId = UUID.randomUUID();
        ExportJob job = new ExportJob(
                jobId,
                tenantId,
                userId != null ? userId : "ANONYMOUS",
                idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey.trim() : null,
                request.dataset(),
                request.resolvedFormat(),
                filterJson,
                request.resolvedMaxRows()
        );

        ExportJob savedJob = repository.saveAndFlush(job);

        auditService.recordAudit(
                tenantId,
                jobId,
                ExportAuditAction.EXPORT_REQUESTED,
                userId,
                clientIp,
                userAgent,
                Map.of(
                        "dataset", request.dataset().name(),
                        "format", request.resolvedFormat().name(),
                        "maxRows", request.resolvedMaxRows()
                )
        );

        // Processa o job de forma imediata e resiliente
        processJob(savedJob, userId, clientIp, userAgent);

        return toResponseDto(savedJob);
    }

    @Transactional
    public void processJob(ExportJob job, String userId, String clientIp, String userAgent) {
        job.markProcessing();
        repository.save(job);

        try {
            ExportDataProvider provider = providerRegistry.getProvider(job.getDataset());
            Map<String, Object> filterMap = parseFilterParams(job.getFilterParamsJson());

            // 1. Coleta dados autorizados do próprio tenant
            ExportDataPage dataPage = provider.fetchTenantData(job.getTenantId(), filterMap, job.getMaxRows());

            // 2. Gera o arquivo sanitizado
            byte[] fileBytes;
            if (job.getFileFormat() == ExportFileFormat.CSV) {
                fileBytes = csvGenerator.generate(dataPage);
            } else {
                fileBytes = xlsxGenerator.generate(job.getDataset().name(), dataPage);
            }

            // 3. Salva no storage privado do tenant
            String fileName = String.format("export_%s_%s%s",
                    job.getDataset().name().toLowerCase(),
                    job.getId(),
                    job.getFileFormat().getFileExtension());

            TenantStorageService.StorageResult storageResult = storageService.store(
                    job.getTenantId(),
                    fileName,
                    fileBytes
            );

            // 4. Gera token seguro e expiração
            String downloadToken = generateSecureToken();
            Instant expiresAt = Instant.now().plus(DEFAULT_EXPIRATION_DURATION);

            job.markCompleted(
                    fileName,
                    storageResult.relativePath(),
                    storageResult.fileSizeBytes(),
                    storageResult.checksumSha256(),
                    downloadToken,
                    dataPage.rows().size(),
                    dataPage.truncated(),
                    expiresAt
            );

            repository.save(job);

            auditService.recordAudit(
                    job.getTenantId(),
                    job.getId(),
                    ExportAuditAction.EXPORT_COMPLETED,
                    userId,
                    clientIp,
                    userAgent,
                    Map.of(
                            "totalRows", dataPage.rows().size(),
                            "isTruncated", dataPage.truncated(),
                            "fileSizeBytes", storageResult.fileSizeBytes(),
                            "checksumSha256", storageResult.checksumSha256()
                    )
            );
        } catch (Exception e) {
            log.error("Erro ao processar job de exportação: job_id={} erro={}", job.getId(), e.getMessage(), e);
            job.markFailed(e.getMessage());
            repository.save(job);

            auditService.recordAudit(
                    job.getTenantId(),
                    job.getId(),
                    ExportAuditAction.EXPORT_FAILED,
                    userId,
                    clientIp,
                    userAgent,
                    Map.of("errorMessage", e.getMessage() != null ? e.getMessage() : "Erro desconhecido")
            );
        }
    }

    @Transactional(readOnly = true)
    public ExportJobResponseDto getJob(UUID tenantId, UUID jobId) {
        ExportJob job = repository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ExportNotFoundException("Job de exportação não encontrado: " + jobId));
        return toResponseDto(job);
    }

    @Transactional(readOnly = true)
    public Page<ExportJobResponseDto> listJobs(UUID tenantId, Pageable pageable) {
        return repository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::toResponseDto);
    }

    /**
     * Valida permissões, tenant isolation, token e expiração antes de servir o download.
     */
    @Transactional
    public DownloadResult downloadExport(
            UUID jobId,
            String downloadToken,
            UUID currentTenantId,
            String userId,
            String clientIp,
            String userAgent
    ) {
        ExportJob job = repository.findById(jobId)
                .orElseThrow(() -> new ExportNotFoundException("Job de exportação não encontrado: " + jobId));

        // 1. ISOLAMENTO CROSS-TENANT ESTREITO
        if (!job.getTenantId().equals(currentTenantId)) {
            auditService.recordAudit(
                    currentTenantId,
                    jobId,
                    ExportAuditAction.EXPORT_CROSS_TENANT_ACCESS_BLOCKED,
                    userId,
                    clientIp,
                    userAgent,
                    Map.of("attemptedJobId", jobId.toString(), "jobTenantId", job.getTenantId().toString())
            );
            log.warn("TENTATIVA DE DOWNLOAD CROSS-TENANT BLOQUEADA: current_tenant={} job_tenant={} job_id={}",
                    currentTenantId, job.getTenantId(), jobId);
            throw new ExportAccessDeniedException("Acesso negado: o relatório solicitado pertence a outro tenant.");
        }

        // 2. VALIDAÇÃO DO TOKEN DE DOWNLOAD
        if (job.getDownloadToken() == null || !job.getDownloadToken().equals(downloadToken)) {
            throw new ExportAccessDeniedException("Token de download inválido ou expirado.");
        }

        // 3. VALIDAÇÃO DE EXPIRAÇÃO TEMPORAL
        if (job.isExpired()) {
            auditService.recordAudit(
                    currentTenantId,
                    jobId,
                    ExportAuditAction.EXPORT_EXPIRED_ACCESS_ATTEMPT,
                    userId,
                    clientIp,
                    userAgent,
                    Map.of("expiresAt", job.getExpiresAt() != null ? job.getExpiresAt().toString() : "null")
            );
            throw new ExportExpiredException("O link de download deste relatório expirou.");
        }

        // 4. VALIDAÇÃO DE STATUS
        if (job.getStatus() != ExportJobStatus.COMPLETED) {
            throw new IllegalStateException("O job de exportação não está concluído. Status atual: " + job.getStatus());
        }

        // 5. CARREGA RECURSO DO STORAGE PRIVADO DO TENANT
        Resource resource = storageService.loadAsResource(currentTenantId, job.getStoragePath());

        // 6. REGISTRO DE AUDITORIA DE DOWNLOAD COM SUCESSO
        auditService.recordAudit(
                currentTenantId,
                jobId,
                ExportAuditAction.EXPORT_DOWNLOADED,
                userId,
                clientIp,
                userAgent,
                Map.of("fileSizeBytes", job.getFileSizeBytes() != null ? job.getFileSizeBytes() : 0)
        );

        return new DownloadResult(
                resource,
                job.getFileName(),
                job.getFileFormat().getContentType(),
                job.getFileSizeBytes()
        );
    }

    private Map<String, Object> parseFilterParams(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    private ExportJobResponseDto toResponseDto(ExportJob job) {
        String downloadUrl = null;
        if (job.getStatus() == ExportJobStatus.COMPLETED && !job.isExpired() && job.getDownloadToken() != null) {
            downloadUrl = String.format("/api/reports/exports/%s/download?token=%s",
                    job.getId(), job.getDownloadToken());
        }

        return new ExportJobResponseDto(
                job.getId(),
                job.getTenantId(),
                job.getRequestedBy(),
                job.getIdempotencyKey(),
                job.getDataset(),
                job.getFileFormat(),
                job.getFilterParamsJson(),
                job.getStatus(),
                job.getMaxRows(),
                job.getTotalRows(),
                job.isTruncated(),
                job.getFileName(),
                job.getFileSizeBytes(),
                job.getChecksumSha256(),
                downloadUrl,
                job.getDownloadToken(),
                job.getExpiresAt(),
                job.getCompletedAt(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    public record DownloadResult(
            Resource resource,
            String fileName,
            String contentType,
            Long fileSizeBytes
    ) {}
}
