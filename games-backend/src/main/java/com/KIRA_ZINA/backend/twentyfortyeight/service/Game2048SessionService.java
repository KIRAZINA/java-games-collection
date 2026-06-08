package com.KIRA_ZINA.backend.twentyfortyeight.service;

import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048Session;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048State;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.MoveDirection;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class Game2048SessionService {
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final Map<String, Game2048Session> sessions = new ConcurrentHashMap<>();

    public Game2048State createSession() {
        String sessionId = UUID.randomUUID().toString();
        Game2048Session session = new Game2048Session(sessionId);
        sessions.put(sessionId, session);
        return session.state();
    }

    public Game2048State state(String sessionId) {
        return requireSession(sessionId).state();
    }

    public Game2048State move(String sessionId, MoveDirection direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Move direction is required");
        }
        return requireSession(sessionId).move(direction);
    }

    public Game2048State reset(String sessionId) {
        return requireSession(sessionId).reset();
    }

    public void closeSession(String sessionId) {
        Game2048Session session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
        }
    }

    @Scheduled(fixedDelayString = "${games.2048.cleanup-delay-ms:60000}")
    public void evictInactiveSessions() {
        Instant expiresBefore = Instant.now().minus(SESSION_TTL);
        sessions.entrySet().removeIf(entry -> entry.getValue().lastTouched().isBefore(expiresBefore));
    }

    private Game2048Session requireSession(String sessionId) {
        Game2048Session session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("2048 session not found: " + sessionId);
        }
        return session;
    }
}
