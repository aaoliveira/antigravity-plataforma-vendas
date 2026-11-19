package com.sellerintelligence.modules.reporting;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.reporting.model.ExportAuditLogDto;
import com.sellerintelligence.modules.reporting.model.ExportJobResponseDto;
import com.sellerintelligence.modules.reporting.model.ExportRequestDto;
import com.sellerintelligence.modules.reporting.service.ExportAuditService;
import com.sellerintelligence.modules.reporting.service.ExportJobService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST para solicitação assíncrona, acompanhamento e download seguro de relatórios (P021-BE).
 * Aplica isolamento de tenant em profundidade, sanitização de fórmulas e URLs temporárias expiradas.
 */
@RestController
@RequestMapping("/api/v1/reports/exports")
public class ExportJobController {

    private final ExportJobService exportJobService;
    private final ExportAuditService auditService;

    public ExportJobController(ExportJobService exportJobService, ExportAuditService auditService) {
        this.exportJobService = exportJobService;
        this.auditService = auditService;
    }

    /**
     * Solicita a geração assíncrona de exportação de dados do tenant.
     */
    @PostMapping
    public ResponseEntity<ExportJobResponseDto> requestExport(
            @Valid @RequestBody ExportRequestDto request,
            HttpServletRequest httpRequest
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String userId = resolveUserId();
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);

        ExportJobResponseDto response = exportJobService.requestExport(
                tenantId,
                request,
                userId,
                clientIp,
                userAgent
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Lista o histórico de exportações do próprio tenant.
     */
    @GetMapping
    public ResponseEntity<Page<ExportJobResponseDto>> listExports(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Page<ExportJobResponseDto> exports = exportJobService.listJobs(tenantId, pageable);
        return ResponseEntity.ok(exports);
    }

    /**
     * Consulta status e detalhes de um job de exportação específico.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExportJobResponseDto> getExport(
            @PathVariable UUID id
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        ExportJobResponseDto export = exportJobService.getJob(tenantId, id);
        return ResponseEntity.ok(export);
    }

    /**
     * Realiza o download seguro do arquivo de exportação gerado.
     * Valida tenant isolation, token temporário e prazo de validade.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadExport(
            @PathVariable UUID id,
            @RequestParam("token") String token,
            HttpServletRequest httpRequest
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String userId = resolveUserId();
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);

        ExportJobService.DownloadResult result = exportJobService.downloadExport(
                id,
                token,
                tenantId,
                userId,
                clientIp,
                userAgent
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header("X-Content-Type-Options", "nosniff")
                .body(result.resource());
    }

    /**
     * Consulta a trilha de auditoria de exportações do tenant.
     */
    @GetMapping("/audit")
    public ResponseEntity<Page<ExportAuditLogDto>> listAuditLogs(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Page<ExportAuditLogDto> auditLogs = auditService.findAuditLogs(tenantId, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "OPERATOR";
    }
}
