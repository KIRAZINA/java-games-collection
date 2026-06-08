package com.KIRA_ZINA.backend.common;

import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048Session;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048State;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.MoveDirection;

public class Game2048SessionWrapper implements GameSession {
    private final Game2048Session session;

    public Game2048SessionWrapper(Game2048Session session) {
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

    public Game2048Session getSession() {
        return session;
    }

    public Game2048State move(MoveDirection direction) {
        return session.move(direction);
    }

    public Game2048State reset() {
        return session.reset();
    }
}