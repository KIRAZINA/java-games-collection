package com.KIRA_ZINA.backend.common;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GameRoom {
    public enum RoomPhase {
        LOBBY,
        READY_CHECK,
        PLAYING,
        GAME_OVER
    }

    private final String roomId;
    private final String roomName;
    private final GameSettings settings;
    private final String ownerId;
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Map<String, Player> spectators = new ConcurrentHashMap<>();
    private final Set<String> readyPlayers = ConcurrentHashMap.newKeySet();
    private volatile RoomPhase phase = RoomPhase.LOBBY;
    private volatile long readyCheckStartTime;
    private volatile long gameStartTime;
    private Instant createdAt = Instant.now();
    private Instant lastActivity = Instant.now();
    private Object gameSession;
    private String gameSessionId;
    private volatile String winnerId;
    private volatile int winnerScore;

    public GameRoom(String roomId, String roomName, GameSettings settings, String ownerId) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.settings = settings;
        this.ownerId = ownerId;
    }

    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public GameSettings getSettings() { return settings; }
    public String getOwnerId() { return ownerId; }
    public RoomPhase getPhase() { return phase; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastActivity() { return lastActivity; }
    public Object getGameSession() { return gameSession; }
    public String getGameSessionId() { return gameSessionId; }
    public long getGameStartTime() { return gameStartTime; }
    public long getReadyCheckStartTime() { return readyCheckStartTime; }
    public String getWinnerId() { return winnerId; }
    public int getWinnerScore() { return winnerScore; }
    public Set<String> getReadyPlayers() { return Collections.unmodifiableSet(readyPlayers); }

    public boolean isPlayerReady(String playerId) {
        return readyPlayers.contains(playerId);
    }

    public boolean allPlayersReady() {
        return readyPlayers.size() >= players.size();
    }

    public synchronized boolean markReady(String playerId) {
        if (!players.containsKey(playerId)) return false;
        boolean added = readyPlayers.add(playerId);
        if (added) touch();
        return added;
    }

    public synchronized void startReadyCheck() {
        if (phase != RoomPhase.LOBBY) return;
        this.phase = RoomPhase.READY_CHECK;
        this.readyCheckStartTime = System.currentTimeMillis();
        touch();
    }

    public synchronized void startGame() {
        if (phase != RoomPhase.READY_CHECK && phase != RoomPhase.LOBBY) return;
        this.phase = RoomPhase.PLAYING;
        this.gameStartTime = System.currentTimeMillis();
        touch();
    }

    public synchronized void finishGame(String winnerId, int winnerScore) {
        this.phase = RoomPhase.GAME_OVER;
        this.winnerId = winnerId;
        this.winnerScore = winnerScore;
        touch();
    }

    public void setGameSession(Object gameSession, String gameSessionId) {
        this.gameSession = gameSession;
        this.gameSessionId = gameSessionId;
        touch();
    }

    public void touch() {
        this.lastActivity = Instant.now();
    }

    public synchronized boolean addPlayer(String playerId, String playerName, boolean isBot) {
        if (players.size() >= settings.maxPlayers()) return false;
        if (players.containsKey(playerId)) return false;
        Player player = new Player(playerId, playerName, isBot);
        players.put(playerId, player);
        touch();
        return true;
    }

    public synchronized boolean removePlayer(String playerId) {
        Player removed = players.remove(playerId);
        if (removed != null) {
            readyPlayers.remove(playerId);
            touch();
            return players.isEmpty() && spectators.isEmpty();
        }
        return false;
    }

    public synchronized boolean addSpectator(String spectatorId, String spectatorName) {
        if (spectators.containsKey(spectatorId)) return false;
        spectators.put(spectatorId, new Player(spectatorId, spectatorName, false));
        touch();
        return true;
    }

    public synchronized void removeSpectator(String spectatorId) {
        spectators.remove(spectatorId);
        touch();
    }

    public Collection<Player> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    public Set<String> getPlayerIds() {
        return Collections.unmodifiableSet(players.keySet());
    }

    public Collection<Player> getSpectators() {
        return Collections.unmodifiableCollection(spectators.values());
    }

    public int getPlayerCount() { return players.size(); }
    public int getSpectatorCount() { return spectators.size(); }
    public boolean isFull() { return players.size() >= settings.maxPlayers(); }
    public boolean hasPlayer(String playerId) { return players.containsKey(playerId); }
    public boolean isOwner(String playerId) { return ownerId.equals(playerId); }
    public Player getPlayer(String playerId) { return players.get(playerId); }

    public static record Player(String id, String name, boolean isBot) {}

    public record RoomSummary(
            String roomId,
            String roomName,
            GameType gameType,
            RoomPhase phase,
            int playerCount,
            int maxPlayers,
            int spectatorCount,
            boolean passwordProtected,
            String ownerName,
            Instant createdAt,
            Instant lastActivity,
            int timeLimitSeconds,
            boolean isSinglePlayer
    ) {
        public static RoomSummary from(GameRoom room) {
            Player owner = room.players.get(room.ownerId);
            String ownerName = owner != null ? owner.name() : "Unknown";
            return new RoomSummary(
                    room.roomId,
                    room.roomName,
                    room.settings.gameType(),
                    room.phase,
                    room.players.size(),
                    room.settings.maxPlayers(),
                    room.spectators.size(),
                    room.settings.passwordProtected(),
                    ownerName,
                    room.createdAt,
                    room.lastActivity,
                    room.settings.timeLimitSeconds(),
                    room.settings.isSinglePlayer()
            );
        }
    }
}
