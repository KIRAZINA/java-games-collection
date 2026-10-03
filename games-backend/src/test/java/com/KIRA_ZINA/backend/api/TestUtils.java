package com.KIRA_ZINA.backend.api;

import com.KIRA_ZINA.backend.blackjack.service.BlackjackSessionService;
import com.KIRA_ZINA.backend.common.GameRoomService;
import org.awaitility.Awaitility;

import java.time.Duration;

public final class TestUtils {
    private TestUtils() {}

    public static void sleepMs(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    public static void resetGameRooms(GameRoomService service) throws Exception {
        java.lang.reflect.Field roomsField = GameRoomService.class.getDeclaredField("rooms");
        roomsField.setAccessible(true);
        java.util.Map<?, ?> rooms = (java.util.Map<?, ?>) roomsField.get(service);
        rooms.clear();

        java.lang.reflect.Field playerToRoomField = GameRoomService.class.getDeclaredField("playerToRoom");
        playerToRoomField.setAccessible(true);
        java.util.Map<?, ?> pt = (java.util.Map<?, ?>) playerToRoomField.get(service);
        pt.clear();

        java.lang.reflect.Field rpsField = GameRoomService.class.getDeclaredField("roomPlayerSessions");
        rpsField.setAccessible(true);
        java.util.Map<?, ?> rps = (java.util.Map<?, ?>) rpsField.get(service);
        rps.clear();

        java.lang.reflect.Field tokensField = GameRoomService.class.getDeclaredField("playerTokens");
        tokensField.setAccessible(true);
        java.util.Map<?, ?> tokens = (java.util.Map<?, ?>) tokensField.get(service);
        tokens.clear();
    }

    public static void resetBlackjackSessions(BlackjackSessionService service) throws Exception {
        java.lang.reflect.Field f = BlackjackSessionService.class.getDeclaredField("sessions");
        f.setAccessible(true);
        java.util.Map<?, ?> m = (java.util.Map<?, ?>) f.get(service);
        m.clear();
    }
}
