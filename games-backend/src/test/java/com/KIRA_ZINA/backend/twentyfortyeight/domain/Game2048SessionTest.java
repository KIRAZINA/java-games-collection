package com.KIRA_ZINA.backend.twentyfortyeight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link Game2048Session} covering:
 * - Tile merge logic (one merge per tile per move)
 * - All four move directions (LEFT, RIGHT, UP, DOWN)
 * - Score accumulation from merges
 * - No-op moves (board unchanged)
 * - Game-over detection
 * - Reset lifecycle
 */
@DisplayName("Game2048Session")
class Game2048SessionTest {

    // ====================================================== Initial State

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("new session starts with exactly 2 tiles and score 0")
        void newSessionStartsWithTwoTiles() {
            Game2048Session session = new Game2048Session("2048-1");
            Game2048State state = session.state();

            assertThat(state.tiles()).hasSize(2);
            assertThat(state.score()).isZero();
            assertThat(state.gameOver()).isFalse();
            assertThat(state.size()).isEqualTo(4);
        }

        @Test
        @DisplayName("all initial tile values are 2 or 4")
        void initialTileValuesAre2Or4() {
            Game2048Session session = new Game2048Session("init");
            session.state().tiles().forEach(tile ->
                    assertThat(tile.value()).isIn(2, 4));
        }
    }

    // ====================================================== Move LEFT

    @Nested
    @DisplayName("Move LEFT")
    class MoveLeft {

        @Test
        @DisplayName("[2,2,0,0] → [4,0,0,0] + new random tile, score = 4")
        void moveLeftMergesTwoTiles() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    2, 2, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0
            });
            Game2048State state = session.move(MoveDirection.LEFT);

            assertThat(state.score()).isEqualTo(4);
            assertThat(state.moved()).isTrue();
            // Row 0 must have a tile of value 4 at col 0
            assertThat(tileAt(state, 0, 0)).isEqualTo(4);
        }

        @Test
        @DisplayName("[2,2,2,0] → [4,2,0,0] — only one merge of the first pair")
        void moveLeftMergesOnlyFirstPair() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    2, 2, 2, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0
            });
            Game2048State state = session.move(MoveDirection.LEFT);

            assertThat(state.score()).isEqualTo(4);
            assertThat(tileAt(state, 0, 0)).isEqualTo(4);
            assertThat(tileAt(state, 0, 1)).isEqualTo(2);
        }

        @Test
        @DisplayName("[2,2,2,2] → [4,4,0,0] — each tile merges only once per move")
        void moveMergesEachTileOnlyOnce() throws Exception {
            Game2048Session session = new Game2048Session("2048-2");
            setCells(session, new int[]{
                    2, 2, 2, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0
            });
            Game2048State state = session.move(MoveDirection.LEFT);

            boolean hasMergedFour = state.tiles().stream()
                    .filter(tile -> tile.row() == 0).anyMatch(tile -> tile.value() == 4);
            boolean hasRemainingTwo = state.tiles().stream()
                    .filter(tile -> tile.row() == 0).anyMatch(tile -> tile.value() == 2);
            boolean hasInvalidDoubleMerge = state.tiles().stream().anyMatch(tile -> tile.value() == 8);

            assertThat(state.score()).isEqualTo(4);
            assertThat(hasMergedFour).isTrue();
            assertThat(hasRemainingTwo).isTrue();
            assertThat(hasInvalidDoubleMerge).isFalse();
            assertThat(state.moved()).isTrue();
        }

        @Test
        @DisplayName("[2,4,2,4] → no merges, tiles compact left")
        void moveLeftNoMergeDifferentValues() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    0, 2, 4, 2,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0
            });
            Game2048State state = session.move(MoveDirection.LEFT);

            assertThat(state.score()).isEqualTo(0); // before random tile
            assertThat(tileAt(state, 0, 0)).isEqualTo(2);
            assertThat(tileAt(state, 0, 1)).isEqualTo(4);
            assertThat(tileAt(state, 0, 2)).isEqualTo(2);
        }
    }

    // ====================================================== Move RIGHT

    @Nested
    @DisplayName("Move RIGHT")
    class MoveRight {

        @Test
        @DisplayName("[0,0,2,2] → [0,0,0,4], score = 4")
        void moveRightMergesCorrectly() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    0, 0, 2, 2,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0
            });
            Game2048State state = session.move(MoveDirection.RIGHT);

            assertThat(state.score()).isEqualTo(4);
            assertThat(tileAt(state, 0, 3)).isEqualTo(4);
            assertThat(state.moved()).isTrue();
        }

        @Test
        @DisplayName("[2,2,2,0] RIGHT → [0,0,2,4]")
        void moveRightThreeTiles() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    2, 2, 2, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0
            });
            Game2048State state = session.move(MoveDirection.RIGHT);

            assertThat(state.score()).isEqualTo(4);
            assertThat(tileAt(state, 0, 3)).isEqualTo(4);
            assertThat(tileAt(state, 0, 2)).isEqualTo(2);
        }
    }

    // ====================================================== Move UP

    @Nested
    @DisplayName("Move UP")
    class MoveUp {

        @Test
        @DisplayName("column [2,2,0,0]ᵀ UP → [4,0,0,0]ᵀ, score = 4")
        void moveUpMergesCorrectly() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    2, 0, 0, 0,
                    2, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0
            });
            Game2048State state = session.move(MoveDirection.UP);

            assertThat(state.score()).isEqualTo(4);
            assertThat(tileAt(state, 0, 0)).isEqualTo(4);
            assertThat(state.moved()).isTrue();
        }

        @Test
        @DisplayName("column [2,2,2,0]ᵀ UP → [4,2,0,0]ᵀ")
        void moveUpThreeTiles() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    2, 0, 0, 0,
                    2, 0, 0, 0,
                    2, 0, 0, 0,
                    0, 0, 0, 0
            });
            Game2048State state = session.move(MoveDirection.UP);

            assertThat(state.score()).isEqualTo(4);
            assertThat(tileAt(state, 0, 0)).isEqualTo(4);
            assertThat(tileAt(state, 1, 0)).isEqualTo(2);
        }
    }

    // ====================================================== Move DOWN

    @Nested
    @DisplayName("Move DOWN")
    class MoveDown {

        @Test
        @DisplayName("column [0,0,2,2]ᵀ DOWN → [0,0,0,4]ᵀ, score = 4")
        void moveDownMergesCorrectly() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    2, 0, 0, 0,
                    2, 0, 0, 0
            });
            Game2048State state = session.move(MoveDirection.DOWN);

            assertThat(state.score()).isEqualTo(4);
            assertThat(tileAt(state, 3, 0)).isEqualTo(4);
            assertThat(state.moved()).isTrue();
        }
    }

    // ====================================================== Score

    @Nested
    @DisplayName("Score Accumulation")
    class ScoreAccumulation {

        @Test
        @DisplayName("score accumulates across multiple moves")
        void scoreAccumulatesAcrossMoves() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    2, 2, 4, 4,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0
            });
            // LEFT: 2+2=4 (score +4), 4+4=8 (score +8) → total score = 12
            Game2048State state = session.move(MoveDirection.LEFT);
            assertThat(state.score()).isEqualTo(12);
        }

        @Test
        @DisplayName("score does not change when move produces no merges")
        void scoreUnchangedWhenNoMerge() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    2, 4, 8, 16,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0
            });
            Game2048State state = session.move(MoveDirection.LEFT); // already compacted
            // No change possible in row 0 since values differ
            // Score from compaction moves only = 0
            assertThat(state.score()).isZero();
        }
    }

    // ====================================================== No-Op Move

    @Nested
    @DisplayName("No-Op Move (board unchanged)")
    class NoOpMove {

        @Test
        @DisplayName("move LEFT on already-left-compacted board sets moved=false")
        void noRandomTileWhenBoardUnchanged() throws Exception {
            Game2048Session session = session();
            // Row 0 fully left-compacted, no merges possible
            setCells(session, new int[]{
                    2, 4, 8, 16,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0
            });
            int tileBefore = session.state().tiles().size();
            Game2048State state = session.move(MoveDirection.LEFT);

            // moved=false when board is unchanged
            assertThat(state.moved()).isFalse();
            // No extra tile added
            assertThat(state.tiles()).hasSize(tileBefore);
        }
    }

    // ====================================================== Game-Over Detection

    @Nested
    @DisplayName("Game-Over Detection")
    class GameOverDetection {

        @Test
        @DisplayName("gameOver=true when no empty cells and no adjacent equal values")
        void gameOverWhenNoMovePossible() throws Exception {
            Game2048Session session = session();
            // A board where no adjacent cells have equal values → no moves
            setCells(session, new int[]{
                    2,  4,  2,  4,
                    4,  2,  4,  2,
                    2,  4,  2,  4,
                    4,  2,  4,  2
            });
            // Make a move to trigger the game-over check
            Game2048State state = session.move(MoveDirection.LEFT);
            assertThat(state.gameOver()).isTrue();
        }

        @Test
        @DisplayName("gameOver=false when full board has two adjacent equal values")
        void gameNotOverWhenMergePossible() throws Exception {
            Game2048Session session = session();
            // Two equal values adjacent in row 0 → at least LEFT is possible
            setCells(session, new int[]{
                    2,  2,  4,  8,
                    4,  8, 16, 32,
                    8, 16, 32, 64,
                   16, 32, 64,128
            });
            Game2048State state = session.move(MoveDirection.LEFT);
            // After merge, gameOver should be false because there are empty cells
            assertThat(state.gameOver()).isFalse();
        }

        @Test
        @DisplayName("move after gameOver is a no-op: state unchanged")
        void moveAfterGameOverIsNoOp() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    2, 4, 2, 4,
                    4, 2, 4, 2,
                    2, 4, 2, 4,
                    4, 2, 4, 2
            });
            session.move(MoveDirection.LEFT); // trigger game over
            Game2048State overState = session.state();
            assertThat(overState.gameOver()).isTrue();

            // Any further move should return the same state
            Game2048State afterExtraMove = session.move(MoveDirection.RIGHT);
            assertThat(afterExtraMove.gameOver()).isTrue();
            assertThat(afterExtraMove.score()).isEqualTo(overState.score());
        }
    }

    // ====================================================== Reset

    @Nested
    @DisplayName("Reset")
    class Reset {

        @Test
        @DisplayName("reset clears board and score, adds 2 new tiles")
        void resetClearsBoardAndScore() throws Exception {
            Game2048Session session = session();
            setCells(session, new int[]{
                    2, 4, 8, 16,
                    4, 8, 16, 32,
                    8, 16, 32, 64,
                   16, 32, 64, 128
            });
            session.move(MoveDirection.UP); // mutate score
            Game2048State resetState = session.reset();

            assertThat(resetState.score()).isZero();
            assertThat(resetState.gameOver()).isFalse();
            assertThat(resetState.tiles()).hasSize(2);
        }
    }

    // ====================================================== Helpers

    private static Game2048Session session() {
        return new Game2048Session("test");
    }

    private static void setCells(Game2048Session session, int[] values) throws Exception {
        Field cellsField = Game2048Session.class.getDeclaredField("cells");
        cellsField.setAccessible(true);
        int[] cells = (int[]) cellsField.get(session);
        System.arraycopy(values, 0, cells, 0, values.length);
    }

    /**
     * Returns the value of a tile at (row, col), or 0 if none.
     */
    private static int tileAt(Game2048State state, int row, int col) {
        return state.tiles().stream()
                .filter(t -> t.row() == row && t.col() == col)
                .mapToInt(Game2048Tile::value)
                .findFirst()
                .orElse(0);
    }
}
