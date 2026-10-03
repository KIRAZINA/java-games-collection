package com.KIRA_ZINA.backend.blackjack.api;

import com.KIRA_ZINA.backend.blackjack.domain.BlackjackState;
import com.KIRA_ZINA.backend.blackjack.domain.DealerDifficulty;
import com.KIRA_ZINA.backend.blackjack.service.BlackjackSessionService;
import com.KIRA_ZINA.backend.common.idempotency.IdempotencyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blackjack/sessions")
public class BlackjackController {
    private final BlackjackSessionService sessions;
    private final IdempotencyService idempotencyService;

    public BlackjackController(BlackjackSessionService sessions, IdempotencyService idempotencyService) {
        this.sessions = sessions;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlackjackState createSession(@RequestBody(required = false) CreateSessionRequest request) {
        return sessions.createSession(
                request == null ? null : request.initialBalance(),
                request == null ? null : request.difficulty()
        );
    }

    @GetMapping("/{sessionId}")
    public BlackjackState getState(@PathVariable("sessionId") String sessionId) {
        return sessions.state(sessionId);
    }

    @PostMapping("/{sessionId}/rounds")
    public BlackjackState startRound(
            @PathVariable("sessionId") String sessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey) throws Exception {
        if (idKey != null && !idKey.isEmpty()) {
            try {
                BlackjackState result = idempotencyService.execute("blackjack:rounds", sessionId, idKey,
                        () -> sessions.startRound(sessionId), r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, BlackjackState.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        return sessions.startRound(sessionId);
    }

    @PostMapping("/{sessionId}/bets")
    public BlackjackState placeBet(
            @PathVariable("sessionId") String sessionId,
            @RequestBody BetRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey) throws Exception {
        if (idKey != null && !idKey.isEmpty()) {
            try {
                BlackjackState result = idempotencyService.execute("blackjack:bets", sessionId, idKey,
                        () -> sessions.placeBet(sessionId, request.amount()), r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, BlackjackState.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        return sessions.placeBet(sessionId, request.amount());
    }

    @PostMapping("/{sessionId}/hit")
    public BlackjackState hit(
            @PathVariable("sessionId") String sessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey) throws Exception {
        if (idKey != null && !idKey.isEmpty()) {
            try {
                BlackjackState result = idempotencyService.execute("blackjack:hit", sessionId, idKey,
                        () -> sessions.hit(sessionId), r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, BlackjackState.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        return sessions.hit(sessionId);
    }

    @PostMapping("/{sessionId}/stand")
    public BlackjackState stand(
            @PathVariable("sessionId") String sessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idKey) throws Exception {
        if (idKey != null && !idKey.isEmpty()) {
            try {
                BlackjackState result = idempotencyService.execute("blackjack:stand", sessionId, idKey,
                        () -> sessions.stand(sessionId), r -> snapshot200(r));
                return result;
            } catch (IdempotencyService.IdempotencyReplayException replay) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(replay.body, BlackjackState.class);
            } catch (IdempotencyService.IdempotencyInProgressException inProgress) {
                throw new IllegalStateException("Duplicate request in progress");
            }
        }
        return sessions.stand(sessionId);
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

    public record CreateSessionRequest(Double initialBalance, DealerDifficulty difficulty) {}
    public record BetRequest(double amount) {}
}
