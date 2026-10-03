package com.KIRA_ZINA.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.KIRA_ZINA.backend.twentyfortyeight.service.Game2048SessionService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Step 3a item 4 — phase transitions must be driven by the @Scheduled task,
 * not by polling GET /state or GET /progress. Runs against the Spring-managed
 * GameRoomService so the real scheduler (fixedDelay=1000) is active.
 */
@SpringBootTest(properties = "logging.level.root=WARN")
@DisplayName("Adversarial Room Phase Transitions (scheduled)")
class AdversarialRoomPhaseTest {

    @Autowired
    private GameRoomService roomService;

    @Autowired
    private Game2048SessionService game2048SessionService;

    private final List<String> createdOwners = new ArrayList<>();

    @AfterEach
    void cleanUpRooms() {
        for (String owner : createdOwners) {
            roomService.leaveRoom(owner);
        }
        createdOwners.clear();
    }

    private static GameSettings mp2048(int timeLimitSeconds) {
        return new GameSettings(GameType.TWENTY_FORTY_EIGHT, Map.of("size", 4),
                false, null, false, 2, timeLimitSeconds, false);
    }

    private static String uniqueId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private GameRoom requireRoom(String roomId) {
        return roomService.getRoom(roomId).orElseThrow();
    }

    @Nested
    @DisplayName("4. tickRoomPhase — scheduler-driven transitions without polling")
    class Item04_SchedulerDrivenTransitions {

        @Test
        @DisplayName("A. READY_CHECK room reaches PLAYING after READY_CHECK_DURATION_MS with zero polling")
        void readyCheckTransitionsWithoutPolling() throws Exception {
            String owner = uniqueId("phaseA-owner");
            createdOwners.add(owner);
            GameRoom.RoomSummary s = roomService.createRoom("PhaseA", mp2048(0), owner, "OA");
            roomService.markPlayerReady(s.roomId(), owner);
            assertThat(requireRoom(s.roomId()).getPhase()).isEqualTo(GameRoom.RoomPhase.READY_CHECK);

            // READY_CHECK_DURATION_MS (3000) + 200ms as specified. No GET /state or
            // GET /progress call happens anywhere in this test.
            Thread.sleep(3200);

            // scheduler granularity is fixedDelay=1000, so allow up to 4 more seconds
            // for the tick that observes the expired window
            Awaitility.await()
                    .atMost(Duration.ofSeconds(4))
                    .pollInterval(Duration.ofMillis(100))
                    .until(() -> requireRoom(s.roomId()).getPhase() == GameRoom.RoomPhase.PLAYING);
        }

        @Test
        @DisplayName("B. PLAYING room with 2s limit reaches GAME_OVER with a winner after 2.5s without polling")
        void playingSettlesWithoutPolling() throws Exception {
            String owner = uniqueId("phaseB-owner");
            createdOwners.add(owner);
            GameRoom.RoomSummary s = roomService.createRoom("PhaseB", mp2048(2), owner, "OB");

            String sessionId = game2048SessionService.createSession().sessionId();
            roomService.registerPlayerSession(s.roomId(), owner, sessionId);
            requireRoom(s.roomId()).startGame();
            assertThat(requireRoom(s.roomId()).getPhase()).isEqualTo(GameRoom.RoomPhase.PLAYING);

            // Wait the specified 2.5s without any polling call...
            Thread.sleep(2500);

            // ...then allow scheduler granularity (fixedDelay=1000) for the settling tick.
            Awaitility.await()
                    .atMost(Duration.ofSeconds(4))
                    .pollInterval(Duration.ofMillis(100))
                    .until(() -> {
                        var room = roomService.getRoom(s.roomId());
                        return room.isPresent()
                                && room.get().getPhase() == GameRoom.RoomPhase.GAME_OVER;
                    });

            GameRoom room = requireRoom(s.roomId());
            assertThat(room.getPhase()).isEqualTo(GameRoom.RoomPhase.GAME_OVER);
            assertThat(room.getWinnerId())
                    .as("settled game must have a winner")
                    .isNotNull();
        }
    }
}
