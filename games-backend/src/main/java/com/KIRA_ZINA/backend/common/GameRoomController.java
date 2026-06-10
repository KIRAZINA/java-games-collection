package com.KIRA_ZINA.backend.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class GameRoomController {
    private final GameRoomService roomService;

    public GameRoomController(GameRoomService roomService) {
        this.roomService = roomService;
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
    public GameRoom.RoomSummary createRoom(@RequestBody CreateRoomRequest request) {
        GameSettings settings = request.settings() != null ? request.settings() : GameSettings.defaultFor(request.gameType());
        return roomService.createRoom(request.roomName(), settings, request.ownerId(), request.ownerName());
    }

    @GetMapping("/{roomId}")
    public GameRoom.RoomSummary getRoom(@PathVariable("roomId") String roomId) {
        return roomService.getRoom(roomId)
                .map(GameRoom.RoomSummary::from)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

    @PostMapping("/{roomId}/join")
    public GameRoom.RoomSummary joinRoom(@PathVariable("roomId") String roomId, @RequestBody JoinRoomRequest request) {
        return roomService.joinRoom(roomId, request.playerId(), request.playerName(), request.password());
    }

    @PostMapping("/{roomId}/spectate")
    public GameRoom.RoomSummary joinAsSpectator(@PathVariable("roomId") String roomId, @RequestBody SpectateRequest request) {
        return roomService.joinAsSpectator(roomId, request.spectatorId(), request.spectatorName());
    }

    @DeleteMapping("/{roomId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveRoom(@PathVariable("roomId") String roomId, @RequestBody LeaveRequest request) {
        roomService.leaveRoom(request.playerId());
    }

    @DeleteMapping("/{roomId}/spectate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveAsSpectator(@PathVariable("roomId") String roomId, @RequestBody LeaveRequest request) {
        roomService.leaveAsSpectator(roomId, request.playerId());
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable("roomId") String roomId, @RequestBody DeleteRoomRequest request) {
        roomService.deleteRoom(roomId, request.requesterId());
    }

    @GetMapping("/player/{playerId}")
    public List<GameRoom.RoomSummary> getRoomsForPlayer(@PathVariable("playerId") String playerId) {
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
    public void registerSession(@PathVariable("roomId") String roomId, @RequestBody RegisterSessionRequest request) {
        roomService.registerPlayerSession(roomId, request.playerId(), request.sessionId());
    }

    public record CreateRoomRequest(
            String roomName,
            GameType gameType,
            GameSettings settings,
            String ownerId,
            String ownerName
    ) {}

    public record JoinRoomRequest(
            String playerId,
            String playerName,
            String password
    ) {}

    public record SpectateRequest(
            String spectatorId,
            String spectatorName
    ) {}

    public record LeaveRequest(
            String playerId
    ) {}

    public record DeleteRoomRequest(
            String requesterId
    ) {}

    public record RegisterSessionRequest(
            String playerId,
            String sessionId
    ) {}
}