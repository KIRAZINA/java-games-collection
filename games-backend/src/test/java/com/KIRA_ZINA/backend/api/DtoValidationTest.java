package com.KIRA_ZINA.backend.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Step 6b Part A - request DTO bean validation")
class DtoValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/rooms without ownerId -> 400 {error,status} naming ownerId (was 500 NPE)")
    void createRoom_ownerIdMissing_400() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"No Owner",
                                    "gameType":"BLACKJACK",
                                    "ownerName":"Alice"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error", containsString("ownerId: must not be blank")))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/rooms with blank ownerId -> 400 naming ownerId (was 201 with empty-key room)")
    void createRoom_ownerIdBlank_400() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"Blank Owner",
                                    "gameType":"BLACKJACK",
                                    "ownerId":"   ",
                                    "ownerName":"Alice"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error", containsString("ownerId: must not be blank")));
    }

    @Test
    @DisplayName("POST /api/rooms with blank roomName and null ownerName -> 400 naming both fields (was 201 silent)")
    void createRoom_roomNameAndOwnerNameInvalid_400() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"  ",
                                    "gameType":"MINESWEEPER",
                                    "ownerId":"dto-owner",
                                    "ownerName":null
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error", containsString("roomName: must not be blank")))
                .andExpect(jsonPath("$.error", containsString("ownerName: must not be blank")));
    }

    @Test
    @DisplayName("POST /api/rooms with null gameType (settings absent) -> 400 naming gameType (was 500 switch NPE)")
    void createRoom_gameTypeNull_400() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"No Type",
                                    "gameType":null,
                                    "ownerId":"dto-notype",
                                    "ownerName":"Alice"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error", containsString("gameType: must not be null")));
    }

    @Test
    @DisplayName("POST /api/rooms with settings.maxPlayers 0 -> 400 naming maxPlayers (cascade @Valid)")
    void createRoom_maxPlayersZero_400() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"Full House",
                                    "gameType":"BLACKJACK",
                                    "ownerId":"dto-cap",
                                    "ownerName":"Alice",
                                    "settings":{
                                        "gameType":"BLACKJACK",
                                        "settings":{},
                                        "passwordProtected":false,
                                        "passwordHash":null,
                                        "allowBots":false,
                                        "maxPlayers":0,
                                        "timeLimitSeconds":0,
                                        "isSinglePlayer":false
                                    }
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error", containsString("maxPlayers: must be greater than or equal to 1")));
    }

    @Test
    @DisplayName("POST /{roomId}/join with null playerId -> 400 naming playerId (was 500 NPE)")
    void joinRoom_playerIdNull_400() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"Join Validation",
                                    "gameType":"BLACKJACK",
                                    "ownerId":"dto-join-owner",
                                    "ownerName":"Alice"
                                }"""))
                .andExpect(status().isCreated())
                .andReturn();
        String roomId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("roomId").asText();

        mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "playerId":null,
                                    "playerName":"Joe"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error", containsString("playerId: must not be blank")));
    }
}
