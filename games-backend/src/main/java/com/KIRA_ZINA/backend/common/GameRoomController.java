package com.KIRA_ZINA.backend.common;

import com.KIRA_ZINA.backend.common.exception.ResourceNotFoundException;
import com.KIRA_ZINA.backend.common.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class GameRoomController {
    private final GameRoomService roomService;
    private final IdempotencyService idempotencyService;

    public GameRoomController(GameRoomService roomService, IdempotencyService idempotencyService) {
        this.roomService = roomService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    public List<GameRoom.RoomSummary> listAllRooms() {
        return roomService.listAllRooms();
    }

    @GetMapping(params = "type")
    public List<GameRoom.RoomSummary> listRoomsByType(@RequestParam("type") GameType type) {
        return roomService.listRooms(type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameRoom.RoomSummary createRoom(@Valid @RequestBody CreateRoomRequest request) {
        GameSettings settings = request.settings() != null ? request.settings() : GameSettings.defaultFor(request.gameType());
        GameRoom.RoomSummary summary = roomService.createRoom(request.roomName(), settings, request.ownerId(), request.ownerName());
        return summary.withPlayerToken(roomService.issuePlayerToken(summary.roomId(), request.ownerId()));
    }

    @GetMapping("/{roomId}")
    public GameRoom.RoomSummary getRoom(@PathVariable("roomId") String roomId) {
        return roomService.getRoom(roomId)
                .map(GameRoom.RoomSummary::from)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
    }

    @PostMapping("/{roomId}/join")
    public GameRoom.RoomSummary joinRoom(
            @PathVariable("roomId") String roomId,
            @Valid @RequestBody JoinRoomRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey) throws Exception {
        if (idKey != null && !idKey.isEmpty()) {
            try {
                GameRoom.RoomSummary result = idempotencyService.execute("rooms:join", roomId + ":" + request.playerId(), idKey,
                        () -> {
                            GameRoom.RoomSummary summary = roomService.joinRoom(roomId, request.playerId(), request.playerName(), request.password());
                            return summary.withPlayerToken(roomService.issuePlayerToken(roomId, request.playerId()));
                        },
                        r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, GameRoom.RoomSummary.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        GameRoom.RoomSummary summary = roomService.joinRoom(roomId, request.playerId(), request.playerName(), request.password());
        return summary.withPlayerToken(roomService.issuePlayerToken(roomId, request.playerId()));
    }

    @PostMapping("/{roomId}/spectate")
    public GameRoom.RoomSummary joinAsSpectator(@PathVariable("roomId") String roomId, @Valid @RequestBody SpectateRequest request) {
        GameRoom.RoomSummary summary = roomService.joinAsSpectator(roomId, request.spectatorId(), request.spectatorName());
        return summary.withPlayerToken(roomService.issuePlayerToken(roomId, request.spectatorId()));
    }

    @DeleteMapping("/{roomId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveRoom(
            @PathVariable("roomId") String roomId,
            @Valid @RequestBody LeaveRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey,
            @RequestHeader(value = "X-Player-Token", required = false) String playerToken) throws Exception {
        roomService.verifyPlayerToken(roomId, request.playerId(), playerToken);
        if (idKey != null && !idKey.isEmpty()) {
            try {
                idempotencyService.execute("rooms:leave", roomId + ":" + request.playerId(), idKey, () -> {
                    roomService.leaveRoom(request.playerId());
                    return null;
                }, r -> new IdempotencyService.CachedResponseSnapshot(204, ""));
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                // replay no-op for 204
                return;
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
            return;
        }
        roomService.leaveRoom(request.playerId());
    }

    @DeleteMapping("/{roomId}/spectate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveAsSpectator(@PathVariable("roomId") String roomId, @Valid @RequestBody LeaveRequest request) {
        roomService.leaveAsSpectator(roomId, request.playerId());
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(
            @PathVariable("roomId") String roomId,
            @Valid @RequestBody DeleteRoomRequest request,
            @RequestHeader(value = "X-Player-Token", required = false) String playerToken) {
        roomService.verifyPlayerToken(roomId, request.requesterId(), playerToken);
        roomService.deleteRoom(roomId, request.requesterId());
    }

    @GetMapping("/player/{playerId}")
    public List<GameRoom.RoomSummary> getRoomsForPlayer(
            @PathVariable("playerId") String playerId,
            @RequestHeader(value = "X-Player-Token", required = false) String playerToken) {
        roomService.verifyPlayerTokenForPlayer(playerId, playerToken);
        return roomService.getRoomsForPlayer(playerId);
    }

    @GetMapping("/{roomId}/state")
    public RoomStateResponse getRoomState(@PathVariable("roomId") String roomId) {
        return roomService.getRoomState(roomId);
    }

    @GetMapping("/{roomId}/progress")
    public RoomProgressResponse getRoomProgress(@PathVariable("roomId") String roomId) {
        return roomService.getRoomProgress(roomId);
    }

    @PostMapping("/{roomId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerSession(
            @PathVariable("roomId") String roomId,
            @Valid @RequestBody RegisterSessionRequest request,
            @RequestHeader(value = "X-Player-Token", required = false) String playerToken) {
        roomService.verifyPlayerToken(roomId, request.playerId(), playerToken);
        roomService.registerPlayerSession(roomId, request.playerId(), request.sessionId());
    }

    /**
     * Marks the player ready and rotates their token (Step 6b.3): the value
     * that authenticated THIS request stops working, and the replacement rides
     * back in the response body. Clients must store it before their next
     * authenticated call - the room games register their session the moment the
     * match starts. An idempotent replay returns the same rotated token rather
     * than rotating twice, so a retried request cannot strand the client with a
     * token the server has already discarded.
     */
    @PostMapping("/{roomId}/ready")
    @ResponseStatus(HttpStatus.OK)
    public ReadyResponse markReady(
            @PathVariable("roomId") String roomId,
            @Valid @RequestBody MarkReadyRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey,
            @RequestHeader(value = "X-Player-Token", required = false) String playerToken) throws Exception {
        roomService.verifyPlayerToken(roomId, request.playerId(), playerToken);
        if (idKey != null && !idKey.isEmpty()) {
            try {
                ReadyResponse result = idempotencyService.execute("rooms:ready", roomId + ":" + request.playerId(), idKey,
                        () -> {
                            roomService.markPlayerReady(roomId, request.playerId());
                            return new ReadyResponse(roomService.rotatePlayerToken(roomId, request.playerId()));
                        },
                        r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, ReadyResponse.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        roomService.markPlayerReady(roomId, request.playerId());
        return new ReadyResponse(roomService.rotatePlayerToken(roomId, request.playerId()));
    }

    private static IdempotencyService.CachedResponseSnapshot snapshot200(Object value) {
        try {
            return new IdempotencyService.CachedResponseSnapshot(200,
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize idempotent response", e);
        }
    }

    public record CreateRoomRequest(
            @NotBlank(message = "must not be blank") String roomName,
            @NotNull(message = "must not be null") GameType gameType,
            @Valid GameSettings settings,
            @NotBlank(message = "must not be blank") String ownerId,
            @NotBlank(message = "must not be blank") String ownerName
    ) {}

    public record JoinRoomRequest(
            @NotBlank(message = "must not be blank") String playerId,
            @NotBlank(message = "must not be blank") String playerName,
            String password
    ) {}

    public record SpectateRequest(
            @NotBlank(message = "must not be blank") String spectatorId,
            @NotBlank(message = "must not be blank") String spectatorName
    ) {}

    public record LeaveRequest(
            @NotBlank(message = "must not be blank") String playerId
    ) {}

    public record DeleteRoomRequest(
            @NotBlank(message = "must not be blank") String requesterId
    ) {}

    public record RegisterSessionRequest(
            @NotBlank(message = "must not be blank") String playerId,
            @NotBlank(message = "must not be blank") String sessionId
    ) {}

    public record MarkReadyRequest(
            @NotBlank(message = "must not be blank") String playerId
    ) {}

    public record ReadyResponse(String playerToken) {}
}
