package com.KIRA_ZINA.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.KIRA_ZINA.backend.blackjack.domain.BlackjackState;
import com.KIRA_ZINA.backend.blackjack.service.BlackjackSessionService;
import com.KIRA_ZINA.backend.minesweeper.service.MinesweeperSessionService;
import com.KIRA_ZINA.backend.twentyfortyeight.service.Game2048SessionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GameRoomService")
class GameRoomServiceTest {

    private GameRoomService roomService;

    @BeforeEach
    void setUp() {
        BlackjackSessionService bjMock = mock(BlackjackSessionService.class);
        MinesweeperSessionService msMock = mock(MinesweeperSessionService.class);
        Game2048SessionService tfMock = mock(Game2048SessionService.class);
        when(bjMock.state(any())).thenThrow(new IllegalArgumentException("not found"));
        when(msMock.state(any())).thenThrow(new IllegalArgumentException("not found"));
        when(tfMock.state(any())).thenThrow(new IllegalArgumentException("not found"));
        roomService = new GameRoomService(bjMock, msMock, tfMock);
    }

    @Nested
    @DisplayName("Create Room")
    class CreateRoom {

        @Test
        @DisplayName("createRoom returns summary with correct fields")
        void createsRoomWithCorrectFields() {
            GameRoom.RoomSummary summary = roomService.createRoom(
                    "Test Room", GameSettings.defaultFor(GameType.BLACKJACK),
                    "owner-1", "Alice");

            assertThat(summary.roomName()).isEqualTo("Test Room");
            assertThat(summary.gameType()).isEqualTo(GameType.BLACKJACK);
            assertThat(summary.state()).isEqualTo(GameRoom.RoomState.WAITING);
            assertThat(summary.playerCount()).isEqualTo(1);
            assertThat(summary.ownerName()).isEqualTo("Alice");
            assertThat(summary.maxPlayers()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("List Rooms")
    class ListRooms {

        @Test
        @DisplayName("listRooms returns rooms filtered by game type")
        void filtersByGameType() {
            roomService.createRoom("BJ 1", GameSettings.defaultFor(GameType.BLACKJACK), "o1", "A");
            roomService.createRoom("BJ 2", GameSettings.defaultFor(GameType.BLACKJACK), "o2", "B");
            roomService.createRoom("MS 1", GameSettings.defaultFor(GameType.MINESWEEPER), "o3", "C");

            List<GameRoom.RoomSummary> bjRooms = roomService.listRooms(GameType.BLACKJACK);
            assertThat(bjRooms).hasSize(2);

            List<GameRoom.RoomSummary> msRooms = roomService.listRooms(GameType.MINESWEEPER);
            assertThat(msRooms).hasSize(1);

            List<GameRoom.RoomSummary> tfRooms = roomService.listRooms(GameType.TWENTY_FORTY_EIGHT);
            assertThat(tfRooms).isEmpty();
        }
    }

    @Nested
    @DisplayName("Join Room")
    class JoinRoom {

        @Test
        @DisplayName("joinRoom adds player and increments count")
        void joinRoomAddsPlayer() {
            GameRoom.RoomSummary summary = roomService.createRoom(
                    "Join Test", GameSettings.defaultFor(GameType.BLACKJACK),
                    "owner-1", "Alice");

            GameRoom.RoomSummary afterJoin = roomService.joinRoom(
                    summary.roomId(), "player-2", "Bob", null);

            assertThat(afterJoin.playerCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("joinRoom with wrong roomId throws")
        void joinRoomNonExistentThrows() {
            assertThatThrownBy(() -> roomService.joinRoom("no-such-room", "p1", "X", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("Leave Room — Auto-Cleanup")
    class LeaveRoom {

        @Test
        @DisplayName("last player leaving removes the room immediately")
        void lastPlayerLeavesRemovesRoom() {
            GameRoom.RoomSummary summary = roomService.createRoom(
                    "Cleanup Test", GameSettings.defaultFor(GameType.BLACKJACK),
                    "owner-1", "Alice");

            assertThat(roomService.getRoom(summary.roomId())).isPresent();

            boolean removed = roomService.leaveRoom("owner-1");
            assertThat(removed).isTrue();

            // Room should be gone
            assertThat(roomService.getRoom(summary.roomId())).isNotPresent();
        }

        @Test
        @DisplayName("room stays when non-last player leaves")
        void nonLastPlayerLeavesRoomStays() {
            GameRoom.RoomSummary summary = roomService.createRoom(
                    "Stay Test", GameSettings.defaultFor(GameType.BLACKJACK),
                    "owner-1", "Alice");

            roomService.joinRoom(summary.roomId(), "player-2", "Bob", null);

            boolean removed = roomService.leaveRoom("player-2");
            assertThat(removed).isFalse();

            // Room still exists with owner
            assertThat(roomService.getRoom(summary.roomId())).isPresent();
        }

        @Test
        @DisplayName("leaving with non-existent player returns false")
        void leaveNonExistentPlayerReturnsFalse() {
            roomService.createRoom(
                    "No-op Test", GameSettings.defaultFor(GameType.BLACKJACK),
                    "owner-1", "Alice");

            boolean removed = roomService.leaveRoom("no-such-player");
            assertThat(removed).isFalse();
        }
    }
}
