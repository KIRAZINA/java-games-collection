package com.KIRA_ZINA.backend.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Item 4 - HTTPS enforcement off by default")
@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = true)
class HttpsEnforcementDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Without GAMES_REQUIRE_HTTPS, plain http succeeds")
    void allowsHttpWithoutFlag() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Without GAMES_REQUIRE_HTTPS, forwarded schemes are ignored")
    void allowsAnyForwardedSchemeWithoutFlag() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header("X-Forwarded-Proto", "http"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms")
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(status().isOk());
    }
}
