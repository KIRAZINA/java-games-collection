package com.KIRA_ZINA.backend.minesweeper.api;

import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperState;
import com.KIRA_ZINA.backend.minesweeper.service.MinesweeperSessionService;
import com.KIRA_ZINA.backend.common.idempotency.IdempotencyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/minesweeper/sessions")
public class MinesweeperController {
    private final MinesweeperSessionService sessions;
    private final IdempotencyService idempotencyService;

    public MinesweeperController(MinesweeperSessionService sessions, IdempotencyService idempotencyService) {
        this.sessions = sessions;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MinesweeperState createSession(@RequestBody(required = false) CreateSessionRequest request) {
        return sessions.createSession(
                request == null ? null : request.rows(),
                request == null ? null : request.cols(),
                request == null ? null : request.mines()
        );
    }

    @GetMapping("/{sessionId}")
    public MinesweeperState state(@PathVariable("sessionId") String sessionId) {
        return sessions.state(sessionId);
    }

    @PostMapping("/{sessionId}/open")
    public MinesweeperState open(
            @PathVariable("sessionId") String sessionId,
            @RequestBody CellActionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey) throws Exception {
        if (idKey != null && !idKey.isEmpty()) {
            try {
                MinesweeperState result = idempotencyService.execute("minesweeper:open", sessionId, idKey,
                        () -> sessions.open(sessionId, request.row(), request.col()), r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, MinesweeperState.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        return sessions.open(sessionId, request.row(), request.col());
    }

    @PostMapping("/{sessionId}/flag")
    public MinesweeperState toggleFlag(
            @PathVariable("sessionId") String sessionId,
            @RequestBody CellActionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey) throws Exception {
        if (idKey != null && !idKey.isEmpty()) {
            try {
                MinesweeperState result = idempotencyService.execute("minesweeper:flag", sessionId, idKey,
                        () -> sessions.toggleFlag(sessionId, request.row(), request.col()), r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, MinesweeperState.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        return sessions.toggleFlag(sessionId, request.row(), request.col());
    }

    @PostMapping("/{sessionId}/reset")
    public MinesweeperState reset(
            @PathVariable("sessionId") String sessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey) throws Exception {
        if (idKey != null && !idKey.isEmpty()) {
            try {
                MinesweeperState result = idempotencyService.execute("minesweeper:reset", sessionId, idKey,
                        () -> sessions.reset(sessionId), r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, MinesweeperState.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        return sessions.reset(sessionId);
    }

    @PostMapping("/{sessionId}/next-board")
    public MinesweeperState nextBoard(
            @PathVariable("sessionId") String sessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey) throws Exception {
        if (idKey != null && !idKey.isEmpty()) {
            try {
                MinesweeperState result = idempotencyService.execute("minesweeper:next-board", sessionId, idKey,
                        () -> sessions.nextBoard(sessionId), r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, MinesweeperState.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        return sessions.nextBoard(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeSession(@PathVariable("sessionId") String sessionId) {
        sessions.closeSession(sessionId);
    }

    private static IdempotencyService.CachedResponseSnapshot snapshot200(Object value) {
        try {
            return new IdempotencyService.CachedResponseSnapshot(200,
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize idempotent response", e);
        }
    }

    public record CreateSessionRequest(Integer rows, Integer cols, Integer mines) {}
    public record CellActionRequest(int row, int col) {}
}
