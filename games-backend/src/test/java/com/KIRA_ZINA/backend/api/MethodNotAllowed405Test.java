package com.KIRA_ZINA.backend.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Item 4.1 FIX 3 - wrong HTTP method returns 405")
class MethodNotAllowed405Test {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("PUT on POST-only and DELETE-only endpoints -> 405 with error/status body")
    void wrongMethodReturns405WithJsonBody() throws Exception {
        mockMvc.perform(put("/api/blackjack/sessions"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(put("/api/rooms/some-room/leave"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").exists());
    }
}
