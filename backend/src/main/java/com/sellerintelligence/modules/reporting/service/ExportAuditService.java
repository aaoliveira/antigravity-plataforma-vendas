package com.sellerintelligence.modules.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.modules.reporting.entity.ExportAuditLog;
import com.sellerintelligence.modules.reporting.model.ExportAuditAction;
import com.sellerintelligence.modules.reporting.model.ExportAuditLogDto;
import com.sellerintelligence.modules.reporting.repository.ExportAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ExportAuditService {

    private static final Logger log = LoggerFactory.getLogger(ExportAuditService.class);

    private final ExportAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public ExportAuditService(ExportAuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExportAuditLog recordAudit(
            UUID tenantId,
            UUID exportJobId,
            ExportAuditAction action,
            String userId,
            String ipAddress,
            String userAgent,
            Map<String, Object> details
    ) {
        String detailsJson = "{}";
        if (details != null && !details.isEmpty()) {
            try {
                detailsJson = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException e) {
                log.warn("Falha ao serializar detalhes de auditoria: {}", e.getMessage());
            }
        }

        ExportAuditLog auditLog = new ExportAuditLog(
                UUID.randomUUID(),
                tenantId,
                exportJobId,
                action,
                userId != null ? userId : "ANONYMOUS",
                ipAddress,
                userAgent,
                detailsJson
        );

        ExportAuditLog saved = repository.save(auditLog);
        log.info("Auditoria de exportação registrada: tenant_id={} job_id={} action={} user_id={}",
                tenantId, exportJobId, action, userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ExportAuditLogDto> findAuditLogs(UUID tenantId, Pageable pageable) {
        return repository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::toDto);
    }

    private ExportAuditLogDto toDto(ExportAuditLog log) {
        return new ExportAuditLogDto(
                log.getId(),
                log.getTenantId(),
                log.getExportJobId(),
                log.getAction(),
                log.getUserId(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getDetailsJson(),
                log.getCreatedAt()
        );
    }
}
