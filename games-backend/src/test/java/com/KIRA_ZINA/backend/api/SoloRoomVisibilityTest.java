package com.KIRA_ZINA.backend.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Step 6g C: solo (practice) rooms must not appear in the public room list,
 * but must stay reachable by their owner.
 */
@DisplayName("Step 6g C - solo rooms stay out of the public room list")
@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = true)
class SoloRoomVisibilityTest {

    @Autowired
    private MockMvc mockMvc;

    private static final ObjectMapper JSON = new ObjectMapper();

    private static String roomId(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString()).get("roomId").asText();
    }

    private static String playerToken(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString()).get("playerToken").asText();
    }

    private static List<String> roomNames(String body) throws Exception {
        List<String> names = new ArrayList<>();
        for (JsonNode node : JSON.readTree(body)) {
            names.add(node.get("roomName").asText());
        }
        return names;
    }

    @Test
    @DisplayName("GET /api/rooms hides the solo room, keeps the multiplayer one")
    void soloRoomAbsentFromPublicList() throws Exception {
        MvcResult solo = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"Solo Practice",
                                    "gameType":"MINESWEEPER",
                                    "settings":{
                                        "gameType":"MINESWEEPER",
                                        "settings":{"rows":9,"cols":9,"mines":10},
                                        "passwordProtected":false,
                                        "passwordHash":null,
                                        "allowBots":false,
                                        "maxPlayers":1,
                                        "timeLimitSeconds":60,
                                        "isSinglePlayer":true
                                    },
                                    "ownerId":"solo-owner-c",
                                    "ownerName":"Solo Owner"
                                }"""))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult multiplayer = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"Public Match",
                                    "gameType":"MINESWEEPER",
                                    "settings":{
                                        "gameType":"MINESWEEPER",
                                        "settings":{"rows":9,"cols":9,"mines":10},
                                        "passwordProtected":false,
                                        "passwordHash":null,
                                        "allowBots":false,
                                        "maxPlayers":2,
                                        "timeLimitSeconds":60,
                                        "isSinglePlayer":false
                                    },
                                    "ownerId":"mp-owner-c",
                                    "ownerName":"MP Owner"
                                }"""))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult all = mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andReturn();
        List<String> names = roomNames(all.getResponse().getContentAsString());
        assertThat(names).as("public list must not expose the solo room").doesNotContain("Solo Practice");
        assertThat(names).as("the multiplayer room must still be listed").contains("Public Match");

        MvcResult byType = mockMvc.perform(get("/api/rooms").param("type", "MINESWEEPER"))
                .andExpect(status().isOk())
                .andReturn();
        List<String> typedNames = roomNames(byType.getResponse().getContentAsString());
        assertThat(typedNames).doesNotContain("Solo Practice");
        assertThat(typedNames).contains("Public Match");
    }

    @Test
    @DisplayName("the solo room still exists for its owner: GET /{roomId} and /player/{playerId}")
    void soloRoomStillReachableByOwner() throws Exception {
        MvcResult solo = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"Owner Solo",
                                    "gameType":"BLACKJACK",
                                    "settings":{
                                        "gameType":"BLACKJACK",
                                        "settings":{"initialBalance":100,"difficulty":"BASIC"},
                                        "passwordProtected":false,
                                        "passwordHash":null,
                                        "allowBots":false,
                                        "maxPlayers":1,
                                        "timeLimitSeconds":0,
                                        "isSinglePlayer":true
                                    },
                                    "ownerId":"solo-owner-c2",
                                    "ownerName":"Solo Owner"
                                }"""))
                .andExpect(status().isCreated())
                .andReturn();
        String roomId = roomId(solo);
        String token = playerToken(solo);

        // direct lookup still works - the room was NOT removed, just unlisted
        mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                .andExpect(status().isOk());

        // the owner-scoped list still contains it
        mockMvc.perform(get("/api/rooms/player/{playerId}", "solo-owner-c2")
                        .header("X-Player-Token", token))
                .andExpect(status().isOk());
    }
}
