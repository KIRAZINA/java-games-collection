package com.KIRA_ZINA.backend.common;

import com.KIRA_ZINA.backend.blackjack.domain.BlackjackState;
import com.KIRA_ZINA.backend.blackjack.service.BlackjackSessionService;
import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperState;
import com.KIRA_ZINA.backend.minesweeper.service.MinesweeperSessionService;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048State;
import com.KIRA_ZINA.backend.twentyfortyeight.service.Game2048SessionService;
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
        String roomId = UUID.randomUUID().toString();
        String passwordHash = null;
        if (settings.passwordProtected() && settings.passwordHash() != null && !settings.passwordHash().isEmpty()) {
            passwordHash = passwordEncoder.encode(settings.passwordHash());
        }
        GameSettings securedSettings = new GameSettings(
                settings.gameType(),
                settings.settings(),
                settings.passwordProtected(),
                passwordHash,
                settings.allowBots(),
                settings.maxPlayers()
        );
        GameRoom room = new GameRoom(roomId, roomName, securedSettings, ownerId);
        room.addPlayer(ownerId, ownerName, false);
        rooms.put(roomId, room);
        playerToRoom.put(ownerId, roomId);
        roomPlayerSessions.put(roomId, new ConcurrentHashMap<>());
        return GameRoom.RoomSummary.from(room);
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
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

        if (room.getState() != GameRoom.RoomState.WAITING) {
            throw new IllegalStateException("Room is not accepting players (state: " + room.getState() + ")");
        }

        if (room.isFull()) {
            throw new IllegalStateException("Room is full");
        }

        if (room.getSettings().passwordProtected()) {
            String storedHash = room.getSettings().passwordHash();
            if (storedHash == null || password == null || !passwordEncoder.matches(password, storedHash)) {
                throw new SecurityException("Invalid password");
            }
        }

        boolean added = room.addPlayer(playerId, playerName, false);
        if (!added) {
            throw new IllegalStateException("Failed to join room");
        }

        playerToRoom.put(playerId, roomId);
        return GameRoom.RoomSummary.from(room);
    }

    public GameRoom.RoomSummary joinAsSpectator(String roomId, String spectatorId, String spectatorName) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

        room.addSpectator(spectatorId, spectatorName);
        return GameRoom.RoomSummary.from(room);
    }

    public boolean leaveRoom(String playerId) {
        String roomId = playerToRoom.remove(playerId);
        if (roomId == null) {
            return false;
        }

        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return false;
        }

        room.removePlayer(playerId);
        Map<String, String> sessions = roomPlayerSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(playerId);
        }

        if (room.getPlayerCount() == 0) {
            rooms.remove(roomId);
            roomPlayerSessions.remove(roomId);
            return true;
        }
        return false;
    }

    public boolean leaveAsSpectator(String roomId, String spectatorId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return false;
        }
        room.removeSpectator(spectatorId);
        return true;
    }

    public void registerPlayerSession(String roomId, String playerId, String sessionId) {
        Map<String, String> sessions = roomPlayerSessions.get(roomId);
        if (sessions == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }
        GameRoom room = rooms.get(roomId);
        if (room == null || !room.hasPlayer(playerId)) {
            throw new IllegalArgumentException("Player not in room");
        }
        sessions.put(playerId, sessionId);
    }

    public RoomStateResponse getRoomState(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

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

        return new RoomStateResponse(
                roomId,
                room.getSettings().gameType().name(),
                room.getState().name(),
                room.getPlayerCount(),
                playerStates
        );
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
                } catch (Exception e) {
                    metrics.put("error", "Session not found");
                }
            }
        }
        return metrics;
    }

    public void setGameSession(String roomId, Object gameSession, String gameSessionId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.setGameSession(gameSession, gameSessionId);
        }
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
        rooms.entrySet().removeIf(entry -> {
            GameRoom room = entry.getValue();
            Duration inactiveDuration = Duration.between(room.getLastActivity(), now);

            if (room.getPlayerCount() == 0 && room.getSpectatorCount() == 0) {
                return inactiveDuration.compareTo(EMPTY_ROOM_TTL) > 0;
            }
            return inactiveDuration.compareTo(ROOM_TTL) > 0;
        });

        Set<String> validRoomIds = rooms.keySet();
        playerToRoom.entrySet().removeIf(entry -> !validRoomIds.contains(entry.getValue()));
        roomPlayerSessions.keySet().retainAll(validRoomIds);
    }

    public void finishRoom(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.setFinished();
        }
    }

    public void deleteRoom(String roomId, String requesterId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        if (!room.isOwner(requesterId)) {
            throw new SecurityException("Only room owner can delete the room");
        }

        room.getPlayers().forEach(p -> playerToRoom.remove(p.id()));
        room.getSpectators().forEach(p -> playerToRoom.remove(p.id()));

        rooms.remove(roomId);
        roomPlayerSessions.remove(roomId);
    }
}
