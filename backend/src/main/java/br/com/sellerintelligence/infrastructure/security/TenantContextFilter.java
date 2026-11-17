package br.com.sellerintelligence.infrastructure.security;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserService;
import br.com.sellerintelligence.infrastructure.logging.CorrelationIdFilter;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.OrganizationService;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private static final Pattern ORG_PATH_PATTERN = Pattern.compile("^/api/v1/organizations/([a-fA-F0-9\\-]{36})(/.*)?$");

    private final UserService userService;
    private final OrganizationService organizationService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public TenantContextFilter(UserService userService,
                               OrganizationService organizationService,
                               AuditService auditService,
                               ObjectMapper objectMapper) {
        this.userService = userService;
        this.organizationService = organizationService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth && jwtAuth.isAuthenticated()) {
            Jwt jwt = jwtAuth.getToken();
            User currentUser = userService.resolveOrCreateUserFromJwt(jwt);

            UUID requestedTenantId = resolveRequestedTenantId(request);

            if (requestedTenantId != null) {
                Optional<Membership> membership = organizationService.findActiveMembership(requestedTenantId, jwt.getSubject());

                if (membership.isEmpty()) {
                    log.warn("Access denied: User sub {} (ID {}) attempted access to unauthorized tenant {}",
                            jwt.getSubject(), currentUser.getId(), requestedTenantId);

                    auditService.recordEvent(
                            requestedTenantId,
                            currentUser.getId().toString(),
                            "ACCESS_DENIED",
                            "Organization",
                            requestedTenantId.toString(),
                            "Cross-tenant access attempted by user without active membership",
                            AuditResult.DENIED
                    );

                    sendForbiddenResponse(request, response, "Access denied to the requested organization or resource");
                    return;
                }

                TenantContext.setTenant(new TenantContext.TenantInfo(
                        requestedTenantId,
                        currentUser.getId(),
                        jwt.getSubject(),
                        currentUser.getEmail(),
                        membership.get().getRole()
                ));
            } else {
                TenantContext.setTenant(new TenantContext.TenantInfo(
                        null,
                        currentUser.getId(),
                        jwt.getSubject(),
                        currentUser.getEmail(),
                        null
                ));
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID resolveRequestedTenantId(HttpServletRequest request) {
        String headerTenantId = request.getHeader("X-Tenant-Id");
        if (StringUtils.hasText(headerTenantId)) {
            try {
                return UUID.fromString(headerTenantId.trim());
            } catch (IllegalArgumentException e) {
                log.debug("Invalid UUID format in X-Tenant-Id header: {}", headerTenantId);
            }
        }

        String uri = request.getRequestURI();
        Matcher matcher = ORG_PATH_PATTERN.matcher(uri);
        if (matcher.matches()) {
            try {
                return UUID.fromString(matcher.group(1));
            } catch (IllegalArgumentException e) {
                log.debug("Invalid UUID format in path: {}", matcher.group(1));
            }
        }

        return null;
    }

    private void sendForbiddenResponse(HttpServletRequest request, HttpServletResponse response, String detail)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
        problemDetail.setTitle("Forbidden");
        problemDetail.setType(URI.create("https://sellerintelligence.com.br/errors/access-denied"));
        problemDetail.setProperty("code", "ACCESS_DENIED");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));

        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }
}
