package com.KIRA_ZINA.backend.twentyfortyeight.api;

import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048State;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.MoveDirection;
import com.KIRA_ZINA.backend.twentyfortyeight.service.Game2048SessionService;
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
@RequestMapping("/api/2048/sessions")
public class Game2048Controller {
    private final Game2048SessionService sessions;

    public Game2048Controller(Game2048SessionService sessions) {
        this.sessions = sessions;
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
    public Game2048State move(@PathVariable("sessionId") String sessionId, @RequestBody MoveRequest request) {
        return sessions.move(sessionId, request.direction());
    }

    @PostMapping("/{sessionId}/reset")
    public Game2048State reset(@PathVariable("sessionId") String sessionId) {
        return sessions.reset(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeSession(@PathVariable("sessionId") String sessionId) {
        sessions.closeSession(sessionId);
    }

    public record MoveRequest(MoveDirection direction) {
    }
}
