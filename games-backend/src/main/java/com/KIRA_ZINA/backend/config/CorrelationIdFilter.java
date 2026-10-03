package com.KIRA_ZINA.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a correlation id to every request: echoes an incoming X-Request-Id
 * header or generates a fresh UUID, exposes it to all log lines through the
 * MDC key {@code requestId}, and echoes it back on the response.
 *
 * Runs before {@link RateLimitFilter} ({@link Ordered#HIGHEST_PRECEDENCE}) so
 * the header is present even on 429 responses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        } else {
            requestId = requestId.trim();
        }

        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            if (log.isDebugEnabled()) {
                log.debug("Request {} {}", request.getMethod(), request.getRequestURI());
            }
            filterChain.doFilter(request, response);
            if (log.isDebugEnabled()) {
                log.debug("Completed {} {} status={}", request.getMethod(), request.getRequestURI(), response.getStatus());
            }
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
