package com.KIRA_ZINA.app.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Cell Model Tests")
class CellTest {

    private Cell cell;

    @BeforeEach
    void setUp() {
        cell = new Cell(2, 3);
    }

    @Nested
    @DisplayName("Cell Initialization")
    class CellInitialization {

        @Test
        @DisplayName("Should initialize with correct coordinates")
        void shouldInitializeWithCorrectCoordinates() {
            assertThat(cell.getRow()).isEqualTo(2);
            assertThat(cell.getCol()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should initialize as covered and not a mine")
        void shouldInitializeAsCoveredAndNotAMine() {
            assertThat(cell.isCovered()).isTrue();
            assertThat(cell.isMine()).isFalse();
            assertThat(cell.isFlagged()).isFalse();
            assertThat(cell.isOpened()).isFalse();
            assertThat(cell.isWrongFlag()).isFalse();
            assertThat(cell.getAdjacentMinesCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should have correct initial state")
        void shouldHaveCorrectInitialState() {
            assertThat(cell.getState()).isEqualTo(Cell.State.COVERED);
        }
    }

    @Nested
    @DisplayName("Mine Operations")
    class MineOperations {

        @Test
        @DisplayName("Should set mine status correctly")
        void shouldSetMineStatusCorrectly() {
            cell.setMine(true);
            assertThat(cell.isMine()).isTrue();

            cell.setMine(false);
            assertThat(cell.isMine()).isFalse();
        }
    }

    @Nested
    @DisplayName("Adjacent Mines Count")
    class AdjacentMinesCount {

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
        @DisplayName("Should set adjacent mines count correctly")
        void shouldSetAdjacentMinesCountCorrectly(int count) {
            cell.setAdjacentMinesCount(count);
            assertThat(cell.getAdjacentMinesCount()).isEqualTo(count);
        }

        @Test
        @DisplayName("Should handle zero adjacent mines")
        void shouldHandleZeroAdjacentMines() {
            cell.setAdjacentMinesCount(0);
            assertThat(cell.getAdjacentMinesCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle maximum adjacent mines (8)")
        void shouldHandleMaximumAdjacentMines() {
            cell.setAdjacentMinesCount(8);
            assertThat(cell.getAdjacentMinesCount()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("State Management")
    class StateManagement {

        @ParameterizedTest
        @EnumSource(Cell.State.class)
        @DisplayName("Should set state correctly")
        void shouldSetStateCorrectly(Cell.State state) {
            cell.setState(state);
            assertThat(cell.getState()).isEqualTo(state);
        }

        @Test
        @DisplayName("Should correctly identify covered state")
        void shouldCorrectlyIdentifyCoveredState() {
            cell.setState(Cell.State.COVERED);
            assertThat(cell.isCovered()).isTrue();
            assertThat(cell.isOpened()).isFalse();
            assertThat(cell.isFlagged()).isFalse();
            assertThat(cell.isWrongFlag()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify opened state")
        void shouldCorrectlyIdentifyOpenedState() {
            cell.setState(Cell.State.OPENED);
            assertThat(cell.isOpened()).isTrue();
            assertThat(cell.isCovered()).isFalse();
            assertThat(cell.isFlagged()).isFalse();
            assertThat(cell.isWrongFlag()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify flagged state")
        void shouldCorrectlyIdentifyFlaggedState() {
            cell.setState(Cell.State.FLAGGED);
            assertThat(cell.isFlagged()).isTrue();
            assertThat(cell.isCovered()).isFalse();
            assertThat(cell.isOpened()).isFalse();
            assertThat(cell.isWrongFlag()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify wrong flag state")
        void shouldCorrectlyIdentifyWrongFlagState() {
            cell.setState(Cell.State.WRONG_FLAG);
            assertThat(cell.isWrongFlag()).isTrue();
            assertThat(cell.isCovered()).isFalse();
            assertThat(cell.isOpened()).isFalse();
            assertThat(cell.isFlagged()).isFalse();
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("Should transition from covered to opened")
        void shouldTransitionFromCoveredToOpened() {
            assertThat(cell.isCovered()).isTrue();
            
            cell.setState(Cell.State.OPENED);
            assertThat(cell.isOpened()).isTrue();
            assertThat(cell.isCovered()).isFalse();
        }

        @Test
        @DisplayName("Should transition from covered to flagged")
        void shouldTransitionFromCoveredToFlagged() {
            assertThat(cell.isCovered()).isTrue();
            
            cell.setState(Cell.State.FLAGGED);
            assertThat(cell.isFlagged()).isTrue();
            assertThat(cell.isCovered()).isFalse();
        }

        @Test
        @DisplayName("Should allow multiple state changes")
        void shouldAllowMultipleStateChanges() {
            cell.setState(Cell.State.FLAGGED);
            assertThat(cell.isFlagged()).isTrue();

            cell.setState(Cell.State.OPENED);
            assertThat(cell.isOpened()).isTrue();
            assertThat(cell.isFlagged()).isFalse();

            cell.setState(Cell.State.WRONG_FLAG);
            assertThat(cell.isWrongFlag()).isTrue();
            assertThat(cell.isOpened()).isFalse();
        }
    }

    @Nested
    @DisplayName("Cell Combinations")
    class CellCombinations {

        @Test
        @DisplayName("Should handle mine with adjacent count")
        void shouldHandleMineWithAdjacentCount() {
            cell.setMine(true);
            cell.setAdjacentMinesCount(3);
            
            assertThat(cell.isMine()).isTrue();
            assertThat(cell.getAdjacentMinesCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle flagged mine")
        void shouldHandleFlaggedMine() {
            cell.setMine(true);
            cell.setState(Cell.State.FLAGGED);
            
            assertThat(cell.isMine()).isTrue();
            assertThat(cell.isFlagged()).isTrue();
        }

        @Test
        @DisplayName("Should handle opened mine")
        void shouldHandleOpenedMine() {
            cell.setMine(true);
            cell.setState(Cell.State.OPENED);
            
            assertThat(cell.isMine()).isTrue();
            assertThat(cell.isOpened()).isTrue();
        }

        @Test
        @DisplayName("Should handle cell with maximum adjacent mines")
        void shouldHandleCellWithMaximumAdjacentMines() {
            cell.setAdjacentMinesCount(8);
            cell.setState(Cell.State.OPENED);
            
            assertThat(cell.getAdjacentMinesCount()).isEqualTo(8);
            assertThat(cell.isOpened()).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle multiple rapid state changes")
        void shouldHandleMultipleRapidStateChanges() {
            for (int i = 0; i < 10; i++) {
                Cell.State state = Cell.State.values()[i % Cell.State.values().length];
                cell.setState(state);
                assertThat(cell.getState()).isEqualTo(state);
            }
        }

        @Test
        @DisplayName("Should handle multiple mine status changes")
        void shouldHandleMultipleMineStatusChanges() {
            for (int i = 0; i < 10; i++) {
                boolean mineStatus = i % 2 == 0;
                cell.setMine(mineStatus);
                assertThat(cell.isMine()).isEqualTo(mineStatus);
            }
        }

        @Test
        @DisplayName("Should handle boundary coordinates")
        void shouldHandleBoundaryCoordinates() {
            Cell edgeCell = new Cell(0, 0);
            assertThat(edgeCell.getRow()).isEqualTo(0);
            assertThat(edgeCell.getCol()).isEqualTo(0);

            Cell highCoordCell = new Cell(999, 999);
            assertThat(highCoordCell.getRow()).isEqualTo(999);
            assertThat(highCoordCell.getCol()).isEqualTo(999);
        }
    }

    @Nested
    @DisplayName("Immutability of Coordinates")
    class ImmutabilityOfCoordinates {

        @Test
        @DisplayName("Should maintain immutable coordinates")
        void shouldMaintainImmutableCoordinates() {
            int originalRow = cell.getRow();
            int originalCol = cell.getCol();

            // Perform various operations
            cell.setMine(true);
            cell.setAdjacentMinesCount(5);
            cell.setState(Cell.State.OPENED);

            // Coordinates should remain unchanged
            assertThat(cell.getRow()).isEqualTo(originalRow);
            assertThat(cell.getCol()).isEqualTo(originalCol);
        }
    }
}
