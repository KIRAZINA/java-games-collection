package com.KIRA_ZINA.backend.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Regression Tests for Fixes 1-5")
public class FixRegressionTests {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Fix 1 — 404 mapping")
    class Fix1_404 {
        @Test
        @DisplayName("Missing room returns 404")
        void missingRoom404() throws Exception {
            mockMvc.perform(get("/api/rooms/nonexistent-room-id"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("Fix 3 — CORS exact match")
    class Fix3_CORS {
        @Test
        @DisplayName("Allowed origin succeeds preflight")
        void allowedOriginPreflight() throws Exception {
            mockMvc.perform(options("/api/blackjack/sessions")
                            .header("Origin", "http://localhost:5173"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Disallowed origin with substring bypass returns 403")
        void disallowedSubstringOriginReturns403() throws Exception {
            mockMvc.perform(options("/api/blackjack/sessions")
                            .header("Origin", "http://localhost.evil.com"))
                    .andExpect(status().isForbidden());
        }
    }
}
