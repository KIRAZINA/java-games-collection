package com.KIRA_ZINA.backend.minesweeper.api;

import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperState;
import com.KIRA_ZINA.backend.minesweeper.service.MinesweeperSessionService;
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
@RequestMapping("/api/minesweeper/sessions")
public class MinesweeperController {
    private final MinesweeperSessionService sessions;

    public MinesweeperController(MinesweeperSessionService sessions) {
        this.sessions = sessions;
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
    public MinesweeperState open(@PathVariable("sessionId") String sessionId, @RequestBody CellActionRequest request) {
        return sessions.open(sessionId, request.row(), request.col());
    }

    @PostMapping("/{sessionId}/flag")
    public MinesweeperState toggleFlag(@PathVariable("sessionId") String sessionId, @RequestBody CellActionRequest request) {
        return sessions.toggleFlag(sessionId, request.row(), request.col());
    }

    @PostMapping("/{sessionId}/reset")
    public MinesweeperState reset(@PathVariable("sessionId") String sessionId) {
        return sessions.reset(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeSession(@PathVariable("sessionId") String sessionId) {
        sessions.closeSession(sessionId);
    }

    public record CreateSessionRequest(Integer rows, Integer cols, Integer mines) {
    }

    public record CellActionRequest(int row, int col) {
    }
}
