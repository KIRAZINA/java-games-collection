package com.KIRA_ZINA.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Step 5b Task 2 - games.* property consumers")
class RateLimitFilterPropertiesTest {

    private static Object readField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    @DisplayName("games.rate-limit.max-cache-entries and games.rate-limit.bucket-idle-ms are read from the environment")
    void cachePropertiesAreReadFromEnvironment() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("games.rate-limit.max-cache-entries", "17");
        environment.setProperty("games.rate-limit.bucket-idle-ms", "123456");

        RateLimitFilter filter = new RateLimitFilter(environment);

        assertEquals(17, readField(filter, "maxCacheEntries"),
                "games.rate-limit.max-cache-entries must be consumed by RateLimitFilter");
        assertEquals(123456L, readField(filter, "bucketIdleMs"),
                "games.rate-limit.bucket-idle-ms must be consumed by RateLimitFilter");
    }

    @Test
    @DisplayName("games.rate-limit.trust-forwarded-for is read from the environment when no -D system property is set")
    void trustForwardedForSpringPropertyIsRead() throws Exception {
        String key = "games.rate-limit.trust-forwarded-for";
        String prior = System.getProperty(key);
        System.clearProperty(key);
        try {
            MockEnvironment environment = new MockEnvironment();
            environment.setProperty(key, "true");

            RateLimitFilter filter = new RateLimitFilter(environment);

            assertTrue((Boolean) readField(filter, "trustXFF"),
                    "games.rate-limit.trust-forwarded-for must be consumed by RateLimitFilter");
        } finally {
            if (prior != null) {
                System.setProperty(key, prior);
            }
        }
    }

    @Test
    @DisplayName("defaults (10000 entries, 300000 ms, trust=false) apply when properties are absent - no behavior change")
    void defaultsApplyWhenPropertiesAbsent() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new MockEnvironment());

        assertEquals(10000, readField(filter, "maxCacheEntries"));
        assertEquals(300000L, readField(filter, "bucketIdleMs"));
        assertFalse((Boolean) readField(filter, "trustXFF"));
    }
}
