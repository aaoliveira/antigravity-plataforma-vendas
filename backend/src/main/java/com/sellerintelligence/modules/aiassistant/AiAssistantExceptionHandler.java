package com.sellerintelligence.modules.aiassistant;

import com.sellerintelligence.core.correlation.CorrelationIdFilter;
import com.sellerintelligence.modules.aiassistant.security.PromptInjectionException;
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
 * Tratamento especializado de exceções para o módulo AI Assistant e Gateway.
 */
@RestControllerAdvice(basePackages = "com.sellerintelligence.modules.aiassistant")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiAssistantExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantExceptionHandler.class);

    @ExceptionHandler(PromptInjectionException.class)
    public ProblemDetail handlePromptInjectionException(PromptInjectionException ex, HttpServletRequest request) {
        log.warn("Tentativa de Prompt Injection bloqueada pelo Gateway de IA: tipo='{}', trecho='{}'",
                ex.getThreatType(), ex.getOffendingSnippet());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Tentativa de Prompt Injection Bloqueada");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/prompt-injection-blocked"));
        problem.setProperty("threat_type", ex.getThreatType());
        problem.setProperty("offending_snippet", ex.getOffendingSnippet());
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(SecurityException.class)
    public ProblemDetail handleSecurityException(SecurityException ex, HttpServletRequest request) {
        log.warn("Violação de segurança no AI Gateway: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
        problem.setTitle("Acesso Não Autorizado pelo AI Gateway");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/ai-gateway-access-denied"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex, HttpServletRequest request) {
        log.error("Dependência ou credencial indisponível no AI Gateway: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage()
        );
        problem.setTitle("Provedor de IA Indisponível");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/ai-provider-unavailable"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(com.sellerintelligence.modules.aiassistant.draft.guardrails.ClaimViolationException.class)
    public ProblemDetail handleClaimViolationException(
            com.sellerintelligence.modules.aiassistant.draft.guardrails.ClaimViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Tentativa de inclusão de claims não comprovadas no AI Listing Studio: {}", ex.getViolations());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Claims Não Comprovadas ou Guardrails Violados");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/unproven-claims-detected"));
        problem.setProperty("violations", ex.getViolations());
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Parâmetros inválidos ou limites violados no módulo de IA: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Parâmetros Inválidos ou Limites de Caracteres Violados");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/invalid-parameters-or-limits"));
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
