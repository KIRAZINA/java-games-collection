package com.KIRA_ZINA.app.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinesweeperModel Tests")
class MinesweeperModelTest {

    @Mock
    private MinesweeperModel.GameListener mockListener;

    private MinesweeperModel model;

    @BeforeEach
    void setUp() {
        model = new MinesweeperModel(9, 9, 10);
        model.addListener(mockListener);
    }

    @Nested
    @DisplayName("Model Initialization")
    class ModelInitialization {

        @Test
        @DisplayName("Should initialize with correct dimensions")
        void shouldInitializeWithCorrectDimensions() {
            assertThat(model.getRows()).isEqualTo(9);
            assertThat(model.getCols()).isEqualTo(9);
        }

        @Test
        @DisplayName("Should initialize with correct mine count")
        void shouldInitializeWithCorrectMineCount() {
            assertThat(model.getTotalMines()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should initialize all cells as covered")
        void shouldInitializeAllCellsAsCovered() {
            for (int r = 0; r < model.getRows(); r++) {
                for (int c = 0; c < model.getCols(); c++) {
                    assertThat(model.getCell(r, c).isCovered()).isTrue();
                }
            }
        }

        @Test
        @DisplayName("Should initialize with no mines placed")
        void shouldInitializeWithNoMinesPlaced() {
            int mineCount = 0;
            for (int r = 0; r < model.getRows(); r++) {
                for (int c = 0; c < model.getCols(); c++) {
                    if (model.getCell(r, c).isMine()) {
                        mineCount++;
                    }
                }
            }
            assertThat(mineCount).isEqualTo(0);
        }

        @Test
        @DisplayName("Should initialize with correct game state")
        void shouldInitializeWithCorrectGameState() {
            assertThat(model.isGameOver()).isFalse();
            assertThat(model.isWon()).isFalse();
            assertThat(model.isFirstClickDone()).isFalse();
            assertThat(model.getFlagsPlaced()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Cell Access")
    class CellAccess {

        @Test
        @DisplayName("Should return valid cell for valid coordinates")
        void shouldReturnValidCellForValidCoordinates() {
            Cell cell = model.getCell(0, 0);
            assertThat(cell).isNotNull();
            assertThat(cell.getRow()).isEqualTo(0);
            assertThat(cell.getCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return valid cell for boundary coordinates")
        void shouldReturnValidCellForBoundaryCoordinates() {
            Cell cell1 = model.getCell(0, 8);
            Cell cell2 = model.getCell(8, 0);
            Cell cell3 = model.getCell(8, 8);

            assertThat(cell1).isNotNull();
            assertThat(cell2).isNotNull();
            assertThat(cell3).isNotNull();
        }

        @Test
        @DisplayName("Should validate coordinates correctly")
        void shouldValidateCoordinatesCorrectly() {
            assertThat(model.isValid(0, 0)).isTrue();
            assertThat(model.isValid(4, 4)).isTrue();
            assertThat(model.isValid(8, 8)).isTrue();

            assertThat(model.isValid(-1, 0)).isFalse();
            assertThat(model.isValid(0, -1)).isFalse();
            assertThat(model.isValid(9, 0)).isFalse();
            assertThat(model.isValid(0, 9)).isFalse();
            assertThat(model.isValid(-1, -1)).isFalse();
            assertThat(model.isValid(9, 9)).isFalse();
        }
    }

    @Nested
    @DisplayName("Game Mechanics")
    class GameMechanics {

        @Test
        @DisplayName("Should place mines on first click")
        void shouldPlaceMinesOnFirstClick() {
            assertThat(model.isFirstClickDone()).isFalse();

            model.openCell(4, 4); // First click

            assertThat(model.isFirstClickDone()).isTrue();

            // Count mines
            int mineCount = 0;
            for (int r = 0; r < model.getRows(); r++) {
                for (int c = 0; c < model.getCols(); c++) {
                    if (model.getCell(r, c).isMine()) {
                        mineCount++;
                    }
                }
            }
            assertThat(mineCount).isEqualTo(10);
        }

        @Test
        @DisplayName("Should not place mine on first click location")
        void shouldNotPlaceMineOnFirstClickLocation() {
            model.openCell(4, 4); // First click
            assertThat(model.getCell(4, 4).isMine()).isFalse();
        }

        @Test
        @DisplayName("Should calculate adjacent mines correctly")
        void shouldCalculateAdjacentMinesCorrectly() {
            // Create a custom model for controlled testing
            MinesweeperModel testModel = new MinesweeperModel(3, 3, 1);
            testModel.openCell(0, 0); // Place mines

            // Find the mine and check adjacent counts
            Cell mineCell = null;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (testModel.getCell(r, c).isMine()) {
                        mineCell = testModel.getCell(r, c);
                        break;
                    }
                }
            }

            assertThat(mineCell).isNotNull();

            // Check adjacent cells have correct count
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    Cell cell = testModel.getCell(r, c);
                    if (!cell.isMine()) {
                        int expectedCount = calculateExpectedAdjacentCount(testModel, r, c);
                        assertThat(cell.getAdjacentMinesCount())
                            .isEqualTo(expectedCount);
                    }
                }
            }
        }

        private int calculateExpectedAdjacentCount(MinesweeperModel model, int row, int col) {
            int count = 0;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nr = row + dr, nc = col + dc;
                    if (model.isValid(nr, nc) && model.getCell(nr, nc).isMine()) {
                        count++;
                    }
                }
            }
            return count;
        }
    }

    @Nested
    @DisplayName("Flag Operations")
    class FlagOperations {

        @Test
        @DisplayName("Should place flag correctly")
        void shouldPlaceFlagCorrectly() {
            int initialFlags = model.getFlagsPlaced();

            model.toggleFlag(8, 8);

            assertThat(model.getCell(8, 8).isFlagged()).isTrue();
            assertThat(model.getFlagsPlaced()).isEqualTo(initialFlags + 1);
            verify(mockListener).onCellUpdated(8, 8);
        }

        @Test
        @DisplayName("Should remove flag correctly")
        void shouldRemoveFlagCorrectly() {
            model.toggleFlag(8, 8);
            assertThat(model.getCell(8, 8).isFlagged()).isTrue();
            assertThat(model.getFlagsPlaced()).isEqualTo(1);

            model.toggleFlag(8, 8); // Remove flag

            assertThat(model.getCell(8, 8).isCovered()).isTrue();
            assertThat(model.getFlagsPlaced()).isEqualTo(0);
            verify(mockListener, times(2)).onCellUpdated(8, 8);
        }

        @Test
        @DisplayName("Should not flag opened cells")
        void shouldNotFlagOpenedCells() {
            MinesweeperModel customModel = new MinesweeperModel(3, 3, 0);
            customModel.openCell(0, 0); // Everything opens

            int initialFlags = customModel.getFlagsPlaced();
            customModel.toggleFlag(1, 1);

            assertThat(customModel.getCell(1, 1).isFlagged()).isFalse();
            assertThat(customModel.getFlagsPlaced()).isEqualTo(initialFlags);
        }

        @Test
        @DisplayName("Should calculate remaining mines correctly")
        void shouldCalculateRemainingMinesCorrectly() {
            int totalMines = model.getTotalMines();

            model.toggleFlag(8, 8);
            model.toggleFlag(7, 7);

            assertThat(model.getRemainingMines()).isEqualTo(totalMines - 2);
        }
    }

    @Nested
    @DisplayName("Win/Loss Conditions")
    class WinLossConditions {

        @Test
        @DisplayName("Should lose when clicking on mine")
        void shouldLoseWhenClickingOnMine() {
            model.openCell(0, 0); // Initialize mines

            // Find and click on a mine
            for (int r = 0; r < model.getRows(); r++) {
                for (int c = 0; c < model.getCols(); c++) {
                    if (model.getCell(r, c).isMine()) {
                        model.openCell(r, c);
                        assertThat(model.isGameOver()).isTrue();
                        assertThat(model.isWon()).isFalse();
                        verify(mockListener).onGameStateChanged(false, true);
                        return;
                    }
                }
            }
            fail("No mine found to test loss condition");
        }

        @Test
        @DisplayName("Should win when all non-mine cells are opened")
        void shouldWinWhenAllNonMineCellsAreOpened() {
            // Create a simple 3x3 model with 1 mine for easier testing
            MinesweeperModel winModel = new MinesweeperModel(3, 3, 1);
            winModel.addListener(mockListener);

            // Initialize mines
            winModel.openCell(0, 0);

            // Open all non-mine cells
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (!winModel.getCell(r, c).isMine()) {
                        winModel.openCell(r, c);
                    }
                }
            }

            assertThat(winModel.isGameOver()).isTrue();
            assertThat(winModel.isWon()).isTrue();
            verify(mockListener).onGameStateChanged(true, false);
        }
    }

    @Nested
    @DisplayName("Flood Fill Algorithm")
    class FloodFillAlgorithm {

        @Test
        @DisplayName("Should open adjacent empty cells")
        void shouldOpenAdjacentEmptyCells() {
            // Create a model with no mines for testing flood fill
            MinesweeperModel floodModel = new MinesweeperModel(3, 3, 0);
            floodModel.openCell(1, 1); // Open center cell

            // All cells should be opened due to flood fill
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    assertThat(floodModel.getCell(r, c).isOpened()).isTrue();
                }
            }
        }

        @Test
        @DisplayName("Should stop flood fill at numbered cells")
        void shouldStopFloodFillAtNumberedCells() {
            // Create a model with mines to test boundary behavior
            MinesweeperModel boundaryModel = new MinesweeperModel(3, 3, 1);
            boundaryModel.openCell(0, 0); // Initialize mines

            // Find a non-mine cell and open it
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (!boundaryModel.getCell(r, c).isMine()) {
                        boundaryModel.openCell(r, c);
                        
                        // Check that flood fill worked appropriately
                        boolean hasOpenedCells = false;
                        for (int rr = 0; rr < 3; rr++) {
                            for (int cc = 0; cc < 3; cc++) {
                                if (boundaryModel.getCell(rr, cc).isOpened()) {
                                    hasOpenedCells = true;
                                    break;
                                }
                            }
                        }
                        assertThat(hasOpenedCells).isTrue();
                        return;
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Listener Management")
    class ListenerManagement {

        @Test
        @DisplayName("Should add and notify listeners")
        void shouldAddAndNotifyListeners() {
            MinesweeperModel.GameListener listener1 = mock(MinesweeperModel.GameListener.class);
            MinesweeperModel.GameListener listener2 = mock(MinesweeperModel.GameListener.class);

            MinesweeperModel testModel = new MinesweeperModel(3, 3, 1);
            testModel.addListener(listener1);
            testModel.addListener(listener2);

            testModel.openCell(0, 0); // Should notify all listeners

            verify(listener1).onCellUpdated(0, 0);
            verify(listener2).onCellUpdated(0, 0);
        }

        @Test
        @DisplayName("Should handle multiple listener notifications")
        void shouldHandleMultipleListenerNotifications() {
            AtomicInteger notificationCount = new AtomicInteger(0);
            MinesweeperModel.GameListener countingListener = new MinesweeperModel.GameListener() {
                @Override
                public void onCellUpdated(int r, int c) {
                    notificationCount.incrementAndGet();
                }
                
                @Override
                public void onGameStateChanged(boolean won, boolean lost) {
                    // Not used in this test
                }
                
                @Override
                public void onFullRefresh() {
                    // Not used in this test
                }
            };

            model.addListener(countingListener);
            model.openCell(0, 0); // Initialize mines
            model.openCell(1, 1);
            model.toggleFlag(2, 2);

            assertThat(notificationCount.get()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Reset Functionality")
    class ResetFunctionality {

        @Test
        @DisplayName("Should reset game to initial state")
        void shouldResetGameToInitialState() {
            // Play some moves
            model.openCell(0, 0); // Initialize mines
            model.openCell(1, 1);
            model.toggleFlag(2, 2);

            // Reset the game
            model.reset();

            // Check initial state is restored
            assertThat(model.isGameOver()).isFalse();
            assertThat(model.isWon()).isFalse();
            assertThat(model.isFirstClickDone()).isFalse();
            assertThat(model.getFlagsPlaced()).isEqualTo(0);

            // Check all cells are covered
            for (int r = 0; r < model.getRows(); r++) {
                for (int c = 0; c < model.getCols(); c++) {
                    Cell cell = model.getCell(r, c);
                    assertThat(cell.isCovered()).isTrue();
                    assertThat(cell.isMine()).isFalse();
                    assertThat(cell.getAdjacentMinesCount()).isEqualTo(0);
                }
            }

            verify(mockListener).onFullRefresh();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @ParameterizedTest
        @CsvSource({
            "1, 1, 0",
            "1, 2, 0",
            "2, 1, 0",
            "2, 2, 1",
            "3, 3, 1",
            "5, 5, 5"
        })
        @DisplayName("Should handle various grid sizes")
        void shouldHandleVariousGridSizes(int rows, int cols, int mines) {
            MinesweeperModel testModel = new MinesweeperModel(rows, cols, mines);
            
            assertThat(testModel.getRows()).isEqualTo(rows);
            assertThat(testModel.getCols()).isEqualTo(cols);
            assertThat(testModel.getTotalMines()).isEqualTo(mines);
            assertThat(testModel.isValid(0, 0)).isTrue();
            assertThat(testModel.isValid(rows - 1, cols - 1)).isTrue();
        }

        @Test
        @DisplayName("Should handle maximum mine count")
        void shouldHandleMaximumMineCount() {
            int totalCells = 9 * 9;
            MinesweeperModel maxMineModel = new MinesweeperModel(9, 9, totalCells - 1);
            
            maxMineModel.openCell(0, 0); // Initialize mines
            
            int mineCount = 0;
            for (int r = 0; r < maxMineModel.getRows(); r++) {
                for (int c = 0; c < maxMineModel.getCols(); c++) {
                    if (maxMineModel.getCell(r, c).isMine()) {
                        mineCount++;
                    }
                }
            }
            assertThat(mineCount).isEqualTo(totalCells - 1);
        }

        @Test
        @DisplayName("Should handle zero mines")
        void shouldHandleZeroMines() {
            MinesweeperModel noMineModel = new MinesweeperModel(5, 5, 0);
            noMineModel.openCell(2, 2);
            
            // Should automatically win since there are no mines
            assertThat(noMineModel.isGameOver()).isTrue();
            assertThat(noMineModel.isWon()).isTrue();
        }
    }
}
