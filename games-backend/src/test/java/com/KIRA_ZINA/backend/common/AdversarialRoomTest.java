package com.KIRA_ZINA.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.KIRA_ZINA.backend.blackjack.service.BlackjackSessionService;
import com.KIRA_ZINA.backend.common.exception.ResourceNotFoundException;
import com.KIRA_ZINA.backend.minesweeper.service.MinesweeperSessionService;
import com.KIRA_ZINA.backend.twentyfortyeight.service.Game2048SessionService;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Adversarial tests for the room/multiplayer layer (Step 3a, items 1-9 unit-level).
 * One nested class per numbered item; each test tries to break the stated invariant.
 */
@DisplayName("Adversarial Room / Multiplayer")
class AdversarialRoomTest {

    private BlackjackSessionService blackjack;
    private MinesweeperSessionService minesweeper;
    private Game2048SessionService game2048;
    private GameRoomService roomService;

    @BeforeEach
    void setUp() {
        blackjack = new BlackjackSessionService();
        minesweeper = new MinesweeperSessionService();
        game2048 = new Game2048SessionService();
        roomService = new GameRoomService(blackjack, minesweeper, game2048);
    }

    private static GameSettings mp2048() {
        return new GameSettings(GameType.TWENTY_FORTY_EIGHT, Map.of("size", 4),
                false, null, false, 2, 0, false);
    }

    private GameRoom requireRoom(String roomId) {
        return roomService.getRoom(roomId).orElseThrow();
    }

    private static void setLastActivity(GameRoom room, Instant when) {
        try {
            Field f = GameRoom.class.getDeclaredField("lastActivity");
            f.setAccessible(true);
            f.set(room, when);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static String uniqueId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Nested
    @DisplayName("1. joinRoom — cross-room atomicity for the same playerId")
    class Item01_CrossRoomAtomicity {

        @Test
        @DisplayName("same playerId joining two rooms concurrently ends up in exactly one room, mapping consistent")
        void crossRoomJoinIsAtomic() throws Exception {
            for (int iter = 0; iter < 50; iter++) {
                String p1 = uniqueId("p1");
                String ownerA = uniqueId("oa");
                String ownerB = uniqueId("ob");
                GameRoom.RoomSummary a = roomService.createRoom("A" + iter, mp2048(), ownerA, "OA");
                GameRoom.RoomSummary b = roomService.createRoom("B" + iter, mp2048(), ownerB, "OB");

                CountDownLatch start = new CountDownLatch(1);
                CyclicBarrier bothReady = new CyclicBarrier(2);
                Queue<Throwable> errors = new ConcurrentLinkedQueue<>();
                AtomicInteger okA = new AtomicInteger();
                AtomicInteger okB = new AtomicInteger();

                Thread tA = new Thread(() -> {
                    try {
                        start.await();
                        bothReady.await();   // both threads live and racing before the call
                        roomService.joinRoom(a.roomId(), p1, "P1", null);
                        okA.incrementAndGet();
                    } catch (InterruptedException | BrokenBarrierException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable t) {
                        errors.add(t);
                    }
                });
                Thread tB = new Thread(() -> {
                    try {
                        start.await();
                        bothReady.await();
                        roomService.joinRoom(b.roomId(), p1, "P1", null);
                        okB.incrementAndGet();
                    } catch (InterruptedException | BrokenBarrierException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable t) {
                        errors.add(t);
                    }
                });
                tA.start();
                tB.start();
                start.countDown();
                tA.join(10_000);
                tB.join(10_000);

                for (Throwable t : errors) {
                    assertThat(t)
                            .as("iter %d: unexpected exception type from joinRoom", iter)
                            .isInstanceOf(IllegalStateException.class);
                }

                boolean inA = requireRoom(a.roomId()).hasPlayer(p1);
                boolean inB = requireRoom(b.roomId()).hasPlayer(p1);

                assertThat(inA ^ inB)
                        .as("iter %d: p1 must be in exactly one of the two rooms (inA=%s, inB=%s)", iter, inA, inB)
                        .isTrue();

                String mapped = roomService.getRoomForPlayer(p1);
                assertThat(mapped)
                        .as("iter %d: playerToRoom must point at the winning room", iter)
                        .isEqualTo(inA ? a.roomId() : b.roomId());

                assertThat(okA.get() + okB.get())
                        .as("iter %d: at least one join must succeed", iter)
                        .isGreaterThanOrEqualTo(1);

                // cleanup for next iteration
                roomService.leaveRoom(p1);
                roomService.leaveRoom(ownerA);
                roomService.leaveRoom(ownerB);
            }
        }
    }

    @Nested
    @DisplayName("2. deleteRoom — playerToRoom corruption on migration")
    class Item02_DeleteRoomMigration {

        @Test
        @DisplayName("player who migrated to room B keeps mapping after room A is deleted")
        void migratedPlayerKeepsMapping() {
            String ownerA = uniqueId("oA");
            String p1 = uniqueId("mig-p1");
            GameRoom.RoomSummary a = roomService.createRoom("A", mp2048(), ownerA, "OA");
            GameRoom.RoomSummary b = roomService.createRoom("B", mp2048(), uniqueId("oB"), "OB");

            roomService.joinRoom(a.roomId(), p1, "P1", null);
            roomService.joinRoom(b.roomId(), p1, "P1", null);   // auto-leave from A

            roomService.deleteRoom(a.roomId(), ownerA);

            assertThat(roomService.getRoom(a.roomId())).isNotPresent();
            assertThat(roomService.getRoomForPlayer(p1)).isEqualTo(b.roomId());
            assertThat(requireRoom(b.roomId()).hasPlayer(p1)).isTrue();
        }

        @Test
        @DisplayName("spectator of the deleted room who plays elsewhere keeps their room mapping")
        void spectatorElsewhereKeepsMapping() {
            String p2 = uniqueId("p2");
            String ownerA = uniqueId("oA");
            GameRoom.RoomSummary b = roomService.createRoom("B", mp2048(), p2, "P2");
            GameRoom.RoomSummary a = roomService.createRoom("A", mp2048(), ownerA, "OA");

            roomService.joinAsSpectator(a.roomId(), p2, "P2");  // p2 is a PLAYER of B, SPECTATOR of A

            roomService.deleteRoom(a.roomId(), ownerA);

            assertThat(roomService.getRoomForPlayer(p2))
                    .as("deleting a room must not strip a player's mapping to a different room")
                    .isEqualTo(b.roomId());
            assertThat(requireRoom(b.roomId()).hasPlayer(p2)).isTrue();
        }
    }

    @Nested
    @DisplayName("3. cleanupInactiveRooms — re-population race")
    class Item03_CleanupRepopulation {

        @Test
        @DisplayName("empty room past EMPTY_ROOM_TTL is removed and its stale mappings stripped")
        void staleEmptyRoomRemoved() {
            String owner = uniqueId("stale-owner");
            GameRoom.RoomSummary s = roomService.createRoom("Stale", GameSettings.defaultFor(GameType.BLACKJACK), owner, "SO");
            GameRoom room = requireRoom(s.roomId());
            room.removePlayer(owner);   // empty the room without service-level removal
            setLastActivity(room, Instant.now().minusSeconds(6 * 60));   // past EMPTY_ROOM_TTL (5 min)

            roomService.cleanupInactiveRooms();

            assertThat(roomService.getRoom(s.roomId())).isNotPresent();
            assertThat(roomService.getRoomForPlayer(owner)).isNull();
        }

        @Test
        @DisplayName("player joining a stale room while cleanup runs never yields corrupted state")
        void joinDuringCleanupNeverCorrupts() throws Exception {
            for (int i = 0; i < 100; i++) {
                String owner = uniqueId("co");
                String joiner = uniqueId("cj");
                GameRoom.RoomSummary s = roomService.createRoom("C" + i, GameSettings.defaultFor(GameType.BLACKJACK), owner, "O");
                String roomId = s.roomId();
                GameRoom room = requireRoom(roomId);
                room.removePlayer(owner);
                setLastActivity(room, Instant.now().minusSeconds(6 * 60));

                CountDownLatch go = new CountDownLatch(1);
                AtomicReference<Throwable> cleanupError = new AtomicReference<>();
                Thread cleaner = new Thread(() -> {
                    try {
                        go.await();
                        roomService.cleanupInactiveRooms();
                    } catch (Throwable t) {
                        cleanupError.set(t);
                    }
                });
                cleaner.start();
                go.countDown();

                Throwable joinError = null;
                try {
                    roomService.joinRoom(roomId, joiner, "J", null);
                } catch (ResourceNotFoundException | IllegalStateException e) {
                    joinError = e;
                }
                cleaner.join(10_000);

                assertThat(cleanupError.get()).as("iter %d: cleanup must not throw", i).isNull();

                String mapped = roomService.getRoomForPlayer(joiner);
                boolean roomExists = roomService.getRoom(roomId).isPresent();
                if (mapped != null) {
                    assertThat(mapped).isEqualTo(roomId);
                    assertThat(roomExists)
                            .as("iter %d: mapping must not point at a removed room", i)
                            .isTrue();
                    assertThat(requireRoom(roomId).hasPlayer(joiner))
                            .as("iter %d: mapped room must contain the player", i)
                            .isTrue();
                } else {
                    assertThat(joinError)
                            .as("iter %d: no mapping means the join must have failed", i)
                            .isNotNull();
                }

                // cleanup for next iteration
                roomService.leaveRoom(joiner);
                roomService.leaveRoom(owner);
            }
        }
    }

    @Nested
    @DisplayName("4. tickRoomPhase — getters must be pure reads")
    class Item04_GettersArePureReads {

        @Test
        @DisplayName("getRoomProgress/getRoomState do not mutate an expired READY_CHECK phase")
        void gettersDoNotTickPhase() throws Exception {
            String owner = uniqueId("tick-owner");
            GameRoom.RoomSummary s = roomService.createRoom("Tick", mp2048(), owner, "TO");
            roomService.markPlayerReady(s.roomId(), owner);
            assertThat(requireRoom(s.roomId()).getPhase()).isEqualTo(GameRoom.RoomPhase.READY_CHECK);

            // Expire the ready-check window (READY_CHECK_DURATION_MS = 3000)
            Thread.sleep(3100);

            // Reads equivalent to GET /progress and GET /state must NOT flip the phase
            roomService.getRoomProgress(s.roomId());
            roomService.getRoomState(s.roomId());

            assertThat(requireRoom(s.roomId()).getPhase())
                    .as("polling reads must not mutate room phase (transition belongs to the scheduler)")
                    .isEqualTo(GameRoom.RoomPhase.READY_CHECK);
        }
    }

    @Nested
    @DisplayName("5. settleGame — deterministic tie-break")
    class Item05_DeterministicTieBreak {

        @Test
        @DisplayName("equal scores always settle to the lexicographically smaller playerId across 50 runs")
        void tieBreakIsDeterministic() {
            for (int i = 0; i < 50; i++) {
                String idA = uniqueId("tie");
                String idB = uniqueId("tie");
                while (idA.equals(idB)) {
                    idB = uniqueId("tie");
                }

                GameRoom.RoomSummary s = roomService.createRoom("Tie" + i, mp2048(), idA, "A");
                roomService.joinRoom(s.roomId(), idB, "B", null);

                String sessA = game2048.createSession().sessionId();
                String sessB = game2048.createSession().sessionId();
                roomService.registerPlayerSession(s.roomId(), idA, sessA);
                roomService.registerPlayerSession(s.roomId(), idB, sessB);

                roomService.finishRoom(s.roomId());

                String expected = idA.compareTo(idB) < 0 ? idA : idB;
                assertThat(requireRoom(s.roomId()).getWinnerId())
                        .as("run %d: tie must go to the lexicographically smaller playerId (%s vs %s)",
                                i, idA, idB)
                        .isEqualTo(expected);

                roomService.leaveRoom(idA);
                roomService.leaveRoom(idB);
            }
        }
    }

    @Nested
    @DisplayName("6. createRoom — single-player session failure must not leave a zombie room")
    class Item06_NoZombieRoom {

        @Test
        @DisplayName("failed session init throws, and no room or mapping is left behind")
        void sessionInitFailureRollsBack() {
            GameSettings broken = new GameSettings(GameType.BLACKJACK,
                    Map.of("difficulty", "INVALID_DIFFICULTY"),
                    false, null, false, 1, 0, true);
            String owner = uniqueId("zombie-owner");

            assertThatThrownBy(() -> roomService.createRoom("Zombie", broken, owner, "Z"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("session");

            assertThat(roomService.listAllRooms())
                    .as("no zombie room may remain")
                    .isEmpty();
            assertThat(roomService.getRoomForPlayer(owner))
                    .as("owner must not be mapped to a room that failed to initialize")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("7. lastActivity visibility")
    class Item07_LastActivityVisibility {

        @Test
        @DisplayName("reader thread eventually observes the writer's latest touch")
        void readerObservesLatestTouch() throws Exception {
            GameRoom.RoomSummary s = roomService.createRoom("Vis", mp2048(), uniqueId("vis-owner"), "VO");
            GameRoom room = requireRoom(s.roomId());

            AtomicReference<Instant> marker = new AtomicReference<>();
            AtomicBoolean observed = new AtomicBoolean(false);
            AtomicBoolean stop = new AtomicBoolean(false);

            Thread reader = new Thread(() -> {
                while (!stop.get()) {
                    Instant m = marker.get();
                    Instant seen = room.getLastActivity();
                    if (m != null && !seen.isBefore(m)) {
                        observed.set(true);
                        return;
                    }
                }
            });
            reader.start();

            long end = System.currentTimeMillis() + 200;
            while (System.currentTimeMillis() < end) {
                room.touch();
            }
            marker.set(Instant.now());
            room.touch();   // the write the reader must observe

            reader.join(3000);
            stop.set(true);
            reader.join(1000);

            assertThat(observed.get())
                    .as("reader must eventually observe the latest touch() write")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("9. Concurrent mixed-ops fuzz")
    class Item09_MixedOpsFuzz {

        private static boolean isBusinessMessage(String message) {
            if (message == null) return false;
            return message.startsWith("Room is full")
                    || message.startsWith("Room is not accepting players")
                    || message.equals("Failed to join room")
                    || message.equals("Player already joined another room")
                    || message.equals("Room is not in LOBBY phase")
                    || message.equals("Player not in room")
                    || message.startsWith("Duplicate request in progress");
        }

        @Test
        @DisplayName("50 threads x 5s mixed ops on 5 rooms: no NPE/CME/internal errors; full consistency afterwards")
        void mixedOpsFuzz() throws Exception {
            List<String> roomIds = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                roomIds.add(roomService.createRoom("Fuzz" + i, mp2048(), "fuzz-owner-" + i, "O" + i).roomId());
            }
            String[] players = new String[20];
            for (int i = 0; i < 20; i++) {
                players[i] = "fuzz-p" + i;
            }

            int nThreads = 50;
            ExecutorService pool = Executors.newFixedThreadPool(nThreads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(nThreads);
            ConcurrentLinkedQueue<String> unexpected = new ConcurrentLinkedQueue<>();
            long deadline = System.currentTimeMillis() + 5000;

            for (int t = 0; t < nThreads; t++) {
                final long seed = 42L + t;
                pool.submit(() -> {
                    Random r = new Random(seed);
                    try {
                        start.await();
                        while (System.currentTimeMillis() < deadline) {
                            try {
                                switch (r.nextInt(6)) {
                                    case 0 -> roomService.joinRoom(
                                            roomIds.get(r.nextInt(roomIds.size())),
                                            players[r.nextInt(players.length)], "F", null);
                                    case 1 -> roomService.leaveRoom(players[r.nextInt(players.length)]);
                                    case 2 -> {
                                        try {
                                            roomService.getRoomState(roomIds.get(r.nextInt(roomIds.size())));
                                        } catch (ResourceNotFoundException ignored) {
                                        }
                                    }
                                    case 3 -> {
                                        try {
                                            roomService.getRoomProgress(roomIds.get(r.nextInt(roomIds.size())));
                                        } catch (ResourceNotFoundException ignored) {
                                        }
                                    }
                                    case 4 -> roomService.markPlayerReady(
                                            roomIds.get(r.nextInt(roomIds.size())),
                                            players[r.nextInt(players.length)]);
                                    case 5 -> roomService.cleanupInactiveRooms();
                                }
                            } catch (ResourceNotFoundException | SecurityException expected) {
                                // business rule: room already gone
                            } catch (IllegalArgumentException business) {
                                // e.g. markPlayerReady: "Player not in room"
                            } catch (IllegalStateException business) {
                                if (!isBusinessMessage(business.getMessage())) {
                                    unexpected.add("IllegalStateException: " + business.getMessage());
                                }
                            } catch (Throwable fatal) {
                                unexpected.add(fatal.getClass().getName() + ": " + fatal.getMessage());
                            }
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
            pool.shutdown();

            assertThat(unexpected)
                    .as("no NPE / CME / internal IllegalStateException during the fuzz")
                    .isEmpty();

            // every mapping points at an existing room that contains the player
            for (String pid : players) {
                String rid = roomService.getRoomForPlayer(pid);
                if (rid != null) {
                    var room = roomService.getRoom(rid);
                    assertThat(room)
                            .as("no orphaned playerToRoom entry: %s -> %s", pid, rid)
                            .isPresent();
                    assertThat(room.get().hasPlayer(pid))
                            .as("mapped room %s must contain %s", rid, pid)
                            .isTrue();
                }
            }
            // every member of every remaining room maps back to that room
            for (GameRoom.RoomSummary summary : roomService.listAllRooms()) {
                GameRoom room = requireRoom(summary.roomId());
                for (String pid : room.getPlayerIds()) {
                    assertThat(roomService.getRoomForPlayer(pid))
                            .as("player %s in room %s must map back to it", pid, summary.roomId())
                            .isEqualTo(summary.roomId());
                }
            }
        }
    }

    @Nested
    @DisplayName("10. cleanupInactiveRooms - solo rooms expire after SOLO_ROOM_TTL (Step 6g D)")
    class Item10_SoloRoomTtl {

        private static GameSettings solo2048() {
            return new GameSettings(GameType.TWENTY_FORTY_EIGHT, Map.of("size", 4),
                    false, null, false, 1, 0, true);
        }

        @Test
        @DisplayName("solo room idle 11 minutes is swept even though it still holds its owner; idle multiplayer room survives")
        void soloRoomSweptAfterElevenMinutes_multiplayerSurvives() {
            String soloOwner = uniqueId("solo-ttl-owner");
            GameRoom.RoomSummary solo = roomService.createRoom("Solo Ghost", solo2048(), soloOwner, "S");
            String mpOwner = uniqueId("mp-ttl-owner");
            GameRoom.RoomSummary mp = roomService.createRoom("MP Room", mp2048(), mpOwner, "M");

            // both rooms idle 11 minutes - beyond the 10m solo TTL, far under the 2h room TTL
            setLastActivity(requireRoom(solo.roomId()), Instant.now().minus(Duration.ofMinutes(11)));
            setLastActivity(requireRoom(mp.roomId()), Instant.now().minus(Duration.ofMinutes(11)));

            roomService.cleanupInactiveRooms();

            assertThat(roomService.getRoom(solo.roomId()))
                    .as("a solo room idle 11 minutes must be swept (the ghost-room bug)")
                    .isNotPresent();
            assertThat(roomService.getRoomForPlayer(soloOwner))
                    .as("the swept room's owner mapping must be cleaned up too")
                    .isNull();
            assertThat(roomService.getRoom(mp.roomId()))
                    .as("a multiplayer room idle 11 minutes is under ROOM_TTL and must stay")
                    .isPresent();
            assertThat(roomService.getRoomForPlayer(mpOwner)).isEqualTo(mp.roomId());
        }

        @Test
        @DisplayName("solo room idle under 10 minutes survives the sweep")
        void soloRoomWithinTtlSurvives() {
            GameRoom.RoomSummary solo =
                    roomService.createRoom("Fresh Solo", solo2048(), uniqueId("solo-fresh"), "S");

            setLastActivity(requireRoom(solo.roomId()), Instant.now().minus(Duration.ofMinutes(9)));

            roomService.cleanupInactiveRooms();

            assertThat(roomService.getRoom(solo.roomId()))
                    .as("a solo room idle 9 minutes is under SOLO_ROOM_TTL and must stay")
                    .isPresent();
        }
    }
}
