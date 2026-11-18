package com.sellerintelligence.modules.marketplaceconnection.exception;

import com.sellerintelligence.core.correlation.CorrelationIdFilter;
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
 * Tratamento de erros especializado para o módulo de Marketplace Connection.
 */
@RestControllerAdvice(basePackages = "com.sellerintelligence.modules.marketplaceconnection")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MarketplaceConnectionExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceConnectionExceptionHandler.class);

    @ExceptionHandler(InvalidStateCsrfException.class)
    public ProblemDetail handleInvalidStateCsrfException(InvalidStateCsrfException ex, HttpServletRequest request) {
        log.warn("Tentativa de callback com state anti-CSRF inválido: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("State Anti-CSRF Inválido");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/invalid-csrf-state"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(UnauthorizedScopeException.class)
    public ProblemDetail handleUnauthorizedScopeException(UnauthorizedScopeException ex, HttpServletRequest request) {
        log.warn("Tentativa de uso de escopo não autorizado: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Escopo Não Autorizado");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/unauthorized-scope"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(MeliOAuthException.class)
    public ProblemDetail handleMeliOAuthException(MeliOAuthException ex, HttpServletRequest request) {
        log.error("Erro na integração OAuth com Mercado Livre: [{}] {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = "invalid_request".equals(ex.getErrorCode()) ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Erro na comunicação com o Mercado Livre: " + ex.getMessage());
        problem.setTitle("Falha de Comunicação OAuth");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/meli-oauth-error"));
        problem.setProperty("oauth_error_code", ex.getErrorCode());
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
