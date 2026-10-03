package com.KIRA_ZINA.backend.common.idempotency;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IdempotencyService {

    private static final long TTL_MS = 10 * 60 * 1000L;
    private static final int MAX_ENTRIES = 50_000;
    private static final long PENDING_TIMEOUT_MS = 5000L;

    private final ConcurrentHashMap<String, Reservation> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> pendingLocks = new ConcurrentHashMap<>();

    private static class Reservation {
        final String state; // "PENDING" or "COMPLETED"
        final int status;
        final String body;
        final long createdAt;
        final CountDownLatch latch;

        Reservation(String state) {
            this.state = state;
            this.status = -1;
            this.body = null;
            this.createdAt = System.currentTimeMillis();
            this.latch = new CountDownLatch(1);
        }

        Reservation(String state, int status, String body) {
            this.state = state;
            this.status = status;
            this.body = body;
            this.createdAt = System.currentTimeMillis();
            this.latch = new CountDownLatch(1);
            this.latch.countDown();
        }
    }

    public String makeKey(String scope, String resourceId, String idempotencyKey) {
        return scope + ":" + (resourceId != null ? resourceId : "global") + ":" + idempotencyKey;
    }

    public <T> T execute(String scope, String resourceId, String idempotencyKey,
                         SupplierWithException<T> action,
                         Function<T, CachedResponseSnapshot> snapshotter) throws Exception {
        String fullKey = makeKey(scope, resourceId, idempotencyKey);
        Reservation res = store.get(fullKey);

        if (res != null && res.state.equals("COMPLETED")) {
            if ((System.currentTimeMillis() - res.createdAt) > TTL_MS) {
                store.remove(fullKey);
                return executeScope(action, snapshotter, scope, resourceId, idempotencyKey, fullKey);
            }
            // Replay completed 2xx response
            throw new IdempotencyReplayException(res.status, res.body);
        }

        if (res != null && res.state.equals("PENDING")) {
            // Wait briefly for the other thread to complete
            boolean completed = res.latch.await(PENDING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            Reservation current = store.get(fullKey);
            if (current != null && current.state.equals("COMPLETED")) {
                if ((System.currentTimeMillis() - current.createdAt) > TTL_MS) {
                    store.remove(fullKey);
                    return executeScope(action, snapshotter, scope, resourceId, idempotencyKey, fullKey);
                }
                throw new IdempotencyReplayException(current.status, current.body);
            }
            // Still pending (or reservation was released after a failed attempt) after timeout
            throw new IdempotencyInProgressException();
        }

        return executeScope(action, snapshotter, scope, resourceId, idempotencyKey, fullKey);
    }

    private <T> T executeScope(SupplierWithException<T> action,
                               Function<T, CachedResponseSnapshot> snapshotter,
                               String scope, String resourceId, String idempotencyKey, String fullKey) throws Exception {
        Reservation pending = new Reservation("PENDING");
        Reservation existing = store.putIfAbsent(fullKey, pending);
        if (existing != null) {
            // Another thread won the race; treat as concurrent
            if (existing.state.equals("PENDING")) {
                boolean done = existing.latch.await(PENDING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                Reservation after = store.get(fullKey);
                if (after != null && after.state.equals("COMPLETED")) {
                    if ((System.currentTimeMillis() - after.createdAt) > TTL_MS) {
                        store.remove(fullKey);
                        return executeScope(action, snapshotter, scope, resourceId, idempotencyKey, fullKey);
                    }
                    throw new IdempotencyReplayException(after.status, after.body);
                }
                throw new IdempotencyInProgressException();
            }
            if (existing.state.equals("COMPLETED")) {
                if ((System.currentTimeMillis() - existing.createdAt) > TTL_MS) {
                    store.remove(fullKey);
                    return executeScope(action, snapshotter, scope, resourceId, idempotencyKey, fullKey);
                }
                throw new IdempotencyReplayException(existing.status, existing.body);
            }
        }

        try {
            T result = action.get();
            // execute() owns the full lifecycle: serialize the successful result, persist the
            // COMPLETED reservation, release waiters, then return. Controllers no longer need
            // a separate storeCompleted() call.
            CachedResponseSnapshot snapshot = snapshotter != null ? snapshotter.apply(result) : null;
            if (snapshot != null && snapshot.status >= 200 && snapshot.status < 300) {
                store.put(fullKey, new Reservation("COMPLETED", snapshot.status, snapshot.body));
            } else {
                // Non-2xx (or missing) snapshots are not cached; release the reservation so
                // the next request with this key executes fresh.
                store.remove(fullKey);
            }
            pending.latch.countDown();
            return result;
        } catch (Exception e) {
            // On failure, release the reservation so retry can proceed
            store.remove(fullKey);
            pending.latch.countDown();
            throw e;
        }
    }

    public ResponseEntity<Object> replayResponse(String scope, String resourceId, String idempotencyKey) throws IdempotencyReplayException {
        String fullKey = makeKey(scope, resourceId, idempotencyKey);
        Reservation res = store.get(fullKey);
        if (res == null || !res.state.equals("COMPLETED")) {
            throw new IdempotencyReplayException(-1, null);
        }
        if ((System.currentTimeMillis() - res.createdAt) > TTL_MS) {
            store.remove(fullKey);
            throw new IdempotencyReplayException(-1, null);
        }
        throw new IdempotencyReplayException(res.status, res.body);
    }

    public CachedResponseSnapshot replayCachedResponse(String scope, String resourceId, String idempotencyKey) {
        String fullKey = makeKey(scope, resourceId, idempotencyKey);
        Reservation res = store.get(fullKey);
        if (res == null || !res.state.equals("COMPLETED")) return null;
        if ((System.currentTimeMillis() - res.createdAt) > TTL_MS) {
            store.remove(fullKey);
            return null;
        }
        return new CachedResponseSnapshot(res.status, res.body);
    }

    @Scheduled(fixedDelayString = "${games.idempotency.cleanup-delay-ms:600000}")
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> (now - e.getValue().createdAt) > TTL_MS);
        while (store.size() > MAX_ENTRIES) {
            List<String> oldestKeys = store.entrySet().stream()
                    .sorted((a, b) -> Long.compare(a.getValue().createdAt, b.getValue().createdAt))
                    .limit(Math.max(1, MAX_ENTRIES / 10))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            oldestKeys.forEach(store::remove);
        }
    }

    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Exception;
    }

    public static class CachedResponseSnapshot {
        public final int status;
        public final String body;
        public CachedResponseSnapshot(int status, String body) {
            this.status = status; this.body = body;
        }
    }

    public static class IdempotencyReplayException extends RuntimeException {
        public final int status;
        public final String body;
        public IdempotencyReplayException(int status, String body) {
            super("Idempotency replay: status=" + status);
            this.status = status;
            this.body = body;
        }
    }

    public static class IdempotencyInProgressException extends IllegalStateException {
        public IdempotencyInProgressException() {
            super("Duplicate request in progress");
        }
    }
}
