package com.KIRA_ZINA.backend.api;

import com.KIRA_ZINA.backend.blackjack.domain.BlackjackState;
import com.KIRA_ZINA.backend.blackjack.domain.RoundPhase;
import com.KIRA_ZINA.backend.blackjack.service.BlackjackSessionService;
import com.KIRA_ZINA.backend.common.GameRoom;
import com.KIRA_ZINA.backend.common.GameRoomService;
import com.KIRA_ZINA.backend.common.GameSettings;
import com.KIRA_ZINA.backend.common.GameType;
import com.KIRA_ZINA.backend.config.RateLimitFilter;
import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperCellState;
import com.KIRA_ZINA.backend.minesweeper.domain.MinesweeperState;
import com.KIRA_ZINA.backend.minesweeper.service.MinesweeperSessionService;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048State;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.Game2048Tile;
import com.KIRA_ZINA.backend.twentyfortyeight.domain.MoveDirection;
import com.KIRA_ZINA.backend.twentyfortyeight.service.Game2048SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Comprehensive Non-Standard & Concurrent Tests")
public class NonStandardConcurrentTest {

    @Autowired
    private BlackjackSessionService blackjackSessionService;
    @Autowired
    private GameRoomService gameRoomService;
    @Autowired
    private MinesweeperSessionService minesweeperSessionService;
    @Autowired
    private Game2048SessionService game2048SessionService;
    @Autowired
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void isolateState() throws Exception {
        resetSharedState();
    }

    @AfterEach
    void cleanUpState() throws Exception {
        resetSharedState();
    }

    private void resetSharedState() throws Exception {
        TestUtils.resetGameRooms(gameRoomService);
        TestUtils.resetBlackjackSessions(blackjackSessionService);
        clearSessionMap(MinesweeperSessionService.class, minesweeperSessionService);
        clearSessionMap(Game2048SessionService.class, game2048SessionService);
    }

    private static void clearSessionMap(Class<?> serviceType, Object service) throws Exception {
        Field f = serviceType.getDeclaredField("sessions");
        f.setAccessible(true);
        ((Map<?, ?>) f.get(service)).clear();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cacheOf(RateLimitFilter filter) throws Exception {
        Field f = RateLimitFilter.class.getDeclaredField("cache");
        f.setAccessible(true);
        return (Map<String, Object>) f.get(filter);
    }

    @Nested
    @DisplayName("Blackjack Non-Standard & Concurrent")
    class BlackjackNonStandard {

        @Test
        @DisplayName("Bet after stand — placeBet in ROUND_OVER phase throws IllegalStateException (mapped to 409)")
        void betAfterStand() throws Exception {
            String id = blackjackSessionService.createSession(null, null).sessionId();
            blackjackSessionService.startRound(id);
            BlackjackState s = blackjackSessionService.placeBet(id, 10.0);
            if (s.phase() == RoundPhase.PLAYER_TURN) {
                s = blackjackSessionService.stand(id);
            }
            assertEquals(RoundPhase.ROUND_OVER, s.phase(), "precondition: round must be over");
            assertThrows(IllegalStateException.class, () -> blackjackSessionService.placeBet(id, 10.0));
        }

        @Test
        @DisplayName("Hit after bust — once the player has busted, hit() throws IllegalStateException (mapped to 409)")
        void hitAfterBust() throws Exception {
            String id = null;
            boolean busted = false;
            for (int attempt = 0; attempt < 50 && !busted; attempt++) {
                id = blackjackSessionService.createSession(null, null).sessionId();
                blackjackSessionService.startRound(id);
                BlackjackState s = blackjackSessionService.placeBet(id, 1.0);
                while (s.phase() == RoundPhase.PLAYER_TURN) {
                    s = blackjackSessionService.hit(id);
                    if (s.playerValue() > 21) {
                        busted = true;
                    }
                }
            }
            assertNotNull(id);
            final String bustedSessionId = id;
            assertTrue(busted, "no player bust within 50 rounds (statistically impossible)");
            assertEquals(RoundPhase.ROUND_OVER, blackjackSessionService.state(bustedSessionId).phase());
            assertThrows(IllegalStateException.class, () -> blackjackSessionService.hit(bustedSessionId));
        }

        @Test
        @DisplayName("Stand after round over — stand() in ROUND_OVER phase throws IllegalStateException (mapped to 409)")
        void standAfterRoundOver() throws Exception {
            String id = blackjackSessionService.createSession(null, null).sessionId();
            blackjackSessionService.startRound(id);
            BlackjackState s = blackjackSessionService.placeBet(id, 10.0);
            while (s.phase() == RoundPhase.PLAYER_TURN) {
                s = blackjackSessionService.hit(id);
            }
            assertEquals(RoundPhase.ROUND_OVER, s.phase(), "precondition: round must be over");
            assertThrows(IllegalStateException.class, () -> blackjackSessionService.stand(id));
        }

        @Test
        @DisplayName("NaN bet → IllegalArgumentException from validateBet (HTTP maps to 400); balance is never corrupted")
        void nanBet() throws Exception {
            String id = blackjackSessionService.createSession(null, null).sessionId();
            blackjackSessionService.startRound(id);
            assertThrows(IllegalArgumentException.class,
                    () -> blackjackSessionService.placeBet(id, Double.NaN));
            BlackjackState state = blackjackSessionService.state(id);
            assertFalse(Double.isNaN(state.balance()), "NaN bet corrupted the balance");
            assertEquals(100.0, state.balance(), 0.0001, "rejected bet must not touch the balance");
        }

        @Test
        @DisplayName("Concurrent hit and stand — no state corruption; every failure is the phase guard; round ends settled")
        void concurrentHitAndStand() throws Exception {
            String id = blackjackSessionService.createSession(null, null).sessionId();
            BlackjackState s = blackjackSessionService.startRound(id);
            int tries = 0;
            while (s.phase() != RoundPhase.PLAYER_TURN && tries++ < 20) {
                s = blackjackSessionService.placeBet(id, 10.0);
                if (s.phase() == RoundPhase.ROUND_OVER) {
                    s = blackjackSessionService.startRound(id);
                }
            }
            assertEquals(RoundPhase.PLAYER_TURN, s.phase(), "could not reach PLAYER_TURN within 20 rounds");

            int callsPerThread = 50;
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CyclicBarrier go = new CyclicBarrier(2);
            ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();
            for (int t = 0; t < 2; t++) {
                final int threadIndex = t;
                pool.submit(() -> {
                    try {
                        go.await();
                        for (int i = 0; i < callsPerThread; i++) {
                            try {
                                if (threadIndex == 0) {
                                    blackjackSessionService.hit(id);
                                } else {
                                    blackjackSessionService.stand(id);
                                }
                            } catch (IllegalStateException expectedPhaseGuard) {
                                // the other thread already advanced the round past PLAYER_TURN
                            }
                        }
                    } catch (Throwable error) {
                        unexpected.add(error);
                    }
                });
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "worker threads did not finish in time");
            assertTrue(unexpected.isEmpty(), "unexpected exceptions: " + unexpected);

            BlackjackState finalState = blackjackSessionService.state(id);
            assertEquals(RoundPhase.ROUND_OVER, finalState.phase(), "round must settle after hit/stand storm");
            assertFalse(finalState.winner() == null || finalState.winner().name().equals("NONE"),
                    "settled round must have a winner");
            assertFalse(Double.isNaN(finalState.balance()), "balance corrupted by concurrent hit/stand");
            assertFalse(Double.isNaN(finalState.currentBet()), "currentBet corrupted by concurrent hit/stand");
        }
    }

    @Nested
    @DisplayName("Minesweeper Non-Standard & Concurrent")
    class MinesweeperNonStandard {

        @Test
        @DisplayName("Flag after loss — toggleFlag is a no-op on a locked/over board (state unchanged)")
        void flagAfterLoss() throws Exception {
            String id = minesweeperSessionService.createSession(6, 6, 8).sessionId();
            boolean lost = false;
            for (int attempt = 0; attempt < 20 && !lost; attempt++) {
                if (attempt > 0) {
                    minesweeperSessionService.reset(id);
                }
                MinesweeperState s = minesweeperSessionService.open(id, 0, 0);
                for (int r = 0; r < 6 && !s.gameOver(); r++) {
                    for (int c = 0; c < 6 && !s.gameOver(); c++) {
                        if (r == 0 && c == 0) {
                            continue;
                        }
                        s = minesweeperSessionService.open(id, r, c);
                    }
                }
                if (s.gameOver() && !s.won()) {
                    lost = true;
                }
            }
            assertTrue(lost, "could not force a loss within 20 boards");

            MinesweeperState before = minesweeperSessionService.state(id);
            MinesweeperState after = minesweeperSessionService.toggleFlag(id, 3, 3);
            assertEquals(before, after, "flagging after loss must not change the board");
            assertTrue(after.isLocked(), "lost board must stay locked");
        }

        @Test
        @DisplayName("Open out-of-bounds — safe no-op: state unchanged, no exception (no 400 path exists at domain level)")
        void openOutOfBounds() throws Exception {
            String id = minesweeperSessionService.createSession(4, 4, 2).sessionId();
            minesweeperSessionService.open(id, 0, 0);
            MinesweeperState before = minesweeperSessionService.state(id);
            assertEquals(before, minesweeperSessionService.open(id, -1, 0), "open(-1,0) must be a no-op");
            assertEquals(before, minesweeperSessionService.open(id, 0, 4), "open(0,cols) must be a no-op");
            assertEquals(before, minesweeperSessionService.open(id, 99, 99), "open(99,99) must be a no-op");
            assertEquals(before, minesweeperSessionService.state(id), "board must be untouched");
        }

        @Test
        @DisplayName("Concurrent opens on same cell — no lost/duplicated opens: score equals opened-cell view count")
        void concurrentOpenSameCell() throws Exception {
            String id = minesweeperSessionService.createSession(6, 6, 6).sessionId();
            int threads = 4;
            int callsPerThread = 20;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CyclicBarrier go = new CyclicBarrier(threads);
            ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    try {
                        go.await();
                        for (int i = 0; i < callsPerThread; i++) {
                            minesweeperSessionService.open(id, 2, 2);
                        }
                    } catch (Throwable error) {
                        unexpected.add(error);
                    }
                });
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "worker threads did not finish in time");
            assertTrue(unexpected.isEmpty(), "unexpected exceptions: " + unexpected);

            MinesweeperState finalState = minesweeperSessionService.state(id);
            assertFalse(finalState.gameOver() && !finalState.won(),
                    "concurrent same-cell opens must never produce a false loss");
            assertTrue(finalState.firstClickDone(), "first click must have been processed");
            assertTrue(finalState.score() >= 1, "the shared cell must have been opened at least once");
            long openedInView = finalState.cells().stream()
                    .filter(cell -> cell.state() == MinesweeperCellState.OPENED)
                    .count();
            assertEquals(openedInView, finalState.score(),
                    "internal opened-cell counter drifted from the cell view (double count?)");
        }
    }

    @Nested
    @DisplayName("2048 Non-Standard & Concurrent")
    class Game2048NonStandard {

        @Test
        @DisplayName("Concurrent moves — grid never corrupts: unique in-bounds power-of-two tiles, bounded move count")
        void concurrentMoves() throws Exception {
            String id = game2048SessionService.createSession().sessionId();
            int threads = 4;
            int callsPerThread = 25;
            int totalCalls = threads * callsPerThread;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CyclicBarrier go = new CyclicBarrier(threads);
            ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();
            MoveDirection[] directions = MoveDirection.values();
            for (int t = 0; t < threads; t++) {
                final int threadIndex = t;
                pool.submit(() -> {
                    try {
                        go.await();
                        for (int i = 0; i < callsPerThread; i++) {
                            game2048SessionService.move(id, directions[(threadIndex + i) % directions.length]);
                        }
                    } catch (Throwable error) {
                        unexpected.add(error);
                    }
                });
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "worker threads did not finish in time");
            assertTrue(unexpected.isEmpty(), "unexpected exceptions: " + unexpected);

            Game2048State finalState = game2048SessionService.state(id);
            assertTrue(finalState.movesMade() <= totalCalls,
                    "movesMade exceeded the number of issued moves: " + finalState.movesMade());
            assertTrue(finalState.score() >= 0, "negative score after concurrent moves");
            Set<String> positions = new java.util.HashSet<>();
            assertTrue(finalState.tiles().size() <= 16, "more than 16 tiles on a 4x4 grid");
            for (Game2048Tile tile : finalState.tiles()) {
                assertTrue(tile.row() >= 0 && tile.row() < 4, "tile row out of bounds: " + tile.row());
                assertTrue(tile.col() >= 0 && tile.col() < 4, "tile col out of bounds: " + tile.col());
                assertTrue(tile.value() == -1 || (tile.value() > 0 && Integer.bitCount(tile.value()) == 1),
                        "corrupt tile value: " + tile.value());
                assertTrue(positions.add(tile.row() + "," + tile.col()),
                        "duplicate tile at " + tile.row() + "," + tile.col());
            }
        }
    }

    @Nested
    @DisplayName("Rooms / Multiplayer Non-Standard")
    class RoomNonStandard {

        @Test
        @DisplayName("Ready check with missing players — partial readiness keeps LOBBY; all ready transitions to READY_CHECK")
        void readyCheckMissingPlayers() throws Exception {
            GameSettings settings = GameSettings.defaultFor(GameType.MINESWEEPER);
            GameRoom.RoomSummary created =
                    gameRoomService.createRoom("ready-check-partial", settings, "p-ready-owner", "Owner");
            String roomId = created.roomId();
            gameRoomService.joinRoom(roomId, "p-ready-joiner", "Joiner", null);

            gameRoomService.markPlayerReady(roomId, "p-ready-owner");
            GameRoom room = gameRoomService.getRoom(roomId).orElseThrow();
            assertEquals(GameRoom.RoomPhase.LOBBY, room.getPhase(),
                    "partial readiness must not start the ready check");
            assertTrue(room.isPlayerReady("p-ready-owner"), "owner ready flag must be registered");
            assertFalse(room.isPlayerReady("p-ready-joiner"), "joiner must not be ready yet");
            assertEquals(1, room.getReadyPlayers().size());
            assertFalse(room.allPlayersReady());

            gameRoomService.markPlayerReady(roomId, "p-ready-joiner");
            assertEquals(GameRoom.RoomPhase.READY_CHECK, room.getPhase(),
                    "all-ready must start the ready check for non-blackjack games");
            assertTrue(room.allPlayersReady());
            assertEquals(Set.of("p-ready-owner", "p-ready-joiner"), room.getReadyPlayers());
        }

        @Test
        @DisplayName("Two players ready simultaneously — no lost ready updates; room starts")
        void twoPlayersSameRoomConcurrent() throws Exception {
            GameSettings settings = GameSettings.defaultFor(GameType.BLACKJACK);
            GameRoom.RoomSummary created =
                    gameRoomService.createRoom("concurrent-ready", settings, "p-sim-a", "Alice");
            String roomId = created.roomId();
            gameRoomService.joinRoom(roomId, "p-sim-b", "Bob", null);

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CyclicBarrier go = new CyclicBarrier(2);
            ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();
            for (String playerId : List.of("p-sim-a", "p-sim-b")) {
                final String id = playerId;
                pool.submit(() -> {
                    try {
                        go.await();
                        gameRoomService.markPlayerReady(roomId, id);
                    } catch (Throwable error) {
                        unexpected.add(error);
                    }
                });
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "worker threads did not finish in time");
            assertTrue(unexpected.isEmpty(), "unexpected exceptions: " + unexpected);

            GameRoom room = gameRoomService.getRoom(roomId).orElseThrow();
            assertEquals(Set.of("p-sim-a", "p-sim-b"), room.getReadyPlayers(),
                    "a ready update was lost under concurrency");
            assertEquals(GameRoom.RoomPhase.PLAYING, room.getPhase(),
                    "all-ready must start the game for blackjack rooms");
            assertEquals(2, room.getPlayerCount());
        }
    }

    @Nested
    @DisplayName("Rate Limiting & CORS")
    class RateLimitAndCors {

        @Test
        @DisplayName("GET requests exempt from rate limiting — 1000 GETs pass (bucket capacity is 200) and no bucket is touched")
        void getExempt() throws Exception {
            Map<String, Object> cache = cacheOf(rateLimitFilter);
            int cacheSizeBefore = cache.size();
            for (int i = 0; i < 1000; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rooms/state");
                request.setRemoteAddr("198.51.100.77");
                MockHttpServletResponse response = new MockHttpServletResponse();
                MockFilterChain chain = new MockFilterChain();
                rateLimitFilter.doFilter(request, response, chain);
                assertEquals(200, response.getStatus(), "GET #" + i + " was rate limited");
                assertNotNull(chain.getRequest(), "GET #" + i + " did not reach the filter chain");
            }
            assertEquals(cacheSizeBefore, cache.size(),
                    "GET requests must not create or touch rate-limit buckets");
        }
    }
}
