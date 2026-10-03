package com.KIRA_ZINA.backend.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Item 4 - HTTPS enforcement (GAMES_REQUIRE_HTTPS=true)")
@SpringBootTest(properties = {"logging.level.root=WARN", "GAMES_REQUIRE_HTTPS=true"})
@AutoConfigureMockMvc(addFilters = true)
class HttpsEnforcementEnabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Request without X-Forwarded-Proto returns 400")
    void rejectsPlainHttpWhenRequired() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"HTTPS required\",\"status\":400}"));
    }

    @Test
    @DisplayName("Request with X-Forwarded-Proto: https passes")
    void allowsForwardedHttpsWhenRequired() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Request with X-Forwarded-Proto: http returns 400")
    void rejectsForwardedHttpWhenRequired() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header("X-Forwarded-Proto", "http"))
                .andExpect(status().isBadRequest());
    }
}
