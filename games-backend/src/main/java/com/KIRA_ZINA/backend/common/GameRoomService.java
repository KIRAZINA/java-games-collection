package com.KIRA_ZINA.backend.common;

import com.KIRA_ZINA.backend.blackjack.domain.BlackjackState;
import com.KIRA_ZINA.backend.blackjack.domain.DealerDifficulty;
import com.KIRA_ZINA.backend.blackjack.service.BlackjackSessionService;
import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperState;
import com.KIRA_ZINA.backend.minesweeper.service.MinesweeperSessionService;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048Session;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048State;
import com.KIRA_ZINA.backend.twentyfortyeight.service.Game2048SessionService;

import static com.KIRA_ZINA.backend.common.RoomProgressResponse.PlayerProgress;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GameRoomService {
    private static final Duration ROOM_TTL = Duration.ofHours(2);
    private static final Duration EMPTY_ROOM_TTL = Duration.ofMinutes(5);
    private static final long READY_CHECK_DURATION_MS = 3000;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> playerToRoom = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> roomPlayerSessions = new ConcurrentHashMap<>();

    private final BlackjackSessionService blackjackSessionService;
    private final MinesweeperSessionService minesweeperSessionService;
    private final Game2048SessionService game2048SessionService;

    public GameRoomService(BlackjackSessionService blackjackSessionService,
                           MinesweeperSessionService minesweeperSessionService,
                           Game2048SessionService game2048SessionService) {
        this.blackjackSessionService = blackjackSessionService;
        this.minesweeperSessionService = minesweeperSessionService;
        this.game2048SessionService = game2048SessionService;
    }

    public GameRoom.RoomSummary createRoom(String roomName, GameSettings settings, String ownerId, String ownerName) {
        if (playerToRoom.containsKey(ownerId)) {
            leaveRoom(ownerId);
        }
        String roomId = UUID.randomUUID().toString();
        String passwordHash = null;
        if (settings.passwordProtected() && settings.passwordHash() != null && !settings.passwordHash().isEmpty()) {
            passwordHash = passwordEncoder.encode(settings.passwordHash());
        }
        int maxPlayers = settings.isSinglePlayer() ? 1 : settings.maxPlayers();
        GameSettings securedSettings = new GameSettings(
                settings.gameType(),
                settings.settings(),
                settings.passwordProtected(),
                passwordHash,
                settings.allowBots(),
                maxPlayers,
                settings.isSinglePlayer() ? 0 : settings.timeLimitSeconds(),
                settings.isSinglePlayer()
        );
        GameRoom room = new GameRoom(roomId, roomName, securedSettings, ownerId);
        room.addPlayer(ownerId, ownerName, false);
        rooms.put(roomId, room);
        playerToRoom.put(ownerId, roomId);
        roomPlayerSessions.put(roomId, new ConcurrentHashMap<>());

        if (securedSettings.isSinglePlayer()) {
            room.markReady(ownerId);
            room.startGame();
            String sessionId = initSessionForPlayer(room, ownerId, securedSettings);
            if (sessionId != null) {
                roomPlayerSessions.get(roomId).put(ownerId, sessionId);
            }
        }

        return GameRoom.RoomSummary.from(room);
    }

    private String initSessionForPlayer(GameRoom room, String playerId, GameSettings settings) {
        try {
            return switch (settings.gameType()) {
                case BLACKJACK -> {
                    double balance = ((Number) settings.settings().getOrDefault("initialBalance", 100.0)).doubleValue();
                    String difficultyStr = (String) settings.settings().getOrDefault("difficulty", "BASIC");
                    DealerDifficulty difficulty = DealerDifficulty.valueOf(difficultyStr);
                    BlackjackState state = blackjackSessionService.createSession(balance, difficulty);
                    yield state.sessionId();
                }
                case MINESWEEPER -> {
                    int rows = ((Number) settings.settings().getOrDefault("rows", 9)).intValue();
                    int cols = ((Number) settings.settings().getOrDefault("cols", 9)).intValue();
                    int mines = ((Number) settings.settings().getOrDefault("mines", 10)).intValue();
                    MinesweeperState state = minesweeperSessionService.createSession(rows, cols, mines);
                    yield state.sessionId();
                }
                case TWENTY_FORTY_EIGHT -> {
                    Game2048State state = game2048SessionService.createSession();
                    yield state.sessionId();
                }
            };
        } catch (Exception e) {
            return null;
        }
    }

    public List<GameRoom.RoomSummary> listRooms(GameType gameType) {
        return rooms.values().stream()
                .filter(room -> room.getSettings().gameType() == gameType)
                .map(GameRoom.RoomSummary::from)
                .sorted(Comparator.comparing(GameRoom.RoomSummary::lastActivity).reversed())
                .collect(Collectors.toList());
    }

    public List<GameRoom.RoomSummary> listAllRooms() {
        return rooms.values().stream()
                .map(GameRoom.RoomSummary::from)
                .sorted(Comparator.comparing(GameRoom.RoomSummary::lastActivity).reversed())
                .collect(Collectors.toList());
    }

    public Optional<GameRoom> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public GameRoom.RoomSummary joinRoom(String roomId, String playerId, String playerName, String password) {
        if (playerToRoom.containsKey(playerId)) {
            leaveRoom(playerId);
        }
        GameRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("Room not found: " + roomId);

        synchronized (room) {
            if (!rooms.containsKey(roomId)) throw new IllegalArgumentException("Room not found: " + roomId);

            if (room.getPhase() != GameRoom.RoomPhase.LOBBY && room.getPhase() != GameRoom.RoomPhase.READY_CHECK) {
                throw new IllegalStateException("Room is not accepting players (phase: " + room.getPhase() + ")");
            }
            if (room.isFull()) throw new IllegalStateException("Room is full");

            if (room.getSettings().passwordProtected()) {
                String storedHash = room.getSettings().passwordHash();
                if (storedHash == null || password == null || !passwordEncoder.matches(password, storedHash)) {
                    throw new SecurityException("Invalid password");
                }
            }

            boolean added = room.addPlayer(playerId, playerName, false);
            if (!added) throw new IllegalStateException("Failed to join room");

            playerToRoom.put(playerId, roomId);
            return GameRoom.RoomSummary.from(room);
        }
    }

    public GameRoom.RoomSummary joinAsSpectator(String roomId, String spectatorId, String spectatorName) {
        GameRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("Room not found: " + roomId);
        room.addSpectator(spectatorId, spectatorName);
        return GameRoom.RoomSummary.from(room);
    }

    public boolean leaveRoom(String playerId) {
        String roomId = playerToRoom.remove(playerId);
        if (roomId == null) return false;

        GameRoom room = rooms.get(roomId);
        if (room == null) return false;

        synchronized (room) {
            room.removePlayer(playerId);
            Map<String, String> sessions = roomPlayerSessions.get(roomId);
            if (sessions != null) sessions.remove(playerId);

            if (room.getPlayers().isEmpty()) {
                rooms.remove(roomId);
                roomPlayerSessions.remove(roomId);
                return true;
            }
            return false;
        }
    }

    public boolean leaveAsSpectator(String roomId, String spectatorId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) return false;
        room.removeSpectator(spectatorId);
        return true;
    }

    public void registerPlayerSession(String roomId, String playerId, String sessionId) {
        rooms.computeIfPresent(roomId, (id, room) -> {
            if (!room.hasPlayer(playerId)) throw new IllegalArgumentException("Player not in room");
            Map<String, String> sessions = roomPlayerSessions.get(roomId);
            if (sessions != null) sessions.put(playerId, sessionId);
            return room;
        });
        if (!rooms.containsKey(roomId)) throw new IllegalArgumentException("Room not found: " + roomId);
    }

    public synchronized void markPlayerReady(String roomId, String playerId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("Room not found: " + roomId);
        if (room.getPhase() != GameRoom.RoomPhase.LOBBY) {
            throw new IllegalStateException("Room is not in LOBBY phase");
        }
        if (!room.hasPlayer(playerId)) throw new IllegalArgumentException("Player not in room");

        room.markReady(playerId);
        if (room.allPlayersReady()) {
            if (room.getSettings().gameType() == GameType.BLACKJACK) {
                room.startGame();
            } else {
                room.startReadyCheck();
            }
        }
    }

    public RoomStateResponse getRoomState(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("Room not found: " + roomId);

        tickRoomPhase(room);

        Map<String, String> sessions = roomPlayerSessions.getOrDefault(roomId, Collections.emptyMap());
        List<RoomStateResponse.PlayerState> playerStates = new ArrayList<>();

        for (Map.Entry<String, String> entry : sessions.entrySet()) {
            String pid = entry.getKey();
            String sid = entry.getValue();
            GameRoom.Player player = room.getPlayer(pid);
            String playerName = player != null ? player.name() : pid;
            Map<String, Object> metrics = extractMetrics(sid, room.getSettings().gameType());
            playerStates.add(new RoomStateResponse.PlayerState(pid, playerName, metrics));
        }

        long timeRemaining = calculateTimeRemaining(room);

        return new RoomStateResponse(
                roomId,
                room.getSettings().gameType().name(),
                room.getPhase().name(),
                room.getPlayerCount(),
                playerStates,
                room.getPhase().name(),
                timeRemaining,
                room.getGameStartTime(),
                room.allPlayersReady(),
                room.getReadyPlayers().size(),
                room.getPlayerCount()
        );
    }

    public RoomProgressResponse getRoomProgress(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("Room not found: " + roomId);

        tickRoomPhase(room);

        Map<String, String> sessions = roomPlayerSessions.getOrDefault(roomId, Collections.emptyMap());
        List<PlayerProgress> playerProgresses = new ArrayList<>();

        for (Map.Entry<String, String> entry : sessions.entrySet()) {
            String pid = entry.getKey();
            String sid = entry.getValue();
            GameRoom.Player player = room.getPlayer(pid);
            String playerName = player != null ? player.name() : pid;
            playerProgresses.add(extractPlayerProgress(sid, room.getSettings().gameType(), pid, playerName));
        }

        long timeRemaining = calculateTimeRemaining(room);

        return new RoomProgressResponse(
                roomId,
                room.getSettings().gameType().name(),
                playerProgresses,
                room.getPhase().name(),
                timeRemaining
        );
    }

    private void tickRoomPhase(GameRoom room) {
        if (room.getSettings().gameType() == GameType.BLACKJACK) return;

        long now = System.currentTimeMillis();

        if (room.getPhase() == GameRoom.RoomPhase.READY_CHECK) {
            if (now - room.getReadyCheckStartTime() >= READY_CHECK_DURATION_MS) {
                room.startGame();
                injectIceBlocksOnStart(room);
            }
        }

        if (room.getPhase() == GameRoom.RoomPhase.PLAYING) {
            int timeLimit = room.getSettings().timeLimitSeconds();
            if (timeLimit > 0) {
                long elapsed = (now - room.getGameStartTime()) / 1000;
                if (elapsed >= timeLimit) {
                    settleGame(room);
                }
            }
        }
    }

    private void injectIceBlocksOnStart(GameRoom room) {
        if (room.getSettings().gameType() != GameType.TWENTY_FORTY_EIGHT) return;
        Map<String, String> sessions = roomPlayerSessions.getOrDefault(room.getRoomId(), Collections.emptyMap());
        for (Map.Entry<String, String> entry : sessions.entrySet()) {
            try {
                Game2048State state = game2048SessionService.state(entry.getValue());
            } catch (Exception ignored) {}
        }
    }

    private long calculateTimeRemaining(GameRoom room) {
        if (room.getSettings().gameType() == GameType.BLACKJACK) {
            return -1;
        }
        if (room.getPhase() == GameRoom.RoomPhase.READY_CHECK) {
            long elapsed = System.currentTimeMillis() - room.getReadyCheckStartTime();
            return Math.max(0, (READY_CHECK_DURATION_MS - elapsed) / 1000);
        }
        if (room.getPhase() == GameRoom.RoomPhase.PLAYING) {
            int timeLimit = room.getSettings().timeLimitSeconds();
            if (timeLimit <= 0) return -1;
            long elapsed = (System.currentTimeMillis() - room.getGameStartTime()) / 1000;
            return Math.max(0, timeLimit - elapsed);
        }
        return -1;
    }

    private void settleGame(GameRoom room) {
        Map<String, String> sessions = roomPlayerSessions.getOrDefault(room.getRoomId(), Collections.emptyMap());

        String bestPlayerId = null;
        int bestScore = -1;

        for (Map.Entry<String, String> entry : sessions.entrySet()) {
            String pid = entry.getKey();
            String sid = entry.getValue();
            try {
                int score = extractScore(sid, room.getSettings().gameType());
                if (score > bestScore) {
                    bestScore = score;
                    bestPlayerId = pid;
                }
            } catch (Exception ignored) {}
        }

        if (bestPlayerId == null && !sessions.isEmpty()) {
            bestPlayerId = sessions.keySet().iterator().next();
        }

        room.finishGame(bestPlayerId, bestScore);
    }

    private int extractScore(String sessionId, GameType gameType) {
        return switch (gameType) {
            case MINESWEEPER -> {
                MinesweeperState state = minesweeperSessionService.state(sessionId);
                yield state.score();
            }
            case TWENTY_FORTY_EIGHT -> {
                Game2048State state = game2048SessionService.state(sessionId);
                yield state.score();
            }
            case BLACKJACK -> {
                BlackjackState state = blackjackSessionService.state(sessionId);
                yield (int) state.balance();
            }
        };
    }

    private Map<String, Object> extractMetrics(String sessionId, GameType gameType) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        switch (gameType) {
            case BLACKJACK -> {
                try {
                    BlackjackState state = blackjackSessionService.state(sessionId);
                    metrics.put("balance", state.balance());
                    metrics.put("phase", state.phase().name());
                    metrics.put("currentBet", state.currentBet());
                    metrics.put("canContinue", state.canContinue());
                } catch (Exception e) {
                    metrics.put("error", "Session not found");
                }
            }
            case MINESWEEPER -> {
                try {
                    MinesweeperState state = minesweeperSessionService.state(sessionId);
                    long openedCount = state.cells().stream()
                            .filter(c -> c.state().name().equals("OPENED"))
                            .count();
                    metrics.put("clearedFields", (int) openedCount);
                    metrics.put("gameOver", state.gameOver());
                    metrics.put("won", state.won());
                    metrics.put("flagsPlaced", state.flagsPlaced());
                    metrics.put("boardsCleared", state.boardsCleared());
                    metrics.put("score", state.score());
                    metrics.put("isLocked", state.isLocked());
                } catch (Exception e) {
                    metrics.put("error", "Session not found");
                }
            }
            case TWENTY_FORTY_EIGHT -> {
                try {
                    Game2048State state = game2048SessionService.state(sessionId);
                    metrics.put("score", state.score());
                    metrics.put("gameOver", state.gameOver());
                    metrics.put("moved", state.moved());
                    metrics.put("movesMade", state.movesMade());
                    metrics.put("iceBlockCount", state.iceBlockCount());
                } catch (Exception e) {
                    metrics.put("error", "Session not found");
                }
            }
        }
        return metrics;
    }

    private PlayerProgress extractPlayerProgress(String sessionId, GameType gameType, String playerId, String playerName) {
        switch (gameType) {
            case BLACKJACK -> {
                try {
                    BlackjackState state = blackjackSessionService.state(sessionId);
                    return new PlayerProgress(playerId, playerName, 0, 0,
                            !state.canContinue(), false, 0,
                            state.balance(), state.phase().name(), 0, 0, false);
                } catch (Exception e) {
                    return new PlayerProgress(playerId, playerName, 0, 0, true, false, 0, 0, "", 0, 0, false);
                }
            }
            case MINESWEEPER -> {
                try {
                    MinesweeperState state = minesweeperSessionService.state(sessionId);
                    long openedCount = state.cells().stream()
                            .filter(c -> c.state().name().equals("OPENED"))
                            .count();
                    return new PlayerProgress(playerId, playerName, state.score(), state.boardsCleared(),
                            state.gameOver(), state.won(), 0,
                            0, "", (int) openedCount, state.flagsPlaced(), state.isLocked());
                } catch (Exception e) {
                    return new PlayerProgress(playerId, playerName, 0, 0, true, false, 0, 0, "", 0, 0, false);
                }
            }
            case TWENTY_FORTY_EIGHT -> {
                try {
                    Game2048State state = game2048SessionService.state(sessionId);
                    return new PlayerProgress(playerId, playerName, state.score(), 0,
                            state.gameOver(), false, state.movesMade(),
                            0, "", 0, 0, false);
                } catch (Exception e) {
                    return new PlayerProgress(playerId, playerName, 0, 0, true, false, 0, 0, "", 0, 0, false);
                }
            }
        }
        return new PlayerProgress(playerId, playerName, 0, 0, true, false, 0, 0, "", 0, 0, false);
    }

    public void setGameSession(String roomId, Object gameSession, String gameSessionId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) room.setGameSession(gameSession, gameSessionId);
    }

    public Object getGameSession(String roomId) {
        GameRoom room = rooms.get(roomId);
        return room != null ? room.getGameSession() : null;
    }

    public String getGameSessionId(String roomId) {
        GameRoom room = rooms.get(roomId);
        return room != null ? room.getGameSessionId() : null;
    }

    public String getRoomForPlayer(String playerId) {
        return playerToRoom.get(playerId);
    }

    public List<GameRoom.RoomSummary> getRoomsForPlayer(String playerId) {
        return rooms.values().stream()
                .filter(room -> room.hasPlayer(playerId) || room.getSpectators().stream().anyMatch(p -> p.id().equals(playerId)))
                .map(GameRoom.RoomSummary::from)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupInactiveRooms() {
        Instant now = Instant.now();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, GameRoom> entry : rooms.entrySet()) {
            GameRoom room = entry.getValue();
            synchronized (room) {
                Duration inactiveDuration = Duration.between(room.getLastActivity(), now);
                boolean isEmpty = room.getPlayerCount() == 0 && room.getSpectatorCount() == 0;
                if (isEmpty && inactiveDuration.compareTo(EMPTY_ROOM_TTL) > 0) {
                    toRemove.add(entry.getKey());
                } else if (!isEmpty && inactiveDuration.compareTo(ROOM_TTL) > 0) {
                    toRemove.add(entry.getKey());
                }
            }
        }

        for (String roomId : toRemove) {
            GameRoom room = rooms.get(roomId);
            if (room != null) {
                synchronized (room) {
                    Duration inactiveDuration = Duration.between(room.getLastActivity(), now);
                    boolean isEmpty = room.getPlayerCount() == 0 && room.getSpectatorCount() == 0;
                    if (isEmpty && inactiveDuration.compareTo(EMPTY_ROOM_TTL) > 0) {
                        rooms.remove(roomId);
                        roomPlayerSessions.remove(roomId);
                    } else if (!isEmpty && inactiveDuration.compareTo(ROOM_TTL) > 0) {
                        rooms.remove(roomId);
                        roomPlayerSessions.remove(roomId);
                    }
                }
            }
        }

        Set<String> validRoomIds = rooms.keySet();
        playerToRoom.entrySet().removeIf(entry -> !validRoomIds.contains(entry.getValue()));
        roomPlayerSessions.keySet().retainAll(validRoomIds);
    }

    public void finishRoom(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) settleGame(room);
    }

    public void deleteRoom(String roomId, String requesterId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("Room not found");
        if (!room.isOwner(requesterId)) throw new SecurityException("Only room owner can delete the room");

        room.getPlayers().forEach(p -> playerToRoom.remove(p.id()));
        room.getSpectators().forEach(p -> playerToRoom.remove(p.id()));

        rooms.remove(roomId);
        roomPlayerSessions.remove(roomId);
    }
}
