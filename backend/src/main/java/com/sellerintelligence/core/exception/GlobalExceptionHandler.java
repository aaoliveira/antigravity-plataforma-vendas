package com.sellerintelligence.core.exception;

import com.sellerintelligence.core.correlation.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Tratamento global padronizado de exceções em conformidade com o padrão RFC 7807 (Problem Details).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso Não Encontrado");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/not-found"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(org.springframework.security.authentication.BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Falha de autenticação (credenciais inválidas): {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Credenciais Inválidas");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/bad-credentials"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(com.sellerintelligence.core.tenant.TenantContextMissingException.class)
    public ProblemDetail handleTenantContextMissingException(com.sellerintelligence.core.tenant.TenantContextMissingException ex, HttpServletRequest request) {
        log.warn("Tentativa de acesso a recurso tenant-scoped sem tenant resolvido: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Contexto de Tenant Ausente");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/tenant-context-missing"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(com.sellerintelligence.core.tenant.TenantAccessDeniedException.class)
    public ProblemDetail handleTenantAccessDeniedException(com.sellerintelligence.core.tenant.TenantAccessDeniedException ex, HttpServletRequest request) {
        log.warn("Tentativa de acesso não autorizada a tenant: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Acesso Negado ao Tenant");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/tenant-access-denied"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(com.sellerintelligence.core.security.BruteForceBlockedException.class)
    public ProblemDetail handleBruteForceBlockedException(com.sellerintelligence.core.security.BruteForceBlockedException ex, HttpServletRequest request) {
        log.warn("Tentativa de autenticação bloqueada por brute force: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setTitle("Limite de Tentativas Excedido");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/brute-force-lockout"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(com.sellerintelligence.core.security.TokenExpiredException.class)
    public ProblemDetail handleTokenExpiredException(com.sellerintelligence.core.security.TokenExpiredException ex, HttpServletRequest request) {
        log.warn("Token JWT expirado: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Token Expirado");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/token-expired"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(com.sellerintelligence.core.security.InvalidTokenException.class)
    public ProblemDetail handleInvalidTokenException(com.sellerintelligence.core.security.InvalidTokenException ex, HttpServletRequest request) {
        log.warn("Token JWT inválido: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Token Inválido");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/invalid-token"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRuleException(BusinessRuleException ex, HttpServletRequest request) {
        log.warn("Violação de regra de negócio: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Violação de Regra de Negócio");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/business-rule-violation"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResourceException(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Recurso duplicado ou conflito de unicidade: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Recurso Duplicado");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/duplicate-resource"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Erro de validação de argumentos de entrada: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Um ou mais campos contêm valores inválidos.");
        problem.setTitle("Erro de Validação de Entrada");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/validation-error"));

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        problem.setProperty("invalid_fields", fieldErrors);
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Parâmetro Inválido");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/invalid-parameter"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Erro interno inesperado no servidor: ", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado. A equipe técnica foi notificada com o identificador de correlação."
        );
        problem.setTitle("Erro Interno do Servidor");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/internal-server-error"));
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
