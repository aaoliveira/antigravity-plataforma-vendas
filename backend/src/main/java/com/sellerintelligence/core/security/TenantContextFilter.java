package com.sellerintelligence.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.correlation.CorrelationIdFilter;
import com.sellerintelligence.core.tenant.TenantContext;
import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantMembershipValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Filtro de validação de contexto multi-tenant e contenção de spoofing.
 * Garante que o tenant_id seja derivado estritamente de identidade/membership validada,
 * rejeitando e auditando qualquer tentativa de spoofing via headers ou parâmetros arbitrários.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String X_TENANT_ID_HEADER = "X-Tenant-Id";
    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private final ObjectProvider<TenantMembershipValidator> membershipValidatorProvider;
    private final ObjectProvider<AuditPublisher> auditPublisherProvider;
    private final ObjectMapper objectMapper;

    public TenantContextFilter(
            ObjectProvider<TenantMembershipValidator> membershipValidatorProvider,
            ObjectProvider<AuditPublisher> auditPublisherProvider,
            ObjectMapper objectMapper
    ) {
        this.membershipValidatorProvider = membershipValidatorProvider;
        this.auditPublisherProvider = auditPublisherProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestedTenantHeader = request.getHeader(X_TENANT_ID_HEADER);

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
                if (requestedTenantHeader != null && !requestedTenantHeader.isBlank()) {
                    UUID requestedTenantId;
                    try {
                        requestedTenantId = UUID.fromString(requestedTenantHeader.trim());
                    } catch (IllegalArgumentException e) {
                        rejectRequest(request, response, HttpStatus.BAD_REQUEST, "Formato inválido do identificador de tenant informado no header X-Tenant-Id.");
                        return;
                    }

                    // Verifica se o tenant solicitado difere do tenant ativo no token
                    UUID activeTokenTenantId = user.getActiveTenantId();
                    if (!requestedTenantId.equals(activeTokenTenantId)) {
                        TenantMembershipValidator validator = membershipValidatorProvider.getIfAvailable();
                        boolean hasMembership = validator != null && validator.hasActiveMembership(user.getUserId(), requestedTenantId);

                        if (!hasMembership) {
                            // TENTATIVA DE SPOOFING IDENTIFICADA!
                            log.warn("SPOOFING DETECTADO: Usuário {} tentou acessar tenant {} sem possuir membership.", user.getEmail(), requestedTenantId);

                            AuditPublisher auditPublisher = auditPublisherProvider.getIfAvailable();
                            if (auditPublisher != null) {
                                String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
                                auditPublisher.publish(
                                        "TENANT_SPOOFING_ATTEMPT",
                                        user.getUserId(),
                                        user.getEmail(),
                                        requestedTenantId,
                                        request.getRemoteAddr(),
                                        request.getHeader("User-Agent"),
                                        correlationId,
                                        "Tentativa de spoofing via header X-Tenant-Id: " + requestedTenantId
                                );
                            }

                            rejectRequest(request, response, HttpStatus.FORBIDDEN, "Acesso negado: o usuário não possui membership ativa na organização solicitada.");
                            return;
                        }

                        // Usuário comprovadamente possui membership na organização solicitada
                        TenantContextHolder.set(new TenantContext(requestedTenantId, "MEMBER"));
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void rejectRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String detail
    ) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle("Violação de Isolamento Multi-Tenant");
        problem.setType(URI.create("https://sellerintelligence.com.br/errors/tenant-spoofing-detected"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());

        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        if (correlationId != null) {
            problem.setProperty("correlation_id", correlationId);
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
