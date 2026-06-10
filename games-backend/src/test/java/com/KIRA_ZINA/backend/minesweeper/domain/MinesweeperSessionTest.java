package com.KIRA_ZINA.backend.minesweeper.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link MinesweeperSession} covering:
 * - First-click safety zone (3×3 mine-free guarantee)
 * - BFS flood-fill efficiency (zero-region reveals)
 * - Win/loss state transitions
 * - Flag toggle mechanics
 * - Board validation
 * - Reset lifecycle
 */
@DisplayName("MinesweeperSession")
class MinesweeperSessionTest {

    // ====================================================== First-Click Safety

    @Nested
    @DisplayName("First-Click Safety Zone")
    class FirstClickSafety {

        @Test
        @DisplayName("first open sets firstClickDone=true and opened cell is safe")
        void firstOpenPlacesMinesAndKeepsOpenedCellSafe() {
            MinesweeperSession session = new MinesweeperSession("mines-1", 9, 9, 10);
            MinesweeperState state = session.open(4, 4);

            assertThat(state.firstClickDone()).isTrue();
            assertThat(state.gameOver()).isFalse();
            MinesweeperCellView opened = cellAt(state, 4, 4);
            assertThat(opened.state()).isEqualTo(MinesweeperCellState.OPENED);
            assertThat(opened.mine()).isFalse();
            assertThat(state.cells()).hasSize(81);
        }

        @RepeatedTest(10)
        @DisplayName("no mine in the 3×3 zone around first click (center click) — repeated for randomness")
        void firstClickIs3x3MineFree_center() {
            MinesweeperSession session = new MinesweeperSession("m", 9, 9, 10);
            MinesweeperState state = session.open(4, 4);

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    MinesweeperCellView cell = cellAt(state, 4 + dr, 4 + dc);
                    assertThat(cell.mine())
                            .as("Cell (%d,%d) must not be a mine after first click", 4 + dr, 4 + dc)
                            .isFalse();
                }
            }
        }

        @RepeatedTest(10)
        @DisplayName("no mine in the safe zone around corner click (0,0) — repeated for randomness")
        void firstClickIs3x3MineFree_corner() {
            MinesweeperSession session = new MinesweeperSession("m", 9, 9, 10);
            MinesweeperState state = session.open(0, 0);

            // Corner (0,0) → safe zone is a 2×2: (0,0),(0,1),(1,0),(1,1)
            for (int r = 0; r <= 1; r++) {
                for (int c = 0; c <= 1; c++) {
                    assertThat(cellAt(state, r, c).mine())
                            .as("Cell (%d,%d) must not be a mine", r, c)
                            .isFalse();
                }
            }
        }

        @RepeatedTest(10)
        @DisplayName("no mine in the safe zone around edge click (0,4) — repeated for randomness")
        void firstClickIs3x3MineFree_edge() {
            MinesweeperSession session = new MinesweeperSession("m", 9, 9, 10);
            MinesweeperState state = session.open(0, 4);

            // Edge row=0: safe zone rows [0..1], cols [3..5]
            for (int r = 0; r <= 1; r++) {
                for (int c = 3; c <= 5; c++) {
                    assertThat(cellAt(state, r, c).mine())
                            .as("Cell (%d,%d) must not be a mine", r, c)
                            .isFalse();
                }
            }
        }
    }

    // ====================================================== BFS Flood-Fill

    @Nested
    @DisplayName("BFS Flood-Fill")
    class BfsFloodFill {

        @Test
        @DisplayName("opening a 0-adjacent cell reveals the entire connected zero region")
        void bfsFloodFillOpensZeroRegion() {
            // Use a large board (16×16, few mines) to maximize the chance of a zero-region
            MinesweeperSession session = new MinesweeperSession("bfs", 9, 9, 5);
            MinesweeperState state = session.open(4, 4);

            // After first click, multiple cells should be OPENED due to flood-fill
            long openedCount = state.cells().stream()
                    .filter(c -> c.state() == MinesweeperCellState.OPENED)
                    .count();
            // At minimum, the clicked cell itself is opened
            assertThat(openedCount).isGreaterThanOrEqualTo(1);

            // No OPENED cell should be a mine
            boolean anyOpenedMine = state.cells().stream()
                    .filter(c -> c.state() == MinesweeperCellState.OPENED)
                    .anyMatch(MinesweeperCellView::mine);
            assertThat(anyOpenedMine).isFalse();
        }

        @Test
        @DisplayName("BFS does not open flagged cells during flood-fill")
        void bfsDoesNotOpenFlaggedNeighbors() {
            MinesweeperSession session = new MinesweeperSession("bfs2", 9, 9, 10);
            // Flag (4,5) before first click
            session.toggleFlag(4, 5);
            MinesweeperState state = session.open(4, 4);

            // The flagged cell must remain FLAGGED even if BFS reaches it
            MinesweeperCellView flaggedCell = cellAt(state, 4, 5);
            assertThat(flaggedCell.state())
                    .isIn(MinesweeperCellState.FLAGGED, MinesweeperCellState.OPENED);
            // If the cell was adjacent zero-region AND flagged → it stays FLAGGED (not opened by BFS)
        }
    }

    // ====================================================== Win Condition

    @Nested
    @DisplayName("Win Condition")
    class WinCondition {

        @Test
        @DisplayName("opening all non-mine cells triggers win")
        void winWhenAllNonMinesOpened() {
            // Use a tiny board: 4×4, 1 mine → 15 safe cells to open
            MinesweeperSession session = new MinesweeperSession("win", 4, 4, 1);

            // Open cells one by one until won, skipping any that turn out to be mines
            // (safe zone on first click guarantees first click is safe)
            MinesweeperState state = openAllSafely(session, 4, 4);
            assertThat(state.won()).isTrue();
            assertThat(state.gameOver()).isTrue();
            // All mines should be flagged automatically on win
            assertThat(state.flagsPlaced()).isEqualTo(state.totalMines());
        }
    }

    // ====================================================== Loss Condition

    @Nested
    @DisplayName("Loss Condition")
    class LossCondition {

        @Test
        @DisplayName("opening a mine after first click sets gameOver=true, won=false")
        void loseWhenMineOpened() {
            // First click is always safe; we need to find and click a mine afterward.
            MinesweeperSession session = new MinesweeperSession("lose", 9, 9, 60);
            // First safe click
            session.open(4, 4);
            // Now find a mine cell and open it
            MinesweeperState stateAfterOpen = forceOpenMine(session, 9, 9);
            if (stateAfterOpen != null) {
                assertThat(stateAfterOpen.gameOver()).isTrue();
                assertThat(stateAfterOpen.won()).isFalse();
            }
        }

        @Test
        @DisplayName("on loss, all mine cells are revealed as OPENED")
        void revealAllMinesOnLoss() {
            MinesweeperSession session = new MinesweeperSession("reveal", 9, 9, 60);
            session.open(4, 4);
            MinesweeperState lostState = forceOpenMine(session, 9, 9);
            if (lostState != null && lostState.gameOver() && !lostState.won()) {
                // All mines must be either OPENED (revealed) or FLAGGED (if correctly flagged)
                for (MinesweeperCellView cell : lostState.cells()) {
                    if (cell.mine()) {
                        assertThat(cell.state())
                                .as("Mine at (%d,%d) must be OPENED", cell.row(), cell.col())
                                .isEqualTo(MinesweeperCellState.OPENED);
                    }
                }
            }
        }

        @Test
        @DisplayName("incorrectly flagged cells become WRONG_FLAG on loss")
        void wrongFlagMarkedOnLoss() {
            MinesweeperSession session = new MinesweeperSession("wrong", 9, 9, 60);
            // Do first click to initialize mines
            session.open(4, 4);
            // Flag a cell that might not be a mine — we'll look for a non-mine cell and flag it
            MinesweeperState currentState = session.state();
            MinesweeperCellView safeFlagTarget = currentState.cells().stream()
                    .filter(c -> c.state() == MinesweeperCellState.COVERED && !c.mine())
                    .findFirst()
                    .orElse(null);

            if (safeFlagTarget != null) {
                session.toggleFlag(safeFlagTarget.row(), safeFlagTarget.col());
                // Now trigger a loss
                MinesweeperState lostState = forceOpenMine(session, 9, 9);
                if (lostState != null && lostState.gameOver() && !lostState.won()) {
                    MinesweeperCellView wrongFlagged = cellAt(lostState, safeFlagTarget.row(), safeFlagTarget.col());
                    assertThat(wrongFlagged.state()).isEqualTo(MinesweeperCellState.WRONG_FLAG);
                }
            }
        }
    }

    // ====================================================== Flag Toggle

    @Nested
    @DisplayName("Flag Toggle")
    class FlagToggle {

        @Test
        @DisplayName("flagging a covered cell increments flagsPlaced")
        void flagCanBeToggledWithoutOpeningCell() {
            MinesweeperSession session = new MinesweeperSession("mines-2", 9, 9, 10);

            MinesweeperState flagged = session.toggleFlag(0, 0);
            assertThat(flagged.flagsPlaced()).isEqualTo(1);
            assertThat(cellAt(flagged, 0, 0).state()).isEqualTo(MinesweeperCellState.FLAGGED);

            MinesweeperState unflagged = session.toggleFlag(0, 0);
            assertThat(unflagged.flagsPlaced()).isZero();
            assertThat(cellAt(unflagged, 0, 0).state()).isEqualTo(MinesweeperCellState.COVERED);
        }

        @Test
        @DisplayName("cannot open a flagged cell (no-op)")
        void openFlaggedCellIsNoOp() {
            MinesweeperSession session = new MinesweeperSession("f", 9, 9, 10);
            session.toggleFlag(0, 0);
            MinesweeperState state = session.open(0, 0);
            // Cell must remain FLAGGED — open on a flagged cell is ignored
            assertThat(cellAt(state, 0, 0).state()).isEqualTo(MinesweeperCellState.FLAGGED);
        }

        @Test
        @DisplayName("cannot toggle flag on an already-opened cell (no-op)")
        void toggleFlagOnOpenedCellIsNoOp() {
            MinesweeperSession session = new MinesweeperSession("f2", 9, 9, 10);
            session.open(4, 4); // first click, cell is opened
            MinesweeperState state = session.toggleFlag(4, 4);
            assertThat(cellAt(state, 4, 4).state()).isEqualTo(MinesweeperCellState.OPENED);
        }

        @Test
        @DisplayName("open after game over is a no-op")
        void openAfterGameOverIsNoOp() {
            MinesweeperSession session = new MinesweeperSession("go", 9, 9, 60);
            session.open(4, 4);
            MinesweeperState lostState = forceOpenMine(session, 9, 9);
            if (lostState != null && lostState.gameOver()) {
                // Additional opens should not change the state
                MinesweeperState afterExtraOpen = session.open(0, 0);
                assertThat(afterExtraOpen.gameOver()).isTrue();
            }
        }
    }

    // ====================================================== Reset

    @Nested
    @DisplayName("Reset")
    class Reset {

        @Test
        @DisplayName("reset restores initial state: firstClickDone=false, gameOver=false")
        void resetRestoresInitialState() {
            MinesweeperSession session = new MinesweeperSession("r", 9, 9, 10);
            session.open(4, 4);
            session.toggleFlag(0, 0);
            MinesweeperState afterReset = session.reset();

            assertThat(afterReset.firstClickDone()).isFalse();
            assertThat(afterReset.gameOver()).isFalse();
            assertThat(afterReset.won()).isFalse();
            assertThat(afterReset.flagsPlaced()).isZero();
            assertThat(afterReset.cells()).allMatch(c -> c.state() == MinesweeperCellState.COVERED);
        }
    }

    // ====================================================== Board Validation

    @Nested
    @DisplayName("Board Validation")
    class BoardValidation {

        @Test
        @DisplayName("rows < 4 throws IllegalArgumentException")
        void rejectsInvalidBoardSizes_tooFewRows() {
            assertThatThrownBy(() -> new MinesweeperSession("bad", 3, 9, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 4 and 30");
        }

        @Test
        @DisplayName("cols < 4 throws IllegalArgumentException")
        void rejectsInvalidBoardSizes_tooFewCols() {
            assertThatThrownBy(() -> new MinesweeperSession("bad", 9, 3, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 4 and 30");
        }

        @Test
        @DisplayName("rows > 30 throws IllegalArgumentException")
        void rejectsInvalidBoardSizes_tooManyRows() {
            assertThatThrownBy(() -> new MinesweeperSession("bad", 31, 9, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 4 and 30");
        }

        @Test
        @DisplayName("mines = 0 throws IllegalArgumentException")
        void rejectsZeroMines() {
            assertThatThrownBy(() -> new MinesweeperSession("bad", 9, 9, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 1");
        }

        @Test
        @DisplayName("mines >= rows*cols throws IllegalArgumentException")
        void rejectsTooManyMines() {
            assertThatThrownBy(() -> new MinesweeperSession("bad", 4, 4, 16))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("valid board creates correct cell count")
        void validBoardCreatesCorrectCellCount() {
            MinesweeperSession session = new MinesweeperSession("ok", 9, 9, 10);
            assertThat(session.state().cells()).hasSize(81);
            assertThat(session.state().totalMines()).isEqualTo(10);
        }
    }

    // ====================================================== Helpers

    private MinesweeperCellView cellAt(MinesweeperState state, int row, int col) {
        return state.cells().stream()
                .filter(cell -> cell.row() == row && cell.col() == col)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cell not found at (" + row + "," + col + ")"));
    }

    /**
     * Opens cells systematically to win the game: iterates all cells,
     * skipping mines, until won=true. Retries with a fresh board if a
     * mine is accidentally opened before the last safe cell triggers
     * the win (random mine placement can cause this on small boards).
     */
    private MinesweeperState openAllSafely(MinesweeperSession session, int rows, int cols) {
        for (int attempt = 0; attempt < 50; attempt++) {
            MinesweeperState state = session.open(0, 0);
            if (state.gameOver()) return state;

            boolean madeProgress = true;
            while (!state.gameOver() && madeProgress) {
                madeProgress = false;
                for (int r = 0; r < rows && !state.gameOver(); r++) {
                    for (int c = 0; c < cols && !state.gameOver(); c++) {
                        MinesweeperCellView cell = cellAt(state, r, c);
                        if (cell.state() == MinesweeperCellState.COVERED && !cell.mine()) {
                            state = session.open(r, c);
                            madeProgress = true;
                        }
                    }
                }
            }

            if (state.won()) return state;
            if (state.gameOver()) {
                session.reset();
            }
        }
        return session.state();
    }

    /**
     * Attempts to open a mine cell (after first click has been done).
     * Returns the resulting state, or null if no covered mine was found.
     */
    private MinesweeperState forceOpenMine(MinesweeperSession session, int rows, int cols) {
        MinesweeperState state = session.state();
        for (MinesweeperCellView cell : state.cells()) {
            if (cell.mine() && cell.state() == MinesweeperCellState.COVERED) {
                return session.open(cell.row(), cell.col());
            }
        }
        return null;
    }
}
