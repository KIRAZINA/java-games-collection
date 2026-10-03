package com.KIRA_ZINA.backend.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Fixed Production Issues — Enabled Regression Tests")
public class FailingBugTests {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Fixed: 404 for missing resources")
    class Fixed404 {
        @Test
        @DisplayName("GET missing blackjack session returns 404 with JSON error body")
        void missingBlackjackSessionReturns404() throws Exception {
            mockMvc.perform(get("/api/blackjack/sessions/nonexistent-id"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
