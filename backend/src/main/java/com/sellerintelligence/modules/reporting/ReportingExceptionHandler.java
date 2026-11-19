package com.sellerintelligence.modules.reporting;

import com.sellerintelligence.core.correlation.CorrelationIdFilter;
import com.sellerintelligence.modules.reporting.security.ExportAccessDeniedException;
import com.sellerintelligence.modules.reporting.security.ExportExpiredException;
import com.sellerintelligence.modules.reporting.security.ExportNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Tratamento especializado de erros RFC 7807 para o módulo de relatórios e exportações (P021-BE).
 */
@RestControllerAdvice(basePackages = "com.sellerintelligence.modules.reporting")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReportingExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ReportingExceptionHandler.class);

    @ExceptionHandler(ExportExpiredException.class)
    public ProblemDetail handleExportExpiredException(ExportExpiredException ex, HttpServletRequest request) {
        log.warn("Tentativa de download de exportação expirada: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.GONE,
                ex.getMessage()
        );
        problem.setTitle("Link de Download Expirado");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/export-expired"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(ExportAccessDeniedException.class)
    public ProblemDetail handleExportAccessDeniedException(ExportAccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acesso negado a arquivo ou job de exportação: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
        problem.setTitle("Acesso Não Autorizado ao Relatório");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/export-access-denied"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(ExportNotFoundException.class)
    public ProblemDetail handleExportNotFoundException(ExportNotFoundException ex, HttpServletRequest request) {
        log.warn("Job de exportação não localizado: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Relatório de Exportação Não Encontrado");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/export-not-found"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(SecurityException.class)
    public ProblemDetail handleSecurityException(SecurityException ex, HttpServletRequest request) {
        log.error("Violação grave de segurança no módulo de exportação: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
        problem.setTitle("Violação de Segurança no Storage de Exportação");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/export-security-violation"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Parâmetros inválidos na solicitação de exportação: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Parâmetros Inválidos na Exportação");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/invalid-export-parameters"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    private void enrichProblemDetail(ProblemDetail problem, HttpServletRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        if (correlationId != null) {
            problem.setProperty("correlation_id", correlationId);
        }
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setInstance(URI.create(request.getRequestURI()));
    }
}
