package com.KIRA_ZINA.backend.twentyfortyeight.api;

import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048State;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.MoveDirection;
import com.KIRA_ZINA.backend.twentyfortyeight.service.Game2048SessionService;
import com.KIRA_ZINA.backend.common.idempotency.IdempotencyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/2048/sessions")
public class Game2048Controller {
    private final Game2048SessionService sessions;
    private final IdempotencyService idempotencyService;

    public Game2048Controller(Game2048SessionService sessions, IdempotencyService idempotencyService) {
        this.sessions = sessions;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Game2048State createSession() {
        return sessions.createSession();
    }

    @GetMapping("/{sessionId}")
    public Game2048State state(@PathVariable("sessionId") String sessionId) {
        return sessions.state(sessionId);
    }

    @PostMapping("/{sessionId}/moves")
    public Game2048State move(
            @PathVariable("sessionId") String sessionId,
            @RequestBody MoveRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey) throws Exception {
        if (idKey != null && !idKey.isEmpty()) {
            try {
                Game2048State result = idempotencyService.execute("2048:move", sessionId, idKey,
                        () -> sessions.move(sessionId, request.direction()), r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, Game2048State.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        return sessions.move(sessionId, request.direction());
    }

    @PostMapping("/{sessionId}/reset")
    public Game2048State reset(
            @PathVariable("sessionId") String sessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey) throws Exception {
        if (idKey != null && !idKey.isEmpty()) {
            try {
                Game2048State result = idempotencyService.execute("2048:reset", sessionId, idKey,
                        () -> sessions.reset(sessionId), r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, Game2048State.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        return sessions.reset(sessionId);
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

    public record MoveRequest(MoveDirection direction) {}
}
