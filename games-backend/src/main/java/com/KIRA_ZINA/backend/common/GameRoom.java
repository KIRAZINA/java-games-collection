package com.KIRA_ZINA.backend.common;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GameRoom {
    public enum RoomState {
        WAITING,      // Room created, waiting for players
        IN_PROGRESS,  // Game is actively being played
        FINISHED      // Game completed
    }

    private final String roomId;
    private final String roomName;
    private final GameSettings settings;
    private final String ownerId;
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Map<String, Player> spectators = new ConcurrentHashMap<>();
    private RoomState state = RoomState.WAITING;
    private Instant createdAt = Instant.now();
    private Instant lastActivity = Instant.now();
    private Object gameSession; // The actual game session (BlackjackSession, MinesweeperSession, etc.)
    private String gameSessionId; // ID of the game session

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
    public RoomState getState() { return state; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastActivity() { return lastActivity; }
    public Object getGameSession() { return gameSession; }
    public String getGameSessionId() { return gameSessionId; }

    public void setGameSession(Object gameSession, String gameSessionId) {
        this.gameSession = gameSession;
        this.gameSessionId = gameSessionId;
        this.state = RoomState.IN_PROGRESS;
        touch();
    }

    public void setFinished() {
        this.state = RoomState.FINISHED;
        touch();
    }

    public void touch() {
        this.lastActivity = Instant.now();
    }

    public synchronized boolean addPlayer(String playerId, String playerName, boolean isBot) {
        if (players.size() >= settings.maxPlayers()) {
            return false;
        }
        if (players.containsKey(playerId)) {
            return false;
        }
        Player player = new Player(playerId, playerName, isBot);
        players.put(playerId, player);
        touch();
        return true;
    }

    public synchronized boolean removePlayer(String playerId) {
        Player removed = players.remove(playerId);
        if (removed != null) {
            touch();
            // If owner leaves, transfer ownership to next player
            if (ownerId.equals(playerId) && !players.isEmpty()) {
                // Note: In a real implementation, you'd need to update the owner
                // For now, we'll just leave it as is
            }
            // Auto-delete if no players left
            return players.isEmpty() && spectators.isEmpty();
        }
        return false;
    }

    public synchronized boolean addSpectator(String spectatorId, String spectatorName) {
        if (spectators.containsKey(spectatorId)) {
            return false;
        }
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

    public int getPlayerCount() {
        return players.size();
    }

    public int getSpectatorCount() {
        return spectators.size();
    }

    public boolean isFull() {
        return players.size() >= settings.maxPlayers();
    }

    public boolean hasPlayer(String playerId) {
        return players.containsKey(playerId);
    }

    public boolean isOwner(String playerId) {
        return ownerId.equals(playerId);
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public static record Player(String id, String name, boolean isBot) {}

    public record RoomSummary(
            String roomId,
            String roomName,
            GameType gameType,
            RoomState state,
            int playerCount,
            int maxPlayers,
            int spectatorCount,
            boolean passwordProtected,
            String ownerName,
            Instant createdAt,
            Instant lastActivity
    ) {
        public static RoomSummary from(GameRoom room) {
            Player owner = room.players.get(room.ownerId);
            String ownerName = owner != null ? owner.name() : "Unknown";
            return new RoomSummary(
                    room.roomId,
                    room.roomName,
                    room.settings.gameType(),
                    room.state,
                    room.players.size(),
                    room.settings.maxPlayers(),
                    room.spectators.size(),
                    room.settings.passwordProtected(),
                    ownerName,
                    room.createdAt,
                    room.lastActivity
            );
        }
    }
}