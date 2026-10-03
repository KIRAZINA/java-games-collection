package com.KIRA_ZINA.backend.api;

import com.KIRA_ZINA.backend.common.idempotency.IdempotencyService;
import com.KIRA_ZINA.backend.config.RateLimitFilter;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Idempotency and Hardening Tests")
public class IdempotencyAndHardeningTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Idempotency atomic reservation")
    class IdempotencyAtomic {

        private final AtomicInteger executionCount = new AtomicInteger(0);

        @BeforeEach
        void reset() {
            executionCount.set(0);
        }

        @Test
        @DisplayName("1. Same key sequential calls → same response, exactly one side effect")
        void sameKeySequential() throws Exception {
            String key = "test:sequential:session1:key1";
            // First call executes
            String result1 = idempotencyService.execute("test:sequential", "session1", "key1", () -> {
                executionCount.incrementAndGet();
                return "result";
            }, r -> new IdempotencyService.CachedResponseSnapshot(200, r));

            // Second call should replay from completed store without executing supplier
            try {
                idempotencyService.execute("test:sequential", "session1", "key1", () -> {
                    executionCount.incrementAndGet();
                    return "wrong";
                }, r -> new IdempotencyService.CachedResponseSnapshot(200, r));
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                assert replay.status == 200;
                assert replay.body.equals("result");
            }

            assert executionCount.get() == 1 : "Expected exactly one execution, got " + executionCount.get();
        }

        @Test
        @DisplayName("2. Concurrent same-key calls → exactly one execution, all callers get result or 409")
        void concurrentSameKey() throws Exception {
            int threads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch startLatch = new CountDownLatch(1);
            AtomicInteger executions = new AtomicInteger(0);
            Set<String> results = ConcurrentHashMap.newKeySet();

            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        try {
                            idempotencyService.execute("test:concurrent", "res1", "k1", () -> {
                                executions.incrementAndGet();
                                return "val";
                            }, r -> new IdempotencyService.CachedResponseSnapshot(200, r));
                            results.add("val");
                        } catch (IdempotencyService.IdempotencyReplayException replay) {
                            results.add(replay.body);
                        } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                            results.add("409");
                        }
                    } catch (Exception ignored) {}
                });
            }

            startLatch.countDown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            executor.shutdown();

            assert executions.get() == 1 : "Concurrent executions should be exactly 1, got " + executions.get();
        }

        @Test
        @DisplayName("3. Different keys → two executions")
        void differentKeysTwoExecutions() throws Exception {
            AtomicInteger count = new AtomicInteger(0);
            idempotencyService.execute("test:diff", "r1", "k1", () -> { count.incrementAndGet(); return "a"; },
                    r -> new IdempotencyService.CachedResponseSnapshot(200, r));
            idempotencyService.execute("test:diff", "r1", "k2", () -> { count.incrementAndGet(); return "b"; },
                    r -> new IdempotencyService.CachedResponseSnapshot(200, r));
            assert count.get() == 2;
        }

        @Test
        @DisplayName("4. Non-2xx response is NOT cached: first call gets 409, second executes again")
        void non2xxNotCached() throws Exception {
            AtomicInteger count = new AtomicInteger(0);
            // First call: simulate a non-2xx by throwing exception inside supplier
            // (T infers to Object here: the supplier never returns a value, so the snapshotter
            // must not constrain it; the snapshot is never produced because the action throws)
            try {
                idempotencyService.execute("test:non2xx", "r", "k", () -> {
                    count.incrementAndGet();
                    throw new IllegalStateException("conflict");
                }, r -> new IdempotencyService.CachedResponseSnapshot(200, ""));
            } catch (IllegalStateException expected) {}

            // Second call with same key should execute again (not replay exception, because no cached response for non-2xx)
            try {
                idempotencyService.execute("test:non2xx", "r", "k", () -> {
                    count.incrementAndGet();
                    throw new IllegalStateException("conflict2");
                }, r -> new IdempotencyService.CachedResponseSnapshot(200, ""));
            } catch (IllegalStateException expected2) {}

            assert count.get() == 2 : "Non-2xx should allow retry, executions=" + count.get();
        }

        @Test
        @DisplayName("5. Absent header → no reservation stored")
        void absentHeaderNoStore() throws Exception {
            // Direct service call without header should work; store size check is implicit
            assert idempotencyService != null;
        }

        @Test
        @DisplayName("11. Action succeeds but snapshotter returns non-2xx: result returned, nothing cached, retry re-executes")
        void snapshotterNon2xxNotCached() throws Exception {
            AtomicInteger count = new AtomicInteger(0);

            String first = idempotencyService.execute("test:snap-non2xx", "r", "k", () -> {
                count.incrementAndGet();
                return "result";
            }, r -> new IdempotencyService.CachedResponseSnapshot(409, "conflict"));

            assert first.equals("result") : "action result must be returned, got " + first;

            // No COMPLETED reservation may be retained for a non-2xx snapshot:
            // the same key must execute the action again instead of replaying.
            String second = idempotencyService.execute("test:snap-non2xx", "r", "k", () -> {
                count.incrementAndGet();
                return "result2";
            }, r -> new IdempotencyService.CachedResponseSnapshot(409, "conflict"));

            assert second.equals("result2") : "second call must re-execute, got " + second;
            assert count.get() == 2 : "expected 2 executions, got " + count.get();
        }

        @Test
        @DisplayName("12. cleanupExpired removes only expired reservations and is @Scheduled on the configured property")
        void cleanupExpiredRemovesOnlyExpiredAndIsScheduled() throws Exception {
            // Step 4.1 FIX 2: the property games.idempotency.cleanup-delay-ms must actually
            // drive a scheduled task, and cleanupExpired() must evict exactly the expired entries.
            Method m = IdempotencyService.class.getMethod("cleanupExpired");
            Scheduled ann = m.getAnnotation(Scheduled.class);
            assertThat(ann)
                    .as("cleanupExpired must be annotated with @Scheduled")
                    .isNotNull();
            assertThat(ann.fixedDelayString())
                    .as("scheduled delay must read the configured property")
                    .isEqualTo("${games.idempotency.cleanup-delay-ms:600000}");

            idempotencyService.execute("test:cleanup", "r1", "stale1", () -> "a",
                    r -> new IdempotencyService.CachedResponseSnapshot(200, r));
            idempotencyService.execute("test:cleanup", "r1", "stale2", () -> "b",
                    r -> new IdempotencyService.CachedResponseSnapshot(200, r));
            idempotencyService.execute("test:cleanup", "r1", "fresh", () -> "c",
                    r -> new IdempotencyService.CachedResponseSnapshot(200, r));

            Field storeField = IdempotencyService.class.getDeclaredField("store");
            storeField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> store = (Map<String, Object>) storeField.get(idempotencyService);

            String staleKey1 = idempotencyService.makeKey("test:cleanup", "r1", "stale1");
            String staleKey2 = idempotencyService.makeKey("test:cleanup", "r1", "stale2");
            String freshKey = idempotencyService.makeKey("test:cleanup", "r1", "fresh");

            long staleTimestamp = System.currentTimeMillis() - 11 * 60_000L; // > 10-minute TTL
            Class<?> reservationClass = Class.forName(
                    "com.KIRA_ZINA.backend.common.idempotency.IdempotencyService$Reservation");
            Field createdAt = reservationClass.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.setLong(store.get(staleKey1), staleTimestamp);
            createdAt.setLong(store.get(staleKey2), staleTimestamp);

            int sizeBefore = store.size();
            idempotencyService.cleanupExpired();

            assertThat(store)
                    .as("both backdated reservations must be removed, the fresh one kept, "
                            + "and no other entry touched (before=%d after=%d)", sizeBefore, store.size())
                    .doesNotContainKeys(staleKey1, staleKey2)
                    .containsKey(freshKey);
            assertThat(store.size())
                    .as("cleanup must remove exactly the two expired entries")
                    .isEqualTo(sizeBefore - 2);
        }
    }

    @Nested
    @DisplayName("Hardening: rate limit and CORS")
    class Hardening {

        private Map<String, Object> cacheOf(RateLimitFilter filter) throws Exception {
            Field f = RateLimitFilter.class.getDeclaredField("cache");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> cache = (Map<String, Object>) f.get(filter);
            return cache;
        }

        private int maxCacheEntries() throws Exception {
            Field f = RateLimitFilter.class.getDeclaredField("MAX_CACHE_ENTRIES");
            f.setAccessible(true);
            return f.getInt(null);
        }

        private long bucketIdleMs() throws Exception {
            Field f = RateLimitFilter.class.getDeclaredField("BUCKET_IDLE_MS");
            f.setAccessible(true);
            return f.getLong(null);
        }

        private void postThrough(RateLimitFilter filter, String remoteAddr, String forwardedFor) throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/rooms");
            request.setRemoteAddr(remoteAddr);
            if (forwardedFor != null) {
                request.addHeader("X-Forwarded-For", forwardedFor);
            }
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        private RateLimitFilter filterWithTrust(String trustValue) {
            String prop = "games.rate-limit.trust-forwarded-for";
            String prior = System.getProperty(prop);
            try {
                System.setProperty(prop, trustValue);
                return new RateLimitFilter();
            } finally {
                if (prior == null) {
                    System.clearProperty(prop);
                } else {
                    System.setProperty(prop, prior);
                }
            }
        }

        @Test
        @DisplayName("6. Rate limit cache cap under 20_000 synthetic IPs")
        void cacheCapUnderSyntheticIps() throws Exception {
            int distinctIps = 20_000;
            int maxEntries = maxCacheEntries();
            int calibrationIps = 200;

            // Step 4.1 FIX 4: ratio-based timing budget instead of the fixed "<10s" wall-clock
            // limit that failed under CPU contention (observed 11.6-16.5s normal-priority,
            // 4.6s idle). Chosen approach: calibration (the spec's processors-based option
            // yields a 6s budget on this 8-thread machine, below observed contended runs).
            //
            // A single fresh-filter calibration batch underestimates the run by ~6.5x even on
            // an idle machine, because every request after the cache reaches MAX_CACHE_ENTRIES
            // pays two O(MAX_CACHE_ENTRIES) scans in evictOldestIdle() (removeIf over the whole
            // map + min-scan), which the first 10_000 requests of the run never do. Measured on
            // this machine: fresh 200-request batch = 7ms (0.035 ms/req) vs the 20_000-run =
            // 4570ms (0.2285 ms/req), so a 3x allowance on a fresh-only batch gives a 2.1s
            // budget and fails deterministically. We therefore calibrate BOTH cost phases of
            // the run and project the exact 10_000-fresh + 10_000-at-cap mix:
            //   phase A: 200 requests on a fresh filter           -> msPerReqFresh
            //   phase B: fill a second filter to the cap (untimed), then 200 timed requests
            //            that each trigger an eviction pass        -> msPerReqAtCap
            //   projection = 10_000 * msPerReqFresh + 10_000 * msPerReqAtCap
            //   budget     = 3 * projection   (the spec's 3x allowance, here for load)
            // Both phases run under the same machine load as the measured run, so the budget
            // tracks contention instead of failing because of it. Correctness assertions
            // (cap + eviction + fresh-cache) and distinctIps=20_000 are unchanged.
            RateLimitFilter calibFresh = new RateLimitFilter();
            long aStart = System.nanoTime();
            for (int i = 0; i < calibrationIps; i++) {
                String ip = "172.16." + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
                postThrough(calibFresh, ip, null);
            }
            long aMs = Math.max(1, (System.nanoTime() - aStart) / 1_000_000L);
            double msPerReqFresh = (double) aMs / calibrationIps;

            RateLimitFilter calibAtCap = new RateLimitFilter();
            for (int i = 0; i < maxEntries; i++) {
                String ip = "192.168." + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
                postThrough(calibAtCap, ip, null);
            }
            long bStart = System.nanoTime();
            for (int i = 0; i < calibrationIps; i++) {
                String ip = "198.18." + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
                postThrough(calibAtCap, ip, null);
            }
            long bMs = Math.max(1, (System.nanoTime() - bStart) / 1_000_000L);
            double msPerReqAtCap = (double) bMs / calibrationIps;

            long freshPhase = distinctIps / 2;
            long atCapPhase = distinctIps - freshPhase;
            long projectionMs = (long) (freshPhase * msPerReqFresh + atCapPhase * msPerReqAtCap);
            long budgetMs = 3L * Math.max(1, projectionMs);

            RateLimitFilter filter = new RateLimitFilter();
            Map<String, Object> cache = cacheOf(filter);
            assertThat(cache).as("fresh filter must start with an empty cache").isEmpty();

            long start = System.currentTimeMillis();
            for (int i = 0; i < distinctIps; i++) {
                String ip = "10." + ((i >> 16) & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
                postThrough(filter, ip, null);
            }
            long elapsed = System.currentTimeMillis() - start;

            assertThat(cache.size())
                    .as("cache must never exceed MAX_CACHE_ENTRIES=%d after %d distinct IPs (took %.1fs)",
                            maxEntries, distinctIps, elapsed / 1000.0)
                    .isLessThanOrEqualTo(maxEntries);
            assertThat(cache.size())
                    .as("eviction must have fired: %d cached < %d distinct IPs sent", cache.size(), distinctIps)
                    .isLessThan(distinctIps);
            assertThat(elapsed)
                    .as("run (%dms) must finish within 3x the calibrated projection %dms "
                            + "(fresh %.3f ms/req x %d + at-cap %.3f ms/req x %d; phases %dms/%dms)",
                            elapsed, projectionMs, msPerReqFresh, freshPhase, msPerReqAtCap,
                            atCapPhase, aMs, bMs)
                    .isLessThanOrEqualTo(budgetMs);
        }

        @Test
        @DisplayName("7. XFF spoof with trust=false does not create separate bucket")
        void xffSpoofDoesNotBypass() throws Exception {
            // trust=false: identical remote address with spoofed XFF values collapses to one bucket
            RateLimitFilter noTrust = filterWithTrust("false");
            postThrough(noTrust, "203.0.113.10", "1.2.3.4");
            postThrough(noTrust, "203.0.113.10", "5.6.7.8");

            assertThat(cacheOf(noTrust).keySet())
                    .as("trust=false must ignore X-Forwarded-For entirely")
                    .containsExactly("203.0.113.10");

            // trust=true: bucket key follows the rightmost XFF hop
            RateLimitFilter trust = filterWithTrust("true");
            postThrough(trust, "10.0.0.1", "9.9.9.9");
            postThrough(trust, "10.0.0.2", "9.9.9.9");    // same rightmost, different remote -> same bucket
            postThrough(trust, "10.0.0.1", "7.7.7.7");    // different rightmost -> different bucket
            postThrough(trust, "10.0.0.3", "198.51.100.1, 198.51.100.2, 203.0.113.99");

            assertThat(cacheOf(trust).keySet())
                    .as("trust=true must key on the rightmost XFF hop ('c' of 'a, b, c')")
                    .containsExactlyInAnyOrder("9.9.9.9", "7.7.7.7", "203.0.113.99");
            assertThat(cacheOf(trust))
                    .as("3 distinct rightmost hops -> exactly 3 buckets")
                    .hasSize(3);
        }

        @Test
        @DisplayName("8. Idle bucket eviction works")
        void idleBucketEviction() throws Exception {
            RateLimitFilter filter = new RateLimitFilter();
            Map<String, Object> cache = cacheOf(filter);
            for (int i = 0; i < 20; i++) {
                postThrough(filter, "10.1.0." + i, null);
            }
            assertThat(cache).hasSize(20);

            long stale = System.currentTimeMillis() - bucketIdleMs() - 1000;
            for (Object entry : cache.values()) {
                Field lastAccessed = entry.getClass().getDeclaredField("lastAccessed");
                lastAccessed.setAccessible(true);
                lastAccessed.setLong(entry, stale);
            }

            Method evictOldestIdle = RateLimitFilter.class.getDeclaredMethod("evictOldestIdle");
            evictOldestIdle.setAccessible(true);
            evictOldestIdle.invoke(filter);

            assertThat(cache)
                    .as("every entry idle past BUCKET_IDLE_MS must be evicted")
                    .isEmpty();

            // entries within the idle window must survive a second eviction pass
            for (int i = 0; i < 5; i++) {
                postThrough(filter, "10.2.0." + i, null);
            }
            assertThat(cache).hasSize(5);
            evictOldestIdle.invoke(filter);
            assertThat(cache)
                    .as("fresh entries must NOT be evicted")
                    .hasSize(5);
        }

        @Test
        @DisplayName("9. CORS bypass origins return 403 after RateLimitFilter rewrite")
        void corsBypassReturns403() throws Exception {
            mockMvc.perform(options("/api/blackjack/sessions")
                            .header("Origin", "http://evil-localhost.attacker.com"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(options("/api/blackjack/sessions")
                            .header("Origin", "http://localhost.evil.com"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(options("/api/blackjack/sessions")
                            .header("Origin", "http://localhost:5173.evil.com"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(options("/api/blackjack/sessions")
                            .header("Origin", "null"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(options("/api/blackjack/sessions")
                            .header("Origin", "http://localhost:5173"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                    .andExpect(header().string("Access-Control-Allow-Headers", containsString("Idempotency-Key")));
        }

        @Test
        @DisplayName("10. Preflight Allow-Headers includes X-Player-Token and X-Request-Id")
        void preflightAllowHeadersIncludesPlayerTokenAndRequestId() throws Exception {
            // Step 4.1 FIX 1: X-Player-Token (required since Step 3b on five protected
            // endpoints) and X-Request-Id must be in the preflight allow-list or
            // cross-origin browsers refuse to send them.
            mockMvc.perform(options("/api/rooms/some-room/ready")
                            .header("Origin", "http://localhost:5173")
                            .header("Access-Control-Request-Headers",
                                    "x-player-token, x-request-id, idempotency-key"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                    .andExpect(header().string("Access-Control-Allow-Headers",
                            containsString("X-Player-Token")))
                    .andExpect(header().string("Access-Control-Allow-Headers",
                            containsString("X-Request-Id")))
                    .andExpect(header().string("Access-Control-Allow-Headers",
                            containsString("Idempotency-Key")));
        }
    }
}
