package com.KIRA_ZINA.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.KIRA_ZINA.backend.common.GameRoomService;
import org.junit.jupiter.api.AfterEach;
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
@DisplayName("Player Token — per-room authorization (Step 3b)")
class PlayerTokenTest {

    private static final String TOKEN_HEADER = "X-Player-Token";

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

    // ============================================================ Issue: token returned on create/join/spectate

    @Test
    @DisplayName("1. POST /api/rooms → 201 with a non-empty playerToken")
    void createRoomIssuesPlayerToken() throws Exception {
        MvcResult created = createRoom("issue-owner", "Issue Owner", "Issue Create");
        assertThat(playerToken(created)).isNotBlank();
    }

    @Test
    @DisplayName("2. POST /{roomId}/join → 200 with a playerToken different from the owner's")
    void joinRoomIssuesDistinctPlayerToken() throws Exception {
        MvcResult created = createRoom("issue-owner-2", "Owner Two", "Issue Join");
        String roomId = roomId(created);
        String ownerToken = playerToken(created);

        MvcResult joined = mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"issue-joiner","playerName":"Joiner"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerToken").isNotEmpty())
                .andReturn();
        assertThat(playerToken(joined)).isNotEqualTo(ownerToken).isNotEqualTo("");
    }

    @Test
    @DisplayName("3. POST /{roomId}/spectate → 200 with a non-empty playerToken")
    void spectateIssuesPlayerToken() throws Exception {
        String roomId = roomId(createRoom("issue-owner-3", "Owner Three", "Issue Spectate"));
        mockMvc.perform(post("/api/rooms/{roomId}/spectate", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spectatorId":"issue-spec","spectatorName":"Spec"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerToken").isNotEmpty());
    }

    @Test
    @DisplayName("4. Re-issuing to an already-tokened member returns the same token (idempotent)")
    void repeatedSpectateReturnsSameToken() throws Exception {
        String roomId = roomId(createRoom("issue-owner-4", "Owner Four", "Issue Idem"));
        MvcResult first = mockMvc.perform(post("/api/rooms/{roomId}/spectate", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spectatorId":"issue-spec-2","spectatorName":"Spec2"}"""))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult second = mockMvc.perform(post("/api/rooms/{roomId}/spectate", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spectatorId":"issue-spec-2","spectatorName":"Spec2"}"""))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(playerToken(second)).isEqualTo(playerToken(first)).isNotEqualTo("");
    }

    // ============================================================ Per-endpoint: DELETE /{roomId}/leave

    @Test
    @DisplayName("5. DELETE /{roomId}/leave without token → 403 with {error,status} body")
    void leaveWithoutToken403() throws Exception {
        String roomId = roomId(createRoom("guard-5", "Guard Five", "Guard Leave"));
        mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"guard-5"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("6. DELETE /{roomId}/leave with wrong token → 403, body identical to missing-token body")
    void leaveWithWrongToken403() throws Exception {
        String roomId = roomId(createRoom("guard-6", "Guard Six", "Guard Leave Wrong"));
        MvcResult missing = mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"guard-6"}"""))
                .andExpect(status().isForbidden())
                .andReturn();
        MvcResult wrong = mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, "not-the-right-token")
                        .content("""
                                {"playerId":"guard-6"}"""))
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(wrong.getResponse().getContentAsString())
                .isEqualTo(missing.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("7. DELETE /{roomId}/leave with valid token → 204")
    void leaveWithValidToken204() throws Exception {
        MvcResult created = createRoom("guard-7", "Guard Seven", "Guard Leave Ok");
        String roomId = roomId(created);
        mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, playerToken(created))
                        .content("""
                                {"playerId":"guard-7"}"""))
                .andExpect(status().isNoContent());
    }

    // ============================================================ Per-endpoint: POST /{roomId}/ready

    @Test
    @DisplayName("8. POST /{roomId}/ready without token → 403; unknown room → 404")
    void readyWithoutToken403() throws Exception {
        String roomId = roomId(createRoom("guard-8", "Guard Eight", "Guard Ready"));
        mockMvc.perform(post("/api/rooms/{roomId}/ready", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"guard-8"}"""))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/rooms/{roomId}/ready", "no-such-room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"guard-8"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("9. POST /{roomId}/ready with wrong token → 403")
    void readyWithWrongToken403() throws Exception {
        String roomId = roomId(createRoom("guard-9", "Guard Nine", "Guard Ready Wrong"));
        mockMvc.perform(post("/api/rooms/{roomId}/ready", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, "wrong-token")
                        .content("""
                                {"playerId":"guard-9"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. POST /{roomId}/ready with valid token → 200")
    void readyWithValidToken200() throws Exception {
        MvcResult created = createRoom("guard-10", "Guard Ten", "Guard Ready Ok");
        String roomId = roomId(created);
        mockMvc.perform(post("/api/rooms/{roomId}/ready", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, playerToken(created))
                        .content("""
                                {"playerId":"guard-10"}"""))
                .andExpect(status().isOk());
    }

    // ============================================================ Per-endpoint: POST /{roomId}/sessions

    @Test
    @DisplayName("11. POST /{roomId}/sessions without token → 403")
    void registerSessionWithoutToken403() throws Exception {
        String roomId = roomId(createRoom("guard-11", "Guard Eleven", "Guard Sessions"));
        mockMvc.perform(post("/api/rooms/{roomId}/sessions", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"guard-11","sessionId":"s-11"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. POST /{roomId}/sessions with wrong token → 403")
    void registerSessionWithWrongToken403() throws Exception {
        String roomId = roomId(createRoom("guard-12", "Guard Twelve", "Guard Sessions Wrong"));
        mockMvc.perform(post("/api/rooms/{roomId}/sessions", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, "wrong-token")
                        .content("""
                                {"playerId":"guard-12","sessionId":"s-12"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("13. POST /{roomId}/sessions with valid token → 201")
    void registerSessionWithValidToken201() throws Exception {
        MvcResult created = createRoom("guard-13", "Guard Thirteen", "Guard Sessions Ok");
        String roomId = roomId(created);
        mockMvc.perform(post("/api/rooms/{roomId}/sessions", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, playerToken(created))
                        .content("""
                                {"playerId":"guard-13","sessionId":"s-13"}"""))
                .andExpect(status().isCreated());
    }

    // ============================================================ Per-endpoint: DELETE /{roomId}

    @Test
    @DisplayName("14. DELETE /{roomId} without token → 403")
    void deleteRoomWithoutToken403() throws Exception {
        String roomId = roomId(createRoom("guard-14", "Guard Fourteen", "Guard Delete"));
        mockMvc.perform(delete("/api/rooms/{roomId}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requesterId":"guard-14"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15. DELETE /{roomId} with wrong token → 403")
    void deleteRoomWithWrongToken403() throws Exception {
        String roomId = roomId(createRoom("guard-15", "Guard Fifteen", "Guard Delete Wrong"));
        mockMvc.perform(delete("/api/rooms/{roomId}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, "wrong-token")
                        .content("""
                                {"requesterId":"guard-15"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("16. DELETE /{roomId} with valid token (owner) → 204")
    void deleteRoomWithValidToken204() throws Exception {
        MvcResult created = createRoom("guard-16", "Guard Sixteen", "Guard Delete Ok");
        String roomId = roomId(created);
        mockMvc.perform(delete("/api/rooms/{roomId}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, playerToken(created))
                        .content("""
                                {"requesterId":"guard-16"}"""))
                .andExpect(status().isNoContent());
    }

    // ============================================================ Per-endpoint: GET /player/{playerId}

    @Test
    @DisplayName("17. GET /api/rooms/player/{playerId} → 403 missing/empty/wrong token (identical bodies), 200 valid")
    void getRoomsForPlayerRequiresValidToken() throws Exception {
        MvcResult created = createRoom("guard-17", "Guard Seventeen", "Guard Player List");
        String roomId = roomId(created);
        String validToken = playerToken(created);

        MvcResult missing = mockMvc.perform(get("/api/rooms/player/{playerId}", "guard-17"))
                .andExpect(status().isForbidden())
                .andReturn();
        MvcResult empty = mockMvc.perform(get("/api/rooms/player/{playerId}", "guard-17")
                        .header(TOKEN_HEADER, ""))
                .andExpect(status().isForbidden())
                .andReturn();
        MvcResult wrong = mockMvc.perform(get("/api/rooms/player/{playerId}", "guard-17")
                        .header(TOKEN_HEADER, "wrong-token"))
                .andExpect(status().isForbidden())
                .andReturn();

        String missingBody = missing.getResponse().getContentAsString();
        assertThat(empty.getResponse().getContentAsString()).isEqualTo(missingBody);
        assertThat(wrong.getResponse().getContentAsString()).isEqualTo(missingBody);

        mockMvc.perform(get("/api/rooms/player/{playerId}", "guard-17")
                        .header(TOKEN_HEADER, validToken))
                .andExpect(status().isOk());
        assertThat(roomId).isNotBlank();
    }

    // ============================================================ Cross-room / cross-player isolation

    @Test
    @DisplayName("18. Token is scoped to one room: token from room A rejected in room B for the same member")
    void tokenScopedToRoom() throws Exception {
        MvcResult roomAResult = createRoom("iso-owner-a", "Iso A", "Iso Room A");
        String roomA = roomId(roomAResult);
        String tokenA = playerToken(roomAResult);

        MvcResult roomBResult = createRoom("iso-owner-b", "Iso B", "Iso Room B");
        String roomB = roomId(roomBResult);

        MvcResult joinedA = mockMvc.perform(post("/api/rooms/{roomId}/join", roomA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"iso-member","playerName":"Member"}"""))
                .andExpect(status().isOk())
                .andReturn();
        String memberTokenA = playerToken(joinedA);

        MvcResult spectatedB = mockMvc.perform(post("/api/rooms/{roomId}/spectate", roomB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spectatorId":"iso-member","spectatorName":"Member"}"""))
                .andExpect(status().isOk())
                .andReturn();
        String memberTokenB = playerToken(spectatedB);
        assertThat(memberTokenA).isNotEqualTo(memberTokenB);

        mockMvc.perform(post("/api/rooms/{roomId}/ready", roomB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, memberTokenA)
                        .content("""
                                {"playerId":"iso-member"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/rooms/{roomId}/sessions", roomB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, memberTokenB)
                        .content("""
                                {"playerId":"iso-member","sessionId":"iso-s"}"""))
                .andExpect(status().isBadRequest());
        assertThat(tokenA).isNotBlank();
    }

    @Test
    @DisplayName("19. Token is bound to the player: another member's valid token cannot act as the owner")
    void tokenBoundToPlayer() throws Exception {
        MvcResult created = createRoom("bind-owner", "Bind Owner", "Bind Room");
        String roomId = roomId(created);
        String ownerToken = playerToken(created);

        MvcResult joined = mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"bind-joiner","playerName":"Joiner"}"""))
                .andExpect(status().isOk())
                .andReturn();
        String joinerToken = playerToken(joined);

        mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, joinerToken)
                        .content("""
                                {"playerId":"bind-owner"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/rooms/{roomId}/leave", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, ownerToken)
                        .content("""
                                {"playerId":"bind-owner"}"""))
                .andExpect(status().isNoContent());
    }

    // ============================================================ Unprotected endpoints

    @Test
    @DisplayName("20. GET /{roomId}/state and /{roomId}/progress require no token")
    void stateAndProgressUnprotected() throws Exception {
        String roomId = roomId(createRoom("open-20", "Open Twenty", "Open State"));
        mockMvc.perform(get("/api/rooms/{roomId}/state", roomId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms/{roomId}/progress", roomId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("21. List endpoints and issue-only endpoints (create/join/spectate) require no token")
    void listAndIssueEndpointsUnprotected() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms?type=MINESWEEPER"))
                .andExpect(status().isOk());
        MvcResult created = createRoom("open-21", "Open Twenty One", "Open List");
        String roomId = roomId(created);
        mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"open-joiner","playerName":"Open Joiner"}"""))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/rooms/{roomId}/spectate", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spectatorId":"open-spec","spectatorName":"Open Spec"}"""))
                .andExpect(status().isOk());
    }

    // ============================================================ 403 body shape (Step 3c)

    @Test
    @DisplayName("22. DELETE /{roomId} as NON-owner with valid token → 403 {error,status}, no message/timestamp/path")
    void deleteRoomNonOwner403NewShape() throws Exception {
        MvcResult created = createRoom("shape-owner", "Shape Owner", "Shape Delete");
        String roomId = roomId(created);
        MvcResult joined = mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"shape-nonowner","playerName":"NonOwner"}"""))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(delete("/api/rooms/{roomId}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TOKEN_HEADER, playerToken(joined))
                        .content("""
                                {"requesterId":"shape-nonowner"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only room owner can delete the room"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    @DisplayName("23. POST /{roomId}/join with wrong password → 403 {error,status}, no message/timestamp/path")
    void joinWrongPassword403NewShape() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"Shape Password Room",
                                    "gameType":"MINESWEEPER",
                                    "settings":{
                                        "gameType":"MINESWEEPER",
                                        "settings":{"rows":9,"cols":9,"mines":10},
                                        "passwordProtected":true,
                                        "passwordHash":"correct-password",
                                        "allowBots":false,
                                        "maxPlayers":2,
                                        "timeLimitSeconds":60,
                                        "isSinglePlayer":false
                                    },
                                    "ownerId":"shape-pw-owner",
                                    "ownerName":"Shape Pw Owner"
                                }"""))
                .andExpect(status().isCreated())
                .andReturn();
        String roomId = roomId(created);

        mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"shape-pw-joiner","playerName":"Pw Joiner","password":"wrong-password"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Invalid password"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    // ============================================================ Helpers

    private MvcResult createRoom(String ownerId, String ownerName, String roomName) throws Exception {
        return mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"%s",
                                    "gameType":"MINESWEEPER",
                                    "ownerId":"%s",
                                    "ownerName":"%s"
                                }""".formatted(roomName, ownerId, ownerName)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String roomId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("roomId").asText();
    }

    private String playerToken(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode token = json.get("playerToken");
        return token == null || token.isNull() ? "" : token.asText();
    }
}
