package com.KIRA_ZINA.backend.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.KIRA_ZINA.backend.config.CorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Item 1 - CorrelationIdFilter")
@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = true)
class CorrelationIdFilterTest {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    static final AtomicReference<String> CAPTURED_MDC = new AtomicReference<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @TestConfiguration
    static class MdcCaptureConfig implements WebMvcConfigurer {
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new HandlerInterceptor() {
                @Override
                public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                         jakarta.servlet.http.HttpServletResponse response,
                                         Object handler) {
                    CAPTURED_MDC.set(MDC.get(CorrelationIdFilter.MDC_KEY));
                    return true;
                }
            });
        }
    }

    @BeforeEach
    void resetCapturedMdc() {
        CAPTURED_MDC.set(null);
    }

    @Test
    @DisplayName("Echoes X-Request-Id and exposes it through MDC during controller execution")
    void echoesIncomingRequestIdAndSetsMdcDuringControllerExecution() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header(CorrelationIdFilter.REQUEST_ID_HEADER, "test-abc"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.REQUEST_ID_HEADER, "test-abc"));

        assertEquals("test-abc", CAPTURED_MDC.get(),
                "MDC requestId must be visible while the controller runs");
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY),
                "MDC must be cleared after the request completes");
    }

    @Test
    @DisplayName("Generates a UUID when no X-Request-Id header is sent")
    void generatesUuidWhenHeaderAbsent() throws Exception {
        String requestId = mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(CorrelationIdFilter.REQUEST_ID_HEADER);

        assertNotNull(requestId, "response must carry a generated X-Request-Id");
        assertTrue(UUID_PATTERN.matcher(requestId).matches(),
                "expected a UUID but got: " + requestId);
        assertEquals(requestId, CAPTURED_MDC.get(),
                "generated id must be the one exposed through MDC");
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    @DisplayName("X-Request-Id is present even on 429 responses (filter runs before RateLimitFilter)")
    void requestIdPresentOnRateLimited429() throws Exception {
        int status = -1;
        String bodyOn429 = null;
        for (int i = 0; i < 300; i++) {
            var result = mockMvc.perform(post("/api/blackjack/sessions")
                            .header(CorrelationIdFilter.REQUEST_ID_HEADER, "limited-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"initialBalance\":100}"))
                    .andReturn();
            status = result.getResponse().getStatus();
            if (status == 429) {
                bodyOn429 = result.getResponse().getContentAsString();
                break;
            }
        }
        assertEquals(429, status, "rate-limit bucket must exhaust within 300 POSTs");
        assertTrue(bodyOn429.contains("Too many requests"), bodyOn429);

        var rejected = mockMvc.perform(post("/api/blackjack/sessions")
                        .header(CorrelationIdFilter.REQUEST_ID_HEADER, "limited-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\":100}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(CorrelationIdFilter.REQUEST_ID_HEADER, "limited-1"))
                .andReturn();
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY),
                "MDC must be cleared even when the chain is rejected");
        assertEquals("limited-1", rejected.getResponse().getHeader(CorrelationIdFilter.REQUEST_ID_HEADER));
    }

    @Test
    @DisplayName("logging.pattern.level carries the MDC requestId")
    void loggingPatternIncludesRequestIdMdc() {
        String pattern = environment.getProperty("logging.pattern.level");
        assertNotNull(pattern, "logging.pattern.level must be configured");
        assertTrue(pattern.contains("%X{requestId}"), "unexpected pattern: " + pattern);
    }

    @Test
    @DisplayName("Boundary logging: DEBUG entry/exit with MDC, nothing else")
    void logsRequestEntryAndExitAtDebug() throws Exception {
        ch.qos.logback.classic.Logger filterLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        Level original = filterLogger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        filterLogger.addAppender(appender);
        filterLogger.setLevel(Level.DEBUG);
        try {
            mockMvc.perform(get("/api/rooms")
                            .header(CorrelationIdFilter.REQUEST_ID_HEADER, "dbg-1"))
                    .andExpect(status().isOk());

            assertFalse(appender.list.isEmpty(), "expected DEBUG boundary logs");
            boolean entry = appender.list.stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("GET /api/rooms"));
            boolean exit = appender.list.stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("status=200"));
            assertTrue(entry, "missing request entry log; events=" + appender.list.size());
            assertTrue(exit, "missing request exit log; events=" + appender.list.size());

            for (ILoggingEvent event : appender.list) {
                assertEquals("dbg-1", event.getMDCPropertyMap().get(CorrelationIdFilter.MDC_KEY),
                        "boundary logs must carry the requestId MDC");
            }
        } finally {
            filterLogger.setLevel(original);
            filterLogger.detachAppender(appender);
        }
    }
}
