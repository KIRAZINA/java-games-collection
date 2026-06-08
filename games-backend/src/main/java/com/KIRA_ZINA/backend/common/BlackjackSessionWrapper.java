package com.KIRA_ZINA.backend.common;

import com.KIRA_ZINA.backend.blackjack.domain.BlackjackSession;
import com.KIRA_ZINA.backend.blackjack.domain.BlackjackState;

public class BlackjackSessionWrapper implements GameSession {
    private final BlackjackSession session;

    public BlackjackSessionWrapper(BlackjackSession session) {
        this.session = session;
    }

    @Override
    public String getSessionId() {
        return session.getId();
    }

    @Override
    public Object getState() {
        return session.state();
    }

    @Override
    public void close() {
        session.close();
    }

    public BlackjackSession getSession() {
        return session;
    }

    public BlackjackState startRound() {
        return session.startRound();
    }

    public BlackjackState placeBet(double amount) {
        return session.placeBet(amount);
    }

    public BlackjackState hit() {
        return session.hit();
    }

    public BlackjackState stand() {
        return session.stand();
    }
}