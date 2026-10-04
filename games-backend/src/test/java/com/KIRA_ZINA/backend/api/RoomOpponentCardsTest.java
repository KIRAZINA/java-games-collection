package com.KIRA_ZINA.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.KIRA_ZINA.backend.blackjack.domain.BlackjackSession;
import com.KIRA_ZINA.backend.blackjack.domain.Card;
import com.KIRA_ZINA.backend.blackjack.domain.Deck;
import com.KIRA_ZINA.backend.blackjack.domain.Rank;
import com.KIRA_ZINA.backend.blackjack.domain.Suit;
import com.KIRA_ZINA.backend.blackjack.service.BlackjackSessionService;
import com.KIRA_ZINA.backend.common.GameRoomService;
import java.lang.reflect.Field;
import java.util.Map;
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
 * Step 6e (A4): opponent-card projection on GET /api/rooms/{roomId}/state.
 *
 * The server never sends card data to the room endpoints before this step;
 * A1 adds a nullable {@code blackjack} field to each players[] entry, built
 * from that player's own session snapshot so the dealer hole-card reveal rule
 * is the pre-existing one. These tests pin that contract:
 *
 *  - a live two-player hand exposes each player's 2 face-up cards and only
 *    the dealer's face-up card (dealerValue null, no hole card);
 *  - once a player's round settles (stand), their dealer hand is fully
 *    revealed while a still-live opponent keeps the hole card hidden;
 *  - non-blackjack rooms and a pre-bet (BETTING) session carry no field.
 *
 * Decks are rigged to a fixed no-blackjack deal (the same
 * {@code setupDeck} reflection pattern as BlackjackSessionTest) so the
 * assertions do not depend on a random shuffle.
 */
@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Opponent cards on GET /{roomId}/state (Step 6e)")
class RoomOpponentCardsTest {

    private static final String REGISTER = "/api/rooms/%s/sessions";
    private static final String READY = "/api/rooms/%s/ready";
    private static final String BET = "/api/blackjack/sessions/%s/bets";
    private static final String STAND = "/api/blackjack/sessions/%s/stand";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private BlackjackSessionService blackjackSessionService;

    @AfterEach
    void cleanup() throws Exception {
        TestUtils.resetGameRooms(gameRoomService);
        TestUtils.resetBlackjackSessions(blackjackSessionService);
    }

    @Test
    @DisplayName("1. Live two-player hand: each player shows 2 cards, dealer face-up only, dealerValue null")
    void liveHandExposesOwnCardsAndDealerFaceUpOnly() throws Exception {
        JsonNode created = createBlackjackRoom("owner-a", "Owner A");
        String roomId = text(created, "roomId");
        String ownerToken = text(created, "playerToken");

        String sa = createBlackjackSession();
        rigDeck(sa, new Card(Suit.HEARTS, Rank.FIVE), new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.SIX), new Card(Suit.SPADES, Rank.EIGHT));
        registerSession(roomId, "owner-a", sa, ownerToken);

        String join = joinPlayer(roomId, "owner-b", "Player B");
        String bToken = joinToken(join);
        String sb = createBlackjackSession();
        rigDeck(sb, new Card(Suit.HEARTS, Rank.FIVE), new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.SIX), new Card(Suit.SPADES, Rank.EIGHT));
        registerSession(roomId, "owner-b", sb, bToken);

        // both ready -> blackjack room starts PLAYING
        markReady(roomId, "owner-a", ownerToken);
        markReady(roomId, "owner-b", bToken);

        placeBet(sa, 10.0);
        placeBet(sb, 10.0);

        JsonNode a = playerByPlayerId(roomId, "owner-a");
        JsonNode b = playerByPlayerId(roomId, "owner-b");

        assertLiveBlackjack(a, "owner-a");
        assertLiveBlackjack(b, "owner-b");
    }

    @Test
    @DisplayName("2. After A stands: A's dealer hand is fully revealed, B still hides the hole card")
    void settledPlayerRevealsDealerWhileLiveOpponentHides() throws Exception {
        JsonNode created = createBlackjackRoom("owner-a", "Owner A");
        String roomId = text(created, "roomId");
        String ownerToken = text(created, "playerToken");

        String sa = createBlackjackSession();
        rigDeck(sa, new Card(Suit.HEARTS, Rank.FIVE), new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.SIX), new Card(Suit.SPADES, Rank.EIGHT));
        registerSession(roomId, "owner-a", sa, ownerToken);

        String join = joinPlayer(roomId, "owner-b", "Player B");
        String bToken = joinToken(join);
        String sb = createBlackjackSession();
        rigDeck(sb, new Card(Suit.HEARTS, Rank.FIVE), new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.SIX), new Card(Suit.SPADES, Rank.EIGHT));
        registerSession(roomId, "owner-b", sb, bToken);

        markReady(roomId, "owner-a", ownerToken);
        markReady(roomId, "owner-b", bToken);

        placeBet(sa, 10.0);
        placeBet(sb, 10.0);
        // A commits: dealer plays out and the round settles -> A at ROUND_OVER
        mockMvc.perform(post(String.format(STAND, sa))).andExpect(status().isOk());

        JsonNode a = playerByPlayerId(roomId, "owner-a");
        JsonNode b = playerByPlayerId(roomId, "owner-b");

        // A settled: full dealer hand (>= 2) with a numeric dealerValue.
        JsonNode abj = a.get("blackjack");
        assertThat(abj).isNotNull();
        assertThat(abj.get("phase").asText()).isEqualTo("ROUND_OVER");
        assertThat(abj.get("playerCards").size()).isEqualTo(2);
        assertThat(abj.get("dealerCards").size()).isGreaterThanOrEqualTo(2);
        assertThat(abj.get("dealerValue").isNull()).as("settled dealerValue must be numeric").isFalse();
        // A settled dealer hand is a real blackjack total; it may bust (> 21),
        // so the valid range is the full 2-card-plus span, not just 0-21.
        assertThat(abj.get("dealerValue").asInt()).as("settled dealer value").isBetween(2, 26);

        // B is still mid-round: hole card still hidden.
        assertLiveBlackjack(b, "owner-b");
    }

    @Test
    @DisplayName("3. Non-blackjack room: blackjack field is absent on every player")
    void nonBlackjackRoomCarriesNoBlackjackField() throws Exception {
        // A 2048 room: a real session so players[] is non-empty.
        MvcResult created = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"2048 Room",
                                    "gameType":"TWENTY_FORTY_EIGHT",
                                    "settings":{
                                        "gameType":"TWENTY_FORTY_EIGHT",
                                        "settings":{"size":4},
                                        "maxPlayers":2,
                                        "timeLimitSeconds":60,
                                        "isSinglePlayer":false
                                    },
                                    "ownerId":"owner-c",
                                    "ownerName":"Owner C"
                                }"""))
                .andExpect(status().isCreated())
                .andReturn();
        String roomId = text(created, "roomId");
        String token = text(created, "playerToken");

        MvcResult session = mockMvc.perform(post("/api/2048/sessions"))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = text(session, "sessionId");
        registerSession(roomId, "owner-c", sessionId, token);

        JsonNode p = playerByPlayerId(roomId, "owner-c");
        assertThat(p.has("metrics")).as("metrics are always present").isTrue();
        assertThat(p.has("blackjack")).as("non-blackjack rooms must omit the blackjack field").isFalse();
    }

    @Test
    @DisplayName("4. Before the first bet (BETTING): blackjack is null/absent on every player")
    void preBetBlackjackIsAbsent() throws Exception {
        JsonNode created = createBlackjackRoom("owner-a", "Owner A");
        String roomId = text(created, "roomId");
        String ownerToken = text(created, "playerToken");

        String sa = createBlackjackSession();
        rigDeck(sa, new Card(Suit.HEARTS, Rank.FIVE), new Card(Suit.CLUBS, Rank.NINE),
                new Card(Suit.DIAMONDS, Rank.SIX), new Card(Suit.SPADES, Rank.EIGHT));
        registerSession(roomId, "owner-a", sa, ownerToken);

        String join = joinPlayer(roomId, "owner-b", "Player B");
        String bToken = joinToken(join);
        String sb = createBlackjackSession();
        registerSession(roomId, "owner-b", sb, bToken);

        // Neither player has placed a bet: both sessions are still in BETTING.
        JsonNode a = playerByPlayerId(roomId, "owner-a");
        JsonNode b = playerByPlayerId(roomId, "owner-b");
        assertThat(a.has("blackjack")).as("BETTING session must not expose blackjack cards").isFalse();
        assertThat(b.has("blackjack")).as("BETTING session must not expose blackjack cards").isFalse();
    }

    // ── assertion helpers ────────────────────────────────────────────────────

    private void assertLiveBlackjack(JsonNode player, String playerId) {
        JsonNode bj = player.get("blackjack");
        assertThat(bj).as("blackjack field for %s", playerId).isNotNull();
        assertThat(bj.get("phase").asText()).isEqualTo("PLAYER_TURN");
        // Two face-up player cards, exactly one visible dealer card.
        assertThat(bj.get("playerCards").size()).isEqualTo(2);
        assertThat(bj.get("playerValue").asInt()).isBetween(2, 21);
        assertThat(bj.get("dealerCards").size())
                .as("a live opponent shows only the dealer's face-up card (no hole card)")
                .isEqualTo(1);
        assertThat(bj.get("dealerValue").isNull())
                .as("dealerValue is null while the hole card is hidden")
                .isTrue();
        // The single dealer card is a real card, not a placeholder.
        JsonNode upCard = bj.get("dealerCards").get(0);
        assertThat(upCard.get("rank").asText()).isNotBlank();
        assertThat(upCard.get("suit").asText()).isNotBlank();
    }

    // ── flow helpers ─────────────────────────────────────────────────────────

    private JsonNode createBlackjackRoom(String ownerId, String ownerName) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roomName":"Opponent Cards Room",
                                    "gameType":"BLACKJACK",
                                    "settings":{
                                        "gameType":"BLACKJACK",
                                        "settings":{"initialBalance":100,"difficulty":"BASIC"},
                                        "maxPlayers":2,
                                        "timeLimitSeconds":0,
                                        "isSinglePlayer":false
                                    },
                                    "ownerId":"%s",
                                    "ownerName":"%s"
                                }""".formatted(ownerId, ownerName)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapperRead(created.getResponse().getContentAsString());
    }

    private String joinPlayer(String roomId, String playerId, String playerName) throws Exception {
        return new String(mockMvc.perform(post("/api/rooms/{roomId}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"%s","playerName":"%s"}""".formatted(playerId, playerName)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private String joinToken(String json) throws Exception {
        return text(objectMapperRead(json), "playerToken");
    }

    private void registerSession(String roomId, String playerId, String sessionId, String token) throws Exception {
        mockMvc.perform(post(String.format(REGISTER, roomId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Player-Token", token)
                        .content("""
                                {"playerId":"%s","sessionId":"%s"}""".formatted(playerId, sessionId)))
                .andExpect(status().isCreated());
    }

    private void markReady(String roomId, String playerId, String token) throws Exception {
        mockMvc.perform(post(String.format(READY, roomId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Player-Token", token)
                        .content("""
                                {"playerId":"%s"}""".formatted(playerId)))
                .andExpect(status().isOk());
    }

    private String createBlackjackSession() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/blackjack/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        return text(r, "sessionId");
    }

    private void placeBet(String sessionId, double amount) throws Exception {
        mockMvc.perform(post(String.format(BET, sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":%s}""".formatted(amount)))
                .andExpect(status().isOk());
    }

    // ── deck rigging (mirrors BlackjackSessionTest.setupDeck) ────────────────

    private void rigDeck(String sessionId, Card... topCards) throws Exception {
        Field sessionsField = BlackjackSessionService.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, BlackjackSession> sessions =
                (Map<String, BlackjackSession>) sessionsField.get(blackjackSessionService);
        BlackjackSession session = sessions.get(sessionId);
        if (session == null) throw new IllegalStateException("session not found: " + sessionId);

        Deck deck = new Deck();
        Field cardsField = Deck.class.getDeclaredField("cards");
        cardsField.setAccessible(true);
        Card[] cards = (Card[]) cardsField.get(deck);
        Field nextIndexField = Deck.class.getDeclaredField("nextCardIndex");
        nextIndexField.setAccessible(true);
        for (int i = 0; i < topCards.length && i < cards.length; i++) {
            cards[cards.length - 1 - i] = topCards[i];
        }
        nextIndexField.set(deck, cards.length);
        Field deckField = BlackjackSession.class.getDeclaredField("deck");
        deckField.setAccessible(true);
        deckField.set(session, deck);
    }

    // ── parsing helpers ──────────────────────────────────────────────────────

    private JsonNode playerByPlayerId(String roomId, String playerId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rooms/{roomId}/state", roomId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode players = json.get("players");
        for (JsonNode p : players) {
            if (p.get("playerId").asText().equals(playerId)) return p;
        }
        throw new AssertionError("player not present in /state: " + playerId);
    }

    private String text(MvcResult result, String field) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return text(json, field);
    }

    private String text(JsonNode json, String field) throws Exception {
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private JsonNode objectMapperRead(String json) throws Exception {
        return objectMapper.readTree(json);
    }
}
