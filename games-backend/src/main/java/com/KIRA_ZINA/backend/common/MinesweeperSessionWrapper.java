package com.KIRA_ZINA.backend.common;

import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperSession;
import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperState;

public class MinesweeperSessionWrapper implements GameSession {
    private final MinesweeperSession session;

    public MinesweeperSessionWrapper(MinesweeperSession session) {
        this.session = session;
    }

    @Override
    public String getSessionId() {
        return session.getSessionId();
    }

    @Override
    public Object getState() {
        return session.state();
    }

    @Override
    public void close() {
        session.close();
    }

    public MinesweeperSession getSession() {
        return session;
    }

    public MinesweeperState open(int row, int col) {
        return session.open(row, col);
    }

    public MinesweeperState toggleFlag(int row, int col) {
        return session.toggleFlag(row, col);
    }

    public MinesweeperState reset() {
        return session.reset();
    }
}