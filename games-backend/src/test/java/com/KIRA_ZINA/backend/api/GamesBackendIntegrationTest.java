package com.KIRA_ZINA.backend.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.KIRA_ZINA.backend.blackjack.service.BlackjackSessionService;
import com.KIRA_ZINA.backend.common.GameRoomService;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.KIRA_ZINA.backend.blackjack.domain.BlackjackSession;
import com.KIRA_ZINA.backend.blackjack.domain.DealerDifficulty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for all three game REST controllers.
 * Verifies HTTP status codes (201, 200, 400, 204), response shapes,
 * error handling, and @Scheduled session eviction.
 */
@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Games Backend Integration Tests")
class GamesBackendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BlackjackSessionService blackjackSessionService;

    @Autowired
    private GameRoomService gameRoomService;

    // ============================================================ Blackjack

    @Nested
    @DisplayName("Blackjack Controller")
    class BlackjackController {

        @Test
        @DisplayName("POST /sessions → 201 CREATED with correct initial state")
        void createBlackjackSession_201() throws Exception {
            mockMvc.perform(post("/api/blackjack/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"initialBalance\":100,\"difficulty\":\"BASIC\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.balance").value(100.0))
                    .andExpect(jsonPath("$.phase").value("BETTING"))
                    .andExpect(jsonPath("$.winner").value("NONE"))
                    .andExpect(jsonPath("$.sessionId", notNullValue()));
        }

        @Test
        @DisplayName("POST /sessions with no body → 201 CREATED (uses defaults)")
        void createBlackjackSessionNoBody_201() throws Exception {
            mockMvc.perform(post("/api/blackjack/sessions"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.balance").value(100.0));
        }

        @Test
        @DisplayName("GET /{sessionId} → 200 OK with full state")
        void getBlackjackSession_200() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/blackjack/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"initialBalance\":200}"))
                    .andExpect(status().isCreated())
                    .andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(get("/api/blackjack/sessions/{id}", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.balance").value(200.0));
        }

        @Test
        @DisplayName("GET /nonexistent → 400 BAD REQUEST (session not found)")
        void getBlackjackSession_400_missing() throws Exception {
            mockMvc.perform(get("/api/blackjack/sessions/nonexistent-session-id"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not found")));
        }

        @Test
        @DisplayName("POST /bets with negative amount → 400 BAD REQUEST")
        void blackjackInvalidBet_400() throws Exception {
            MvcResult created = createBlackjackSession(100);
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/blackjack/sessions/{id}/rounds", sessionId))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/blackjack/sessions/{id}/bets", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":-1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Minimum bet")));
        }

        @Test
        @DisplayName("POST /bets with amount > MAX_BET → 400 BAD REQUEST")
        void blackjackBetOverMax_400() throws Exception {
            MvcResult created = createBlackjackSession(2000);
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/blackjack/sessions/{id}/rounds", sessionId));

            mockMvc.perform(post("/api/blackjack/sessions/{id}/bets", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":1001}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Maximum bet")));
        }

        @Test
        @DisplayName("POST /hit before bet placed → 409 CONFLICT (wrong phase)")
        void hitWhenNotPlayerTurn_400() throws Exception {
            MvcResult created = createBlackjackSession(100);
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/blackjack/sessions/{id}/rounds", sessionId));

            // Hit without placing a bet (still in BETTING phase)
            mockMvc.perform(post("/api/blackjack/sessions/{id}/hit", sessionId))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("POST /stand before bet placed → 409 CONFLICT (wrong phase)")
        void standWhenNotPlayerTurn_400() throws Exception {
            MvcResult created = createBlackjackSession(100);
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/blackjack/sessions/{id}/rounds", sessionId));

            mockMvc.perform(post("/api/blackjack/sessions/{id}/stand", sessionId))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("DELETE /{sessionId} → 204 NO CONTENT")
        void deleteBlackjackSession_204() throws Exception {
            MvcResult created = createBlackjackSession(100);
            String sessionId = sessionId(created);

            mockMvc.perform(delete("/api/blackjack/sessions/{id}", sessionId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Full lifecycle: create → start round → bet → settle → delete")
        void blackjackEndpointsCreatePlayAndCloseSession() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/blackjack/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"initialBalance\":100,\"difficulty\":\"BASIC\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.balance").value(100.0))
                    .andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/blackjack/sessions/{sessionId}/rounds", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phase").value("BETTING"));

            mockMvc.perform(post("/api/blackjack/sessions/{sessionId}/bets", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":10}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerCards", hasSize(2)));

            mockMvc.perform(delete("/api/blackjack/sessions/{sessionId}", sessionId))
                    .andExpect(status().isNoContent());
        }
    }

    // ============================================================ Minesweeper

    @Nested
    @DisplayName("Minesweeper Controller")
    class MinesweeperController {

        @Test
        @DisplayName("POST /sessions → 201 CREATED with correct cell count")
        void createMinesweeperSession_201() throws Exception {
            mockMvc.perform(post("/api/minesweeper/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rows\":9,\"cols\":9,\"mines\":10}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cells", hasSize(81)))
                    .andExpect(jsonPath("$.rows").value(9))
                    .andExpect(jsonPath("$.cols").value(9))
                    .andExpect(jsonPath("$.totalMines").value(10))
                    .andExpect(jsonPath("$.firstClickDone").value(false));
        }

        @Test
        @DisplayName("POST /sessions with no body → 201 CREATED (uses defaults 9×9, 10 mines)")
        void createMinesweeperSessionNoBody_201() throws Exception {
            mockMvc.perform(post("/api/minesweeper/sessions"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cells", hasSize(81)));
        }

        @Test
        @DisplayName("POST /sessions with rows=3 → 400 BAD REQUEST (invalid board)")
        void minesweeperInvalidBoard_400() throws Exception {
            mockMvc.perform(post("/api/minesweeper/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rows\":3,\"cols\":9,\"mines\":5}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("between 4 and 30")));
        }

        @Test
        @DisplayName("GET /{sessionId} → 200 OK")
        void getMinesweeperSession_200() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/minesweeper/sessions"))
                    .andExpect(status().isCreated()).andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(get("/api/minesweeper/sessions/{id}", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId));
        }

        @Test
        @DisplayName("POST /{sessionId}/open → 200 OK, firstClickDone=true")
        void minesweeperOpenCell_200() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/minesweeper/sessions"))
                    .andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/minesweeper/sessions/{id}/open", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"row\":4,\"col\":4}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstClickDone").value(true));
        }

        @Test
        @DisplayName("POST /{sessionId}/flag → 200 OK, flagsPlaced increments")
        void minesweeperFlagCell_200() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/minesweeper/sessions"))
                    .andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/minesweeper/sessions/{id}/flag", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"row\":0,\"col\":0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.flagsPlaced").value(1));
        }

        @Test
        @DisplayName("POST /{sessionId}/reset → 200 OK, firstClickDone=false")
        void minesweeperReset_200() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/minesweeper/sessions"))
                    .andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/minesweeper/sessions/{id}/open", sessionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"row\":4,\"col\":4}"));

            mockMvc.perform(post("/api/minesweeper/sessions/{id}/reset", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstClickDone").value(false));
        }

        @Test
        @DisplayName("Full lifecycle: create → open → flag → delete")
        void minesweeperEndpointsCreateOpenFlagAndCloseSession() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/minesweeper/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rows\":9,\"cols\":9,\"mines\":10}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cells", hasSize(81)))
                    .andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/minesweeper/sessions/{sessionId}/open", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"row\":4,\"col\":4}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstClickDone").value(true));

            mockMvc.perform(post("/api/minesweeper/sessions/{sessionId}/flag", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"row\":0,\"col\":0}"))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/api/minesweeper/sessions/{sessionId}", sessionId))
                    .andExpect(status().isNoContent());
        }
    }

    // ============================================================ 2048

    @Nested
    @DisplayName("Game2048 Controller")
    class Game2048Controller {

        @Test
        @DisplayName("POST /sessions → 201 CREATED with 2 tiles and score=0")
        void create2048Session_201() throws Exception {
            mockMvc.perform(post("/api/2048/sessions"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tiles", hasSize(2)))
                    .andExpect(jsonPath("$.score").value(0))
                    .andExpect(jsonPath("$.gameOver").value(false))
                    .andExpect(jsonPath("$.size").value(4));
        }

        @Test
        @DisplayName("GET /{sessionId} → 200 OK")
        void get2048Session_200() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/2048/sessions"))
                    .andExpect(status().isCreated()).andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(get("/api/2048/sessions/{id}", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId));
        }

        @Test
        @DisplayName("POST /{sessionId}/moves with valid direction → 200 OK")
        void game2048Move_200() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/2048/sessions"))
                    .andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/2048/sessions/{id}/moves", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"direction\":\"LEFT\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(4));
        }

        @Test
        @DisplayName("POST /{sessionId}/moves with null direction → 400 BAD REQUEST")
        void game2048NullDirection_400() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/2048/sessions"))
                    .andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/2048/sessions/{id}/moves", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"direction\":null}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /{sessionId}/moves with invalid direction string → 400 BAD REQUEST")
        void game2048InvalidDirection_400() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/2048/sessions"))
                    .andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/2048/sessions/{id}/moves", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"direction\":\"DIAGONAL\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /{sessionId}/reset → 200 OK with 2 fresh tiles")
        void game2048Reset_200() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/2048/sessions"))
                    .andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/2048/sessions/{id}/reset", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tiles", hasSize(2)))
                    .andExpect(jsonPath("$.score").value(0));
        }

        @Test
        @DisplayName("Full lifecycle: create → move → reset → delete")
        void game2048EndpointsCreateMoveResetAndCloseSession() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/2048/sessions"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tiles", hasSize(2)))
                    .andReturn();
            String sessionId = sessionId(created);

            mockMvc.perform(post("/api/2048/sessions/{sessionId}/moves", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"direction\":\"LEFT\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(4));

            mockMvc.perform(post("/api/2048/sessions/{sessionId}/reset", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tiles", hasSize(2)));

            mockMvc.perform(delete("/api/2048/sessions/{sessionId}", sessionId))
                    .andExpect(status().isNoContent());
        }
    }

    // ============================================================ Session TTL Eviction

    @Nested
    @DisplayName("Session TTL Eviction (@Scheduled)")
    class SessionEviction {

        @Test
        @DisplayName("evictInactiveSessions removes sessions past their TTL")
        void evictInactiveBlackjackSessions() throws Exception {
            // Create a session through the API
            MvcResult created = createBlackjackSession(100);
            String sessionId = sessionId(created);

            // Manipulate lastTouched to be far in the past via reflection
            // This simulates a session that has been idle for > 30 minutes
            BlackjackSession session = getSessionFromService(sessionId);
            if (session != null) {
                setLastTouched(session, Instant.now().minusSeconds(31 * 60));
            }

            // Trigger the cleanup
            blackjackSessionService.evictInactiveSessions();

            // Session should now be gone → GET returns 400
            mockMvc.perform(get("/api/blackjack/sessions/{id}", sessionId))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================ Rooms

    @Nested
    @DisplayName("Rooms Controller")
    class RoomsController {

        @Test
        @DisplayName("POST /api/rooms → 201 CREATED")
        void createRoom_201() throws Exception {
            mockMvc.perform(post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "roomName":"Test Room",
                                        "gameType":"BLACKJACK",
                                        "ownerId":"owner1",
                                        "ownerName":"Alice"
                                    }"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.roomId", notNullValue()))
                    .andExpect(jsonPath("$.roomName").value("Test Room"))
                    .andExpect(jsonPath("$.gameType").value("BLACKJACK"))
                    .andExpect(jsonPath("$.phase").value("LOBBY"))
                    .andExpect(jsonPath("$.playerCount").value(1));
        }

        @Test
        @DisplayName("GET /api/rooms?type=BLACKJACK → 200 OK with created room")
        void listRoomsByType_200() throws Exception {
            mockMvc.perform(post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "roomName":"BJ Room",
                                        "gameType":"BLACKJACK",
                                        "ownerId":"owner2",
                                        "ownerName":"Bob"
                                    }"""))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/rooms?type=BLACKJACK"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].roomName").value("BJ Room"));
        }

        @Test
        @DisplayName("GET /api/rooms/{roomId} → 200 OK with room details")
        void getRoom_200() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "roomName":"Get Room",
                                        "gameType":"MINESWEEPER",
                                        "ownerId":"owner3",
                                        "ownerName":"Carol"
                                    }"""))
                    .andExpect(status().isCreated()).andReturn();
            String roomId = roomSessionId(created);

            mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomId").value(roomId))
                    .andExpect(jsonPath("$.roomName").value("Get Room"))
                    .andExpect(jsonPath("$.gameType").value("MINESWEEPER"));
        }

        @Test
        @DisplayName("POST /{roomId}/join → 200 OK, playerCount increments")
        void joinRoom_200() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "roomName":"Join Room",
                                        "gameType":"BLACKJACK",
                                        "ownerId":"owner4",
                                        "ownerName":"Dave"
                                    }"""))
                    .andExpect(status().isCreated()).andReturn();
            String roomId = roomSessionId(created);

            mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId":"player5",
                                        "playerName":"Eve"
                                    }"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerCount").value(2));
        }

        @Test
        @DisplayName("DELETE /{roomId}/leave → 204 NO CONTENT, playerCount decrements")
        void leaveRoom_204() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "roomName":"Leave Room",
                                        "gameType":"TWENTY_FORTY_EIGHT",
                                        "ownerId":"owner6",
                                        "ownerName":"Frank"
                                    }"""))
                    .andExpect(status().isCreated()).andReturn();
            String roomId = roomSessionId(created);

            // Owner leaves — room has 0 players and gets removed immediately
            mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId":"owner6"
                                    }"""))
                    .andExpect(status().isNoContent());

            // Room should be gone
            mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Full room lifecycle: create → join → get → leave → cleanup")
        void roomFullLifecycle() throws Exception {
            // Create
            MvcResult created = mockMvc.perform(post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "roomName":"Lifecycle",
                                        "gameType":"BLACKJACK",
                                        "ownerId":"lifecycle-owner",
                                        "ownerName":"Grace"
                                    }"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.playerCount").value(1))
                    .andReturn();
            String roomId = roomSessionId(created);

            // Join
            mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId":"lifecycle-player",
                                        "playerName":"Heidi"
                                    }"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerCount").value(2));

            // Get — confirm room exists with 2 players
            mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerCount").value(2));

            // Join player leaves — 1 remaining
            mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId":"lifecycle-player"
                                    }"""))
                    .andExpect(status().isNoContent());

            // Room still exists (owner still there)
            mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerCount").value(1));

            // Owner leaves — room removed
            mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId":"lifecycle-owner"
                                    }"""))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================ Multi-Player Integration

    @Nested
    @DisplayName("Multi-Player Integration")
    class MultiPlayerIntegration {

        @Test
        @DisplayName("Room lifecycle: create (multiplayer) → join (max capacity) → leave → cleanup")
        void multiplayerRoomCreationAndJoining() throws Exception {
            // Player 1 creates a Minesweeper room (multiplayer, max 2 players, 120s time limit)
            MvcResult created = mockMvc.perform(post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "roomName": "Multiplayer Minesweeper",
                                        "gameType": "MINESWEEPER",
                                        "ownerId": "multi-owner",
                                        "ownerName": "Alice",
                                        "settings": {
                                            "gameType": "MINESWEEPER",
                                            "settings": {"rows": 9, "cols": 9, "mines": 10},
                                            "passwordProtected": false,
                                            "passwordHash": "",
                                            "allowBots": false,
                                            "maxPlayers": 2,
                                            "timeLimitSeconds": 120,
                                            "isSinglePlayer": false
                                        }
                                    }"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.roomId", notNullValue()))
                    .andExpect(jsonPath("$.playerCount").value(1))
                    .andExpect(jsonPath("$.phase").value("LOBBY"))
                    .andReturn();
            String roomId = roomSessionId(created);

            // Player 2 joins
            mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId": "multi-joiner",
                                        "playerName": "Bob"
                                    }"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerCount").value(2));

            // GET room confirms both players
            mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerCount").value(2));

            // Player 3 tries to join (room full) → 409 CONFLICT
            mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId": "multi-rejected",
                                        "playerName": "Charlie"
                                    }"""))
                    .andExpect(status().isConflict());

            // Player 2 leaves
            mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId": "multi-joiner"
                                    }"""))
                    .andExpect(status().isNoContent());

            // GET room shows 1 player remaining
            mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerCount").value(1));

            // Owner leaves → room removed
            mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId": "multi-owner"
                                    }"""))
                    .andExpect(status().isNoContent());

            // Room is gone
            mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Two players share a Minesweeper room; one makes a move, progress reflects both")
        void multiplayerMinesweeperInteraction() throws Exception {
            // Player 1 creates a Minesweeper session
            MvcResult session1Created = mockMvc.perform(post("/api/minesweeper/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rows\":9,\"cols\":9,\"mines\":10}"))
                    .andExpect(status().isCreated())
                    .andReturn();
            String sessionId1 = sessionId(session1Created);

            // Player 1 creates a multiplayer Minesweeper room
            MvcResult roomCreated = mockMvc.perform(post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "roomName": "MS Interaction",
                                        "gameType": "MINESWEEPER",
                                        "ownerId": "interact-owner",
                                        "ownerName": "Alice",
                                        "settings": {
                                            "gameType": "MINESWEEPER",
                                            "settings": {"rows": 9, "cols": 9, "mines": 10},
                                            "passwordProtected": false,
                                            "passwordHash": "",
                                            "allowBots": false,
                                            "maxPlayers": 2,
                                            "timeLimitSeconds": 120,
                                            "isSinglePlayer": false
                                        }
                                    }"""))
                    .andExpect(status().isCreated())
                    .andReturn();
            String roomId = roomSessionId(roomCreated);

            // Player 1 registers session with room
            mockMvc.perform(post("/api/rooms/{roomId}/sessions", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId": "interact-owner",
                                        "sessionId": "%s"
                                    }""".formatted(sessionId1)))
                    .andExpect(status().isCreated());

            // Player 2 joins
            mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId": "interact-joiner",
                                        "playerName": "Bob"
                                    }"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerCount").value(2));

            // Player 2 creates and registers their session
            MvcResult session2Created = mockMvc.perform(post("/api/minesweeper/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rows\":9,\"cols\":9,\"mines\":10}"))
                    .andExpect(status().isCreated())
                    .andReturn();
            String sessionId2 = sessionId(session2Created);

            mockMvc.perform(post("/api/rooms/{roomId}/sessions", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId": "interact-joiner",
                                        "sessionId": "%s"
                                    }""".formatted(sessionId2)))
                    .andExpect(status().isCreated());

            // Both players now have registered sessions — progress shows 2 entries
            mockMvc.perform(get("/api/rooms/{roomId}/progress", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomPhase").value("LOBBY"))
                    .andExpect(jsonPath("$.players", hasSize(2)));

            // Player 1 opens a cell
            mockMvc.perform(post("/api/minesweeper/sessions/{id}/open", sessionId1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"row\":4,\"col\":4}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstClickDone").value(true));

            // Cleanup: leave players in reverse order, delete sessions
            mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId": "interact-joiner"
                                    }"""))
                    .andExpect(status().isNoContent());

            mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "playerId": "interact-owner"
                                    }"""))
                    .andExpect(status().isNoContent());

            mockMvc.perform(delete("/api/minesweeper/sessions/{id}", sessionId1))
                    .andExpect(status().isNoContent());

            mockMvc.perform(delete("/api/minesweeper/sessions/{id}", sessionId2))
                    .andExpect(status().isNoContent());
        }

        @AfterEach
        void cleanupGameRoomServiceState() throws Exception {
            // Use reflection to clear the internal maps of GameRoomService.
            // This prevents leftover state from polluting other tests.
            Field roomsField = GameRoomService.class.getDeclaredField("rooms");
            roomsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> rooms = (Map<String, ?>) roomsField.get(gameRoomService);
            rooms.clear();

            Field playerToRoomField = GameRoomService.class.getDeclaredField("playerToRoom");
            playerToRoomField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> playerToRoom = (Map<String, ?>) playerToRoomField.get(gameRoomService);
            playerToRoom.clear();

            Field roomPlayerSessionsField = GameRoomService.class.getDeclaredField("roomPlayerSessions");
            roomPlayerSessionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> roomPlayerSessions = (Map<String, ?>) roomPlayerSessionsField.get(gameRoomService);
            roomPlayerSessions.clear();
        }
    }

    // ============================================================ Helpers

    private MvcResult createBlackjackSession(int balance) throws Exception {
        return mockMvc.perform(post("/api/blackjack/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\":" + balance + "}"))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String sessionId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("sessionId").asText();
    }

    private String roomSessionId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("roomId").asText();
    }

    @SuppressWarnings("unchecked")
    private BlackjackSession getSessionFromService(String sessionId) throws Exception {
        Field sessionsField = BlackjackSessionService.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Map<String, BlackjackSession> sessions =
                (Map<String, BlackjackSession>) sessionsField.get(blackjackSessionService);
        return sessions.get(sessionId);
    }

    private void setLastTouched(BlackjackSession session, Instant time) throws Exception {
        Field lastTouchedField = BlackjackSession.class.getDeclaredField("lastTouched");
        lastTouchedField.setAccessible(true);
        lastTouchedField.set(session, time);
    }
}
