package com.KIRA_ZINA.backend.api;

import com.KIRA_ZINA.backend.config.RateLimitFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Item 2 - Actuator hardening")
@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = true)
class ActuatorHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @AfterEach
    void clearSharedRateLimitBuckets() throws Exception {
        cacheOf(rateLimitFilter).clear();
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> cacheOf(RateLimitFilter filter) throws Exception {
        Field f = RateLimitFilter.class.getDeclaredField("cache");
        f.setAccessible(true);
        return (Map<Object, Object>) f.get(filter);
    }

    @Test
    @DisplayName("GET /actuator/health returns 200 with status UP and no details")
    void healthIsUpWithoutDetails() throws Exception {
        var result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(!body.contains("details") && !body.contains("components"),
                "show-details=never must hide component details: " + body);
    }

    @Test
    @DisplayName("env/beans/configprops/heapdump/threaddump/loggers/mappings/metrics are not exposed")
    void sensitiveEndpointsNotExposed() throws Exception {
        List<String> hidden = List.of(
                "/actuator/env", "/actuator/beans", "/actuator/configprops",
                "/actuator/heapdump", "/actuator/threaddump", "/actuator/loggers",
                "/actuator/mappings", "/actuator/metrics");
        for (String path : hidden) {
            mockMvc.perform(get(path)).andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("Health stays UP after 1000 same-IP POSTs and bypasses the rate-limit bucket")
    void healthSurvivesRateLimitExhaustionAndBypassesBucket() throws Exception {
        int created = 0;
        int limited = 0;
        for (int i = 0; i < 1000; i++) {
            int s = mockMvc.perform(post("/api/blackjack/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"initialBalance\":100}"))
                    .andReturn().getResponse().getStatus();
            if (s == 201) {
                created++;
            } else if (s == 429) {
                limited++;
            } else {
                throw new AssertionError("unexpected status " + s + " at POST #" + i);
            }
        }
        assertTrue(created > 0, "some POSTs must succeed before the bucket empties");
        assertTrue(limited > 0, "the 1000 POSTs must exhaust the rate-limit bucket");

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Explicit bypass proof: on a clean cache, POSTing the health path must
        // reach the dispatcher (non-429) WITHOUT creating a rate-limit bucket.
        cacheOf(rateLimitFilter).clear();
        int healthPostStatus = mockMvc.perform(post("/actuator/health"))
                .andReturn().getResponse().getStatus();
        assertNotEquals(429, healthPostStatus,
                "health path must never be rate limited");
        assertTrue(cacheOf(rateLimitFilter).isEmpty(),
                "health request must not create a rate-limit bucket, but found keys: "
                        + cacheOf(rateLimitFilter).keySet());
    }
}
