package com.KIRA_ZINA.backend.blackjack.service;

import com.KIRA_ZINA.backend.blackjack.domain.BlackjackSession;
import com.KIRA_ZINA.backend.blackjack.domain.BlackjackState;
import com.KIRA_ZINA.backend.blackjack.domain.DealerDifficulty;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BlackjackSessionService {
    private static final double DEFAULT_INITIAL_BALANCE = 100.0;
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final Map<String, BlackjackSession> sessions = new ConcurrentHashMap<>();

    public BlackjackState createSession(Double initialBalance, DealerDifficulty difficulty) {
        String sessionId = UUID.randomUUID().toString();
        BlackjackSession session = new BlackjackSession(
                sessionId,
                initialBalance == null ? DEFAULT_INITIAL_BALANCE : initialBalance,
                difficulty == null ? DealerDifficulty.BASIC : difficulty
        );
        sessions.put(sessionId, session);
        return session.state();
    }

    public BlackjackState startRound(String sessionId) {
        return requireSession(sessionId).startRound();
    }

    public BlackjackState placeBet(String sessionId, double amount) {
        return requireSession(sessionId).placeBet(amount);
    }

    public BlackjackState hit(String sessionId) {
        return requireSession(sessionId).hit();
    }

    public BlackjackState stand(String sessionId) {
        return requireSession(sessionId).stand();
    }

    public BlackjackState state(String sessionId) {
        return requireSession(sessionId).state();
    }

    public void closeSession(String sessionId) {
        BlackjackSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
        }
    }

    @Scheduled(fixedDelayString = "${games.blackjack.cleanup-delay-ms:60000}")
    public void evictInactiveSessions() {
        Instant expiresBefore = Instant.now().minus(SESSION_TTL);
        sessions.entrySet().removeIf(entry -> entry.getValue().lastTouched().isBefore(expiresBefore));
    }

    private BlackjackSession requireSession(String sessionId) {
        BlackjackSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Blackjack session not found: " + sessionId);
        }
        return session;
    }
}
