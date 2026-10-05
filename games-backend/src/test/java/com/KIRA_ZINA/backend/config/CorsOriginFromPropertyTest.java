package com.KIRA_ZINA.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Step 6g F - CORS origins are read via relaxed binding, not only System.getenv")
class CorsOriginFromPropertyTest {

    private static MockHttpServletResponse preflight(RateLimitFilter filter, String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/rooms");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    @DisplayName("games.cors.allowed-origins in the Environment allows that origin's preflight (the Render key)")
    void propertyOriginAllowsPreflight() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("games.cors.allowed-origins", "https://example.com");
        RateLimitFilter filter = new RateLimitFilter(environment);

        MockHttpServletResponse response = preflight(filter, "https://example.com");

        assertEquals(200, response.getStatus(),
                "an origin configured as the games.cors.allowed-origins property must pass preflight");
        assertEquals("https://example.com", response.getHeader("Access-Control-Allow-Origin"),
                "the preflight must echo the allowed origin");
    }

    @Test
    @DisplayName("an origin outside the configured list is still refused with 403 (no over-permissive drift)")
    void unknownOriginStillForbidden() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("games.cors.allowed-origins", "https://example.com");
        RateLimitFilter filter = new RateLimitFilter(environment);

        MockHttpServletResponse response = preflight(filter, "https://evil.example.org");

        assertEquals(403, response.getStatus(),
                "relaxed binding must widen the key, never the allowed set");
    }

    @Test
    @DisplayName("localhost defaults still allow preflight when no property and no env var are present")
    void localhostDefaultsSurvive() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new MockEnvironment());

        MockHttpServletResponse response = preflight(filter, "http://localhost:5173");

        assertEquals(200, response.getStatus(),
                "the documented localhost default must not regress");
    }
}
