package com.KIRA_ZINA.backend.blackjack.api;

import com.KIRA_ZINA.backend.blackjack.domain.BlackjackState;
import com.KIRA_ZINA.backend.blackjack.domain.DealerDifficulty;
import com.KIRA_ZINA.backend.blackjack.service.BlackjackSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blackjack/sessions")
public class BlackjackController {
    private final BlackjackSessionService sessions;

    public BlackjackController(BlackjackSessionService sessions) {
        this.sessions = sessions;
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
    public BlackjackState startRound(@PathVariable("sessionId") String sessionId) {
        return sessions.startRound(sessionId);
    }

    @PostMapping("/{sessionId}/bets")
    public BlackjackState placeBet(@PathVariable("sessionId") String sessionId, @RequestBody BetRequest request) {
        return sessions.placeBet(sessionId, request.amount());
    }

    @PostMapping("/{sessionId}/hit")
    public BlackjackState hit(@PathVariable("sessionId") String sessionId) {
        return sessions.hit(sessionId);
    }

    @PostMapping("/{sessionId}/stand")
    public BlackjackState stand(@PathVariable("sessionId") String sessionId) {
        return sessions.stand(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeSession(@PathVariable("sessionId") String sessionId) {
        sessions.closeSession(sessionId);
    }

    public record CreateSessionRequest(Double initialBalance, DealerDifficulty difficulty) {
    }

    public record BetRequest(double amount) {
    }
}
