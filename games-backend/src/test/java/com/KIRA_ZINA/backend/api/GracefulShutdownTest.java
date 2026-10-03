package com.KIRA_ZINA.backend.api;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.KIRA_ZINA.backend.GamesBackendApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Item 3 - Graceful shutdown")
class GracefulShutdownTest {

    public static class Probe {
        final AtomicInteger ticks = new AtomicInteger();

        @Scheduled(fixedRate = 50)
        void tick() {
            ticks.incrementAndGet();
        }
    }

    @RestController
    static class SlowEndpoint {
        @GetMapping("/__slow")
        String slow() throws InterruptedException {
            Thread.sleep(1200);
            return "slow-done";
        }
    }

    @Configuration
    static class ShutdownCounterConfig {
        @Bean
        Probe probe() {
            return new Probe();
        }

        @Bean
        SlowEndpoint slowEndpoint() {
            return new SlowEndpoint();
        }
    }

    private static boolean mentionsTaskRejected(ILoggingEvent event) {
        String message = String.valueOf(event.getFormattedMessage());
        if (message.contains("TaskRejectedException")) {
            return true;
        }
        IThrowableProxy proxy = event.getThrowableProxy();
        while (proxy != null) {
            if (proxy.getClassName() != null && proxy.getClassName().contains("TaskRejectedException")) {
                return true;
            }
            proxy = proxy.getCause();
        }
        return false;
    }

    private static Set<Thread> schedulingThreads() {
        Set<Thread> result = new HashSet<>();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.isAlive() && t.getName().startsWith("scheduling-")) {
                result.add(t);
            }
        }
        return result;
    }

    @Test
    @DisplayName("close() lets in-flight request finish, stops scheduler pool, no TaskRejectedException")
    void contextCloseStopsScheduledTasksWithoutTaskRejected() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);

        Set<Thread> preexistingScheduling = schedulingThreads();

        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(
                        GamesBackendApplication.class, ShutdownCounterConfig.class)
                .properties("server.port=0", "spring.main.banner-mode=off")
                .run();
        try {
            Probe probe = ctx.getBean(Probe.class);

            long deadline = System.currentTimeMillis() + 5000;
            while (probe.ticks.get() < 2 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertTrue(probe.ticks.get() >= 2,
                    "scheduled task must be running before close, ticks=" + probe.ticks.get());

            Set<Thread> schedulerPool = schedulingThreads();
            schedulerPool.removeAll(preexistingScheduling);
            assertFalse(schedulerPool.isEmpty(),
                    "app must own at least one live scheduling-* thread before close");

            int port = ((ServletWebServerApplicationContext) ctx).getWebServer().getPort();
            AtomicInteger responseCode = new AtomicInteger(-1);
            AtomicReference<String> responseBody = new AtomicReference<>();
            AtomicReference<Exception> clientError = new AtomicReference<>();

            Thread client = new Thread(() -> {
                try {
                    HttpURLConnection conn = (HttpURLConnection)
                            new URL("http://localhost:" + port + "/__slow").openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(15000);
                    responseCode.set(conn.getResponseCode());
                    responseBody.set(new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    clientError.set(e);
                }
            }, "slow-request-client");
            client.start();

            Thread.sleep(300);
            assertTrue(client.isAlive(), "slow request must be in flight before close() is called");

            ctx.close();
            assertFalse(ctx.isActive(), "context must be inactive after close()");

            client.join(15000);
            assertFalse(client.isAlive(), "in-flight request must complete during graceful shutdown");
            assertNull(clientError.get(),
                    "in-flight request must complete with a valid response, got: " + clientError.get());
            assertEquals(200, responseCode.get(), "in-flight request must return 200 through close()");
            assertEquals("slow-done", responseBody.get(), "in-flight response body must be intact");

            Thread.sleep(100);
            int settled = probe.ticks.get();
            Thread.sleep(400);
            assertEquals(settled, probe.ticks.get(),
                    "scheduled task must not fire after context close");

            long threadDeadline = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < threadDeadline
                    && schedulerPool.stream().anyMatch(Thread::isAlive)) {
                Thread.sleep(50);
            }
            assertTrue(schedulerPool.stream().noneMatch(Thread::isAlive),
                    "every scheduling-* thread owned by this context must terminate after close");

            boolean taskRejected = appender.list.stream()
                    .anyMatch(GracefulShutdownTest::mentionsTaskRejected);
            assertFalse(taskRejected,
                    "no TaskRejectedException may be logged during shutdown");
        } finally {
            if (ctx.isActive()) {
                ctx.close();
            }
            root.detachAppender(appender);
        }
    }
}
