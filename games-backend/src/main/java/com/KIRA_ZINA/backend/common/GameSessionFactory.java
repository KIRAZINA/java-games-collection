package com.KIRA_ZINA.backend.common;

import com.KIRA_ZINA.backend.blackjack.domain.BlackjackSession;
import com.KIRA_ZINA.backend.blackjack.domain.BlackjackState;
import com.KIRA_ZINA.backend.blackjack.domain.DealerDifficulty;
import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperSession;
import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperState;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048Session;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048State;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class GameSessionFactory {

    public Object createSession(GameRoom room) {
        return switch (room.getSettings().gameType()) {
            case BLACKJACK -> createBlackjackSession(room);
            case MINESWEEPER -> createMinesweeperSession(room);
            case TWENTY_FORTY_EIGHT -> create2048Session(room);
        };
    }

    public String getSessionId(Object session) {
        if (session instanceof BlackjackSession bs) {
            return bs.getId();
        } else if (session instanceof MinesweeperSession ms) {
            return ms.getSessionId();
        } else if (session instanceof Game2048Session gs) {
            return gs.getSessionId();
        }
        return null;
    }

    public Object getState(Object session) {
        if (session instanceof BlackjackSession bs) {
            return bs.state();
        } else if (session instanceof MinesweeperSession ms) {
            return ms.state();
        } else if (session instanceof Game2048Session gs) {
            return gs.state();
        }
        return null;
    }

    private BlackjackSession createBlackjackSession(GameRoom room) {
        Map<String, Object> settings = room.getSettings().settings();
        double initialBalance = ((Number) settings.getOrDefault("initialBalance", 100.0)).doubleValue();
        String difficultyStr = (String) settings.getOrDefault("difficulty", "BASIC");
        DealerDifficulty difficulty = DealerDifficulty.valueOf(difficultyStr);

        String sessionId = UUID.randomUUID().toString();
        return new BlackjackSession(sessionId, initialBalance, difficulty);
    }

    private MinesweeperSession createMinesweeperSession(GameRoom room) {
        Map<String, Object> settings = room.getSettings().settings();
        int rows = ((Number) settings.getOrDefault("rows", 9)).intValue();
        int cols = ((Number) settings.getOrDefault("cols", 9)).intValue();
        int mines = ((Number) settings.getOrDefault("mines", 10)).intValue();

        String sessionId = UUID.randomUUID().toString();
        return new MinesweeperSession(sessionId, rows, cols, mines);
    }

    private Game2048Session create2048Session(GameRoom room) {
        String sessionId = UUID.randomUUID().toString();
        return new Game2048Session(sessionId);
    }
}