package com.KIRA_ZINA.backend.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.KIRA_ZINA.backend.blackjack.service.BlackjackSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.KIRA_ZINA.backend.common.GameRoomService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Item 7 - extraction failure logging")
@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = false)
class GameRoomExtractionLoggingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BlackjackSessionService blackjackSessionService;

    private ch.qos.logback.classic.Logger roomLogger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void attachAppender() {
        roomLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GameRoomService.class);
        originalLevel = roomLogger.getLevel();
        appender = new ListAppender<>();
        appender.start();
        roomLogger.addAppender(appender);
        roomLogger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void detachAppender() {
        roomLogger.setLevel(originalLevel);
        roomLogger.detachAppender(appender);
    }

    private String createRoom(String owner, String name) throws Exception {
        var result = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomName\":\"extract-log-room\",\"gameType\":\"BLACKJACK\","
                                + "\"ownerId\":\"" + owner + "\",\"ownerName\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        var node = MAPPER.readTree(result.getResponse().getContentAsString());
        String roomId = node.get("roomId").asText();
        String token = node.get("playerToken").asText();
        assertNotNull(token, "createRoom must issue a player token");
        return roomId + "|" + token;
    }

    private void deleteRoomQuietly(String roomId, String owner, String token) {
        try {
            mockMvc.perform(delete("/api/rooms/" + roomId)
                            .header("X-Player-Token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"requesterId\":\"" + owner + "\"}"))
                    .andReturn();
        } catch (Exception ignored) {
            // best-effort cleanup only
        }
    }

    @Test
    @DisplayName("Session-not-found: DEBUG logged, /state keeps literal error marker, /progress keeps fallback shape")
    void sessionNotFoundLogsDebugAndKeepsMarkers() throws Exception {
        String[] room = createRoom("xl-owner-a", "Owner A").split("\\|");
        String roomId = room[0];
        String token = room[1];
        try {
            mockMvc.perform(post("/api/rooms/" + roomId + "/sessions")
                            .header("X-Player-Token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"playerId\":\"xl-owner-a\",\"sessionId\":\"missing-session-xyz\"}"))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/rooms/" + roomId + "/state"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.players[0].playerId").value("xl-owner-a"))
                    .andExpect(jsonPath("$.players[0].metrics.error").value("Session not found"));

            mockMvc.perform(get("/api/rooms/" + roomId + "/progress"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.players[0].playerId").value("xl-owner-a"))
                    .andExpect(jsonPath("$.players[0].gameOver").value(true))
                    .andExpect(jsonPath("$.players[0].phase").value(""))
                    .andExpect(jsonPath("$.players[0].balance").value(0));

            List<ILoggingEvent> debugs = appender.list.stream()
                    .filter(e -> e.getLevel() == Level.DEBUG)
                    .filter(e -> e.getFormattedMessage().contains("missing-session-xyz"))
                    .toList();
            assertFalse(debugs.isEmpty(), "expected DEBUG log naming the missing session");
            boolean rnfeLogged = debugs.stream().anyMatch(e ->
                    e.getThrowableProxy() != null
                            && e.getThrowableProxy().getClassName()
                                    .endsWith("ResourceNotFoundException"));
            assertTrue(rnfeLogged, "DEBUG event must carry the ResourceNotFoundException");

            boolean warnForSession = appender.list.stream()
                    .filter(e -> e.getLevel() == Level.WARN)
                    .anyMatch(e -> e.getFormattedMessage().contains("missing-session-xyz"));
            assertFalse(warnForSession,
                    "ResourceNotFoundException must log at DEBUG, never WARN");
        } finally {
            deleteRoomQuietly(roomId, "xl-owner-a", token);
        }
    }

    @Test
    @DisplayName("Non-RNFE failure (corrupted session state): WARN logged with sessionId + exception class, /state 200 with marker")
    void corruptedSessionStateLogsWarnAndKeepsMarkers() throws Exception {
        String[] room = createRoom("xl-owner-b", "Owner B").split("\\|");
        String roomId = room[0];
        String token = room[1];
        try {
            var sessionCreate = mockMvc.perform(post("/api/blackjack/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"initialBalance\":100}"))
                    .andExpect(status().isCreated())
                    .andReturn();
            String sessionId = MAPPER.readTree(sessionCreate.getResponse().getContentAsString())
                    .get("sessionId").asText();

            mockMvc.perform(post("/api/rooms/" + roomId + "/sessions")
                            .header("X-Player-Token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"playerId\":\"xl-owner-b\",\"sessionId\":\"" + sessionId + "\"}"))
                    .andExpect(status().isCreated());

            // Corrupt the live session: nulling the non-final deck field makes
            // snapshot() throw NullPointerException - a non-RNFE failure.
            Field sessionsField = BlackjackSessionService.class.getDeclaredField("sessions");
            sessionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> sessions = (Map<String, Object>) sessionsField.get(blackjackSessionService);
            Object session = sessions.get(sessionId);
            assertNotNull(session, "session must exist before corruption");
            Field deckField = session.getClass().getDeclaredField("deck");
            deckField.setAccessible(true);
            deckField.set(session, null);

            mockMvc.perform(get("/api/rooms/" + roomId + "/state"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.players[0].playerId").value("xl-owner-b"))
                    .andExpect(jsonPath("$.players[0].metrics.error").value("Session not found"));

            List<ILoggingEvent> warns = appender.list.stream()
                    .filter(e -> e.getLevel() == Level.WARN)
                    .filter(e -> e.getFormattedMessage().contains(sessionId))
                    .toList();
            assertFalse(warns.isEmpty(),
                    "expected WARN naming the session; events=" + appender.list.size());
            assertTrue(warns.get(0).getFormattedMessage().contains("NullPointerException"),
                    "WARN must include the exception class: " + warns.get(0).getFormattedMessage());
        } finally {
            deleteRoomQuietly(roomId, "xl-owner-b", token);
        }
    }
}
