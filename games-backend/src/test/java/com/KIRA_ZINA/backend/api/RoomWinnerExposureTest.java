package com.KIRA_ZINA.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.KIRA_ZINA.backend.common.GameRoom;
import com.KIRA_ZINA.backend.common.GameRoomService;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Step 6b.3: settleGame() has always computed winnerId/winnerScore on
 * GameRoom (GameRoomService:370-401), but no response DTO carried them, so
 * STEP_6b.md:86 ("verify both pages see GAME_OVER and a winner") was
 * unassertable at the HTTP level and the UI could not show a winner.
 * GET /{roomId}/state is the endpoint both games already poll, so that is
 * where the settled result is exposed.
 */
@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Room winner exposure on GET /{roomId}/state (Step 6b.3)")
class RoomWinnerExposureTest {

    private static final String SESSIONS_PATH = "/api/rooms/%s/sessions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GameRoomService gameRoomService;

    @AfterEach
    void cleanupRooms() throws Exception {
        TestUtils.resetGameRooms(gameRoomService);
    }

    @Test
    @DisplayName("1. Settled room: GET /state reports GAME_OVER with a non-null winnerId and numeric winnerScore")
    void settledRoomExposesWinnerOnState() throws Exception {
        // Multiplayer room with a 1s limit: it is created in LOBBY, so this
        // test drives the same path a browser does (register -> ready ->
        // READY_CHECK -> PLAYING -> settle). Note createRoom forces
        // timeLimitSeconds=0 for single-player rooms (GameRoomService:71), so
        // a solo room can never settle on the timer.
        MvcResult created = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"Winner Exposure Room",
                                    "gameType":"TWENTY_FORTY_EIGHT",
                                    "settings":{
                                        "gameType":"TWENTY_FORTY_EIGHT",
                                        "settings":{"size":4},
                                        "maxPlayers":2,
                                        "timeLimitSeconds":1,
                                        "isSinglePlayer":false
                                    },
                                    "ownerId":"winner-owner",
                                    "ownerName":"Winner Owner"
                                }"""))
                .andExpect(status().isCreated())
                .andReturn();
        String roomId = text(created, "roomId");
        String token = text(created, "playerToken");
        assertThat(token).isNotBlank();

        // A real 2048 session so settleGame has an actual score to read
        // (a fake sessionId would still settle, but with score -1).
        MvcResult session = mockMvc.perform(post("/api/2048/sessions"))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = text(session, "sessionId");

        // No phase check on registration, so this lands while still in LOBBY
        // and cannot lose the race against the settling tick.
        mockMvc.perform(post(String.format(SESSIONS_PATH, roomId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Player-Token", token)
                        .content("""
                                {"playerId":"winner-owner","sessionId":"%s"}""".formatted(sessionId)))
                .andExpect(status().isCreated());

        // Owner alone satisfies allPlayersReady() -> READY_CHECK (3s) ->
        // PLAYING -> settle after the 1s limit.
        mockMvc.perform(post("/api/rooms/{roomId}/ready", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Player-Token", token)
                        .content("""
                                {"playerId":"winner-owner"}"""))
                .andExpect(status().isOk());

        // No polling from this assertion: wait for the scheduler-driven settle
        Awaitility.await()
                .atMost(Duration.ofSeconds(12))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> gameRoomService.getRoom(roomId)
                        .map(r -> r.getPhase() == GameRoom.RoomPhase.GAME_OVER)
                        .orElse(false));

        mockMvc.perform(get("/api/rooms/{roomId}/state", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomPhase").value("GAME_OVER"))
                .andExpect(jsonPath("$.winnerId").value("winner-owner"))
                .andExpect(jsonPath("$.winnerScore").isNumber())
                .andExpect(jsonPath("$.winnerScore").value(0));

        // The name the UI shows next to the winner comes from the same payload
        mockMvc.perform(get("/api/rooms/{roomId}/state", roomId))
                .andExpect(jsonPath("$.players[0].playerId").value("winner-owner"))
                .andExpect(jsonPath("$.players[0].playerName").value("Winner Owner"));
    }

    @Test
    @DisplayName("2. Unsettled room: winnerId/winnerScore are null - no phantom winner before GAME_OVER")
    void unsettledRoomReportsNoWinner() throws Exception {
        // Multiplayer 2048 room with a 60s limit stays in LOBBY for the whole
        // test, so nothing can settle: both fields must read as JSON null.
        MvcResult created = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"Not Settled Room",
                                    "gameType":"TWENTY_FORTY_EIGHT",
                                    "settings":{
                                        "gameType":"TWENTY_FORTY_EIGHT",
                                        "settings":{"size":4},
                                        "maxPlayers":2,
                                        "timeLimitSeconds":60,
                                        "isSinglePlayer":false
                                    },
                                    "ownerId":"pending-owner",
                                    "ownerName":"Pending Owner"
                                }"""))
                .andExpect(status().isCreated())
                .andReturn();
        String roomId = text(created, "roomId");

        mockMvc.perform(get("/api/rooms/{roomId}/state", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomPhase").value("LOBBY"))
                .andExpect(jsonPath("$.winnerId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.winnerScore").value(org.hamcrest.Matchers.nullValue()));
    }

    private String text(MvcResult result, String field) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }
}
