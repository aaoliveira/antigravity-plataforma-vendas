package com.sellerintelligence.core.security;

import com.sellerintelligence.core.tenant.TenantContext;
import com.sellerintelligence.core.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticação que intercepta requisições HTTP com header Authorization Bearer,
 * valida a assinatura e expiração do JWT e configura o SecurityContext e o TenantContext inicial.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            try {
                JwtTokenProvider.Claims claims = jwtTokenProvider.validateAndParseToken(token);
                AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                        claims.userId(),
                        claims.email(),
                        claims.tenantId(),
                        claims.roles()
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        authenticatedUser.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (claims.tenantId() != null) {
                    String role = claims.roles().isEmpty() ? "MEMBER" : claims.roles().get(0);
                    TenantContextHolder.set(new TenantContext(claims.tenantId(), role));
                }
            } catch (Exception e) {
                // Registra o erro de validação para o AuthenticationEntryPoint capturar se a rota for protegida
                request.setAttribute("jwt_exception", e);
                SecurityContextHolder.clearContext();
                TenantContextHolder.clear();
            }
        }

        filterChain.doFilter(request, response);
    }
}
