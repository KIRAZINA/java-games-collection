package com.KIRA_ZINA.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_CACHE_ENTRIES = 10_000;
    private static final long BUCKET_IDLE_MS = 5 * 60 * 1000L; // 5 minutes

    private final Map<String, BucketEntry> cache = new ConcurrentHashMap<>();
    private final Set<String> allowedOrigins = new HashSet<>();
    private final boolean trustXFF;
    private final int maxCacheEntries;
    private final long bucketIdleMs;

    public RateLimitFilter(Environment environment) {
        this.allowedOrigins.add("http://localhost:5173");
        this.allowedOrigins.add("http://localhost:3000");
        String envOrigins = System.getenv("GAMES_CORS_ALLOWED_ORIGINS");
        if (envOrigins != null && !envOrigins.isEmpty()) {
            for (String origin : envOrigins.split(",")) {
                allowedOrigins.add(origin.trim());
            }
        }
        this.trustXFF = "true".equalsIgnoreCase(
                System.getProperty("games.rate-limit.trust-forwarded-for",
                        environment.getProperty("games.rate-limit.trust-forwarded-for", "false")));
        this.maxCacheEntries = environment.getProperty(
                "games.rate-limit.max-cache-entries", Integer.class, MAX_CACHE_ENTRIES);
        this.bucketIdleMs = environment.getProperty(
                "games.rate-limit.bucket-idle-ms", Long.class, BUCKET_IDLE_MS);
    }

    private static class BucketEntry {
        final Bucket bucket;
        long lastAccessed;
        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessed = System.currentTimeMillis();
        }
    }

    private Bucket createNewBucket() {
        Refill refill = Refill.intervally(30, Duration.ofSeconds(10));
        Bandwidth limit = Bandwidth.classic(200, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        boolean allowedOrigin = false;
        if (origin != null) {
            String normalized = normalizeOrigin(origin);
            allowedOrigin = allowedOrigins.contains(normalized);
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            if (allowedOrigin && origin != null) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
                response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Origin, Accept, X-Requested-With, Idempotency-Key, X-Player-Token, X-Request-Id");
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("Access-Control-Max-Age", "600");
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\":\"CORS origin not allowed\"}");
            }
            return;
        }

        if (allowedOrigin && origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Origin, Accept, X-Requested-With, Idempotency-Key, X-Player-Token, X-Request-Id");
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }

        // Liveness/readiness probes must never be rate limited: bypass before
        // any bucket is created or consumed for this request.
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveIP(request);
        if (cache.size() >= maxCacheEntries) {
            evictOldestIdle();
        }

        BucketEntry entry = cache.compute(ip, (k, v) -> {
            if (v == null) return new BucketEntry(createNewBucket());
            v.lastAccessed = System.currentTimeMillis();
            return v;
        });

        if (entry.bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"Too many requests\"}");
        }
    }

    private String resolveIP(HttpServletRequest request) {
        if (trustXFF) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.trim().isEmpty()) {
                String[] hops = xff.split(",");
                // When TRUE, use only the rightmost hop (added by our trusted proxy)
                String rightmost = hops[hops.length - 1].trim();
                return rightmost.isEmpty() ? request.getRemoteAddr() : rightmost;
            }
        } else {
            // When FALSE, ignore X-Forwarded-For entirely
            return request.getRemoteAddr();
        }
        return request.getRemoteAddr();
    }

    private String normalizeOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            String host = uri.getHost() != null ? uri.getHost() : origin;
            int port = uri.getPort();
            if (port == -1) port = ("https".equals(scheme)) ? 443 : 80;
            return scheme + "://" + host + (port == 443 || port == 80 ? "" : ":" + port);
        } catch (Exception e) {
            return origin.trim();
        }
    }

    private void evictOldestIdle() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> (now - e.getValue().lastAccessed) > bucketIdleMs);
        if (cache.size() >= maxCacheEntries) {
            // If still over capacity, evict oldest by access time
            Optional<Map.Entry<String, BucketEntry>> oldest = cache.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().lastAccessed));
            oldest.ifPresent(e -> cache.remove(e.getKey()));
        }
    }
}
