package com.KIRA_ZINA.backend.minesweeper.service;

import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperSession;
import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperState;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MinesweeperSessionService {
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final Map<String, MinesweeperSession> sessions = new ConcurrentHashMap<>();

    public MinesweeperState createSession(Integer rows, Integer cols, Integer mines) {
        String sessionId = UUID.randomUUID().toString();
        MinesweeperSession session = new MinesweeperSession(
                sessionId,
                rows == null ? 9 : rows,
                cols == null ? 9 : cols,
                mines == null ? 10 : mines
        );
        sessions.put(sessionId, session);
        return session.state();
    }

    public MinesweeperState state(String sessionId) {
        return requireSession(sessionId).state();
    }

    public MinesweeperState open(String sessionId, int row, int col) {
        return requireSession(sessionId).open(row, col);
    }

    public MinesweeperState toggleFlag(String sessionId, int row, int col) {
        return requireSession(sessionId).toggleFlag(row, col);
    }

    public MinesweeperState reset(String sessionId) {
        return requireSession(sessionId).reset();
    }

    public void closeSession(String sessionId) {
        MinesweeperSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
        }
    }

    @Scheduled(fixedDelayString = "${games.minesweeper.cleanup-delay-ms:60000}")
    public void evictInactiveSessions() {
        Instant expiresBefore = Instant.now().minus(SESSION_TTL);
        sessions.entrySet().removeIf(entry -> entry.getValue().lastTouched().isBefore(expiresBefore));
    }

    private MinesweeperSession requireSession(String sessionId) {
        MinesweeperSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Minesweeper session not found: " + sessionId);
        }
        return session;
    }
}
