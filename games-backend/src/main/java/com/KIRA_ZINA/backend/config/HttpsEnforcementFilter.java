package com.KIRA_ZINA.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Optional proxy-contract enforcement for deployments behind a TLS-terminating
 * proxy. Disabled by default; set {@code GAMES_REQUIRE_HTTPS=true} to reject
 * any request that did not arrive over https.
 *
 * The scheme is taken from the {@code X-Forwarded-Proto} header when present
 * (added by the trusted proxy), otherwise from the connection itself
 * ({@code isSecure()}/{@code getScheme()}). Dual-check keeps the filter
 * correct regardless of whether {@code ForwardedHeaderFilter} already consumed
 * the header (it runs after this filter, but the check is order-agnostic).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    private final boolean requireHttps;

    public HttpsEnforcementFilter(@Value("${GAMES_REQUIRE_HTTPS:false}") boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (requireHttps && !isHttps(request)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"HTTPS required\",\"status\":400}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isHttps(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto != null && !proto.isBlank()) {
            // Proxy may append hops ("https, http"); the first is the client-facing scheme.
            String first = proto.split(",")[0].trim();
            return "https".equalsIgnoreCase(first);
        }
        return request.isSecure() || "https".equalsIgnoreCase(request.getScheme());
    }
}
