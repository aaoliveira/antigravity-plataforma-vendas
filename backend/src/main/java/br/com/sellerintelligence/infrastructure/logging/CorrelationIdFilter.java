package br.com.sellerintelligence.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID_KEY = "trace_id";
    public static final String MDC_TENANT_ID_KEY = "tenant_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_CORRELATION_ID_KEY, correlationId);

        String tenantId = request.getHeader("X-Tenant-Id");
        if (StringUtils.hasText(tenantId)) {
            MDC.put(MDC_TENANT_ID_KEY, tenantId);
        } else {
            MDC.put(MDC_TENANT_ID_KEY, "anonymous");
        }

        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID_KEY);
            MDC.remove(MDC_TENANT_ID_KEY);
        }
    }
}
