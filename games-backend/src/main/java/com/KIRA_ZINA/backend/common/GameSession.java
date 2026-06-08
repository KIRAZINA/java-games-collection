package com.KIRA_ZINA.backend.common;

public interface GameSession {
    String getSessionId();
    Object getState();
    void close();
}