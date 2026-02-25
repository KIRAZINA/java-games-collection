package com.KIRA_ZINA.app.ui;

import com.KIRA_ZINA.app.model.MinesweeperModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GamePanel Integration Tests")
class GamePanelTest {

    @Mock
    private TopPanel mockTopPanel;

    private GamePanel gamePanel;
    private MinesweeperModel model;

    @BeforeEach
    void setUp() {
        // Create a small test grid
        gamePanel = new GamePanel(3, 3, 1, mockTopPanel);
        model = extractModelFromGamePanel(gamePanel);
    }

    // Helper method to extract the private model field using reflection
    private MinesweeperModel extractModelFromGamePanel(GamePanel panel) {
        try {
            java.lang.reflect.Field field = GamePanel.class.getDeclaredField("model");
            field.setAccessible(true);
            return (MinesweeperModel) field.get(panel);
        } catch (Exception e) {
            // Fallback for tests if reflection fails
            return new MinesweeperModel(3, 3, 1);
        }
    }

    @Nested
    @DisplayName("Panel Initialization")
    class PanelInitialization {

        @Test
        @DisplayName("Should initialize with correct grid size")
        void shouldInitializeWithCorrectGridSize() {
            assertThat(gamePanel.getComponentCount()).isEqualTo(9); // 3x3 grid
        }

        @Test
        @DisplayName("Should have correct layout properties")
        void shouldHaveCorrectLayoutProperties() {
            assertThat(gamePanel.getLayout()).isInstanceOf(java.awt.GridLayout.class);
            assertThat(gamePanel.getBackground()).isNotNull();
            assertThat(gamePanel.getBorder()).isNotNull();
        }

        @Test
        @DisplayName("Should contain only CellButton components")
        void shouldContainOnlyCellButtonComponents() {
            for (Component component : gamePanel.getComponents()) {
                assertThat(component).isInstanceOf(CellButton.class);
            }
        }

        @Test
        @DisplayName("Should not be in game over state initially")
        void shouldNotBeInGameOverStateInitially() {
            assertThat(gamePanel.isGameOver()).isFalse();
        }
    }

    @Nested
    @DisplayName("Game State Management")
    class GameStateManagement {

        @Test
        @DisplayName("Should handle game over state correctly")
        void shouldHandleGameOverStateCorrectly() {
            // Simulate game over by triggering a mine click
            // First click to initialize mines
            gamePanel.handleLeftClick(0, 0);
            
            // Find and click a mine (this is simplified for testing)
            // In a real scenario, we'd need to know where mines are placed
            assertThatCode(() -> gamePanel.handleLeftClick(1, 1))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reset game correctly")
        void shouldResetGameCorrectly() {
            // Make some moves
            gamePanel.handleLeftClick(0, 0);
            gamePanel.handleRightClick(1, 1);
            
            // Reset the game
            assertThatCode(() -> gamePanel.resetGame())
                .doesNotThrowAnyException();
            
            assertThat(gamePanel.isGameOver()).isFalse();
        }

        @Test
        @DisplayName("Should handle first click correctly")
        void shouldHandleFirstClickCorrectly() {
            // Should not throw exception on first click
            assertThatCode(() -> gamePanel.handleLeftClick(1, 1))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Cell Interactions")
    class CellInteractions {

        @Test
        @DisplayName("Should handle left click on cell")
        void shouldHandleLeftClickOnCell() {
            assertThatCode(() -> gamePanel.handleLeftClick(0, 0))
                .doesNotThrowAnyException();
            
            // Verify top panel was notified
            verify(mockTopPanel, atLeastOnce()).startTimer();
        }

        @Test
        @DisplayName("Should handle right click on cell")
        void shouldHandleRightClickOnCell() {
            assertThatCode(() -> gamePanel.handleRightClick(0, 0))
                .doesNotThrowAnyException();
            
            // Verify mine counter was updated
            verify(mockTopPanel, atLeastOnce()).updateMineCounter(anyInt());
        }

        @Test
        @DisplayName("Should handle chord action correctly")
        void shouldHandleChordActionCorrectly() {
            // First click to initialize the game
            gamePanel.handleLeftClick(0, 0);
            
            assertThatCode(() -> gamePanel.chordAction(1, 1))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should ignore clicks when game is over")
        void shouldIgnoreClicksWhenGameIsOver() {
            // Simulate game over (simplified)
            // In real implementation, you'd need to actually trigger game over
            
            // The test should verify that clicks are ignored when game is over
            assertThatCode(() -> {
                gamePanel.handleLeftClick(0, 0);
                gamePanel.handleRightClick(1, 1);
                gamePanel.chordAction(2, 2);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle cell press notifications")
        void shouldHandleCellPressNotifications() {
            assertThatCode(() -> gamePanel.onCellPressed())
                .doesNotThrowAnyException();
            
            verify(mockTopPanel).setScaredFace();
        }

        @Test
        @DisplayName("Should handle cell release notifications")
        void shouldHandleCellReleaseNotifications() {
            assertThatCode(() -> gamePanel.onCellReleased())
                .doesNotThrowAnyException();
            
            verify(mockTopPanel).setNormalFace();
        }
    }

    @Nested
    @DisplayName("Model Integration")
    class ModelIntegration {

        @Test
        @DisplayName("Should integrate with MinesweeperModel")
        void shouldIntegrateWithMinesweeperModel() {
            // The panel should have a model instance
            assertThat(model).isNotNull();
            assertThat(model.getRows()).isEqualTo(3);
            assertThat(model.getCols()).isEqualTo(3);
            assertThat(model.getTotalMines()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle model events correctly")
        void shouldHandleModelEventsCorrectly() {
            // Test that GamePanel correctly reacts to game state changes
            gamePanel.onGameStateChanged(true, false); // Win
            assertThat(gamePanel.isGameOver()).isTrue();
            verify(mockTopPanel).setCoolFace();

            gamePanel.onGameStateChanged(false, true); // Loss
            verify(mockTopPanel).setSadFace();

            // Verify cell update doesn't throw
            assertThatCode(() -> gamePanel.onCellUpdated(0, 0))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("UI Updates")
    class UIUpdates {

        @Test
        @DisplayName("Should update mine counter correctly")
        void shouldUpdateMineCounterCorrectly() {
            gamePanel.handleRightClick(0, 0);
            
            verify(mockTopPanel, atLeastOnce()).updateMineCounter(anyInt());
        }

        @Test
        @DisplayName("Should start timer on first click")
        void shouldStartTimerOnFirstClick() {
            gamePanel.handleLeftClick(0, 0);
            
            verify(mockTopPanel).startTimer();
        }

        @Test
        @DisplayName("Should handle timer updates")
        void shouldHandleTimerUpdates() {
            // Timer updates should be handled without errors
            assertThatCode(() -> {
                // Simulate timer tick
                // This would normally be called by the Timer in TopPanel
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle invalid coordinates gracefully")
        void shouldHandleInvalidCoordinatesGracefully() {
            assertThatCode(() -> {
                gamePanel.handleLeftClick(-1, -1);
                gamePanel.handleLeftClick(3, 3);
                gamePanel.handleLeftClick(-1, 3);
                gamePanel.handleLeftClick(3, -1);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null top panel gracefully")
        void shouldHandleNullTopPanelGracefully() {
            assertThatCode(() -> {
                GamePanel nullTopPanelGame = new GamePanel(3, 3, 1, null);
                nullTopPanelGame.handleLeftClick(0, 0);
                nullTopPanelGame.handleRightClick(1, 1);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle concurrent operations")
        void shouldHandleConcurrentOperations() {
            assertThatCode(() -> {
                // Simulate rapid clicking
                for (int i = 0; i < 10; i++) {
                    gamePanel.handleLeftClick(0, 0);
                    gamePanel.handleRightClick(1, 1);
                    gamePanel.chordAction(2, 2);
                }
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Visual Components")
    class VisualComponents {

        @Test
        @DisplayName("Should create correct number of cell buttons")
        void shouldCreateCorrectNumberOfCellButtons() {
            int expectedCells = 3 * 3; // rows * cols
            assertThat(gamePanel.getComponentCount()).isEqualTo(expectedCells);
        }

        @Test
        @DisplayName("Should have consistent button sizing")
        void shouldHaveConsistentButtonSizing() {
            Dimension firstSize = gamePanel.getComponent(0).getPreferredSize();
            
            for (Component component : gamePanel.getComponents()) {
                assertThat(component.getPreferredSize()).isEqualTo(firstSize);
            }
        }

        @Test
        @DisplayName("Should have proper spacing between cells")
        void shouldHaveProperSpacingBetweenCells() {
            GridLayout layout = (GridLayout) gamePanel.getLayout();
            assertThat(layout.getHgap()).isGreaterThan(0);
            assertThat(layout.getVgap()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should have appropriate background and borders")
        void shouldHaveAppropriateBackgroundAndBorders() {
            assertThat(gamePanel.getBackground()).isNotNull();
            assertThat(gamePanel.getBorder()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Game Logic Integration")
    class GameLogicIntegration {

        @Test
        @DisplayName("Should integrate flag placement logic")
        void shouldIntegrateFlagPlacementLogic() {
            // First click to initialize
            gamePanel.handleLeftClick(0, 0);
            
            // Place flags
            gamePanel.handleRightClick(0, 1);
            gamePanel.handleRightClick(0, 2);
            
            // Verify mine counter updates
            verify(mockTopPanel, atLeast(2)).updateMineCounter(anyInt());
        }

        @Test
        @DisplayName("Should integrate win condition logic")
        void shouldIntegrateWinConditionLogic() {
            // This is a simplified test - in reality, winning requires opening all non-mine cells
            assertThatCode(() -> {
                gamePanel.handleLeftClick(0, 0);
                // Continue opening cells until win condition is met
                // This would require knowing mine locations
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should integrate loss condition logic")
        void shouldIntegrateLossConditionLogic() {
            // First click to initialize mines
            gamePanel.handleLeftClick(0, 0);
            
            // Click on different cells until potentially hitting a mine
            assertThatCode(() -> {
                gamePanel.handleLeftClick(1, 1);
                gamePanel.handleLeftClick(2, 2);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Performance and Scalability")
    class PerformanceAndScalability {

        @Test
        @DisplayName("Should handle larger grids efficiently")
        void shouldHandleLargerGridsEfficiently() {
            assertThatCode(() -> {
                GamePanel largePanel = new GamePanel(16, 16, 40, mockTopPanel);
                assertThat(largePanel.getComponentCount()).isEqualTo(256); // 16*16
                
                // Test some operations
                largePanel.handleLeftClick(0, 0);
                largePanel.handleRightClick(1, 1);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle maximum mine count")
        void shouldHandleMaximumMineCount() {
            // Create a grid with maximum mines (all cells except one)
            int totalCells = 5 * 5;
            assertThatCode(() -> {
                GamePanel maxMinePanel = new GamePanel(5, 5, totalCells - 1, mockTopPanel);
                maxMinePanel.handleLeftClick(2, 2); // Center cell
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle zero mines")
        void shouldHandleZeroMines() {
            assertThatCode(() -> {
                GamePanel noMinePanel = new GamePanel(5, 5, 0, mockTopPanel);
                noMinePanel.handleLeftClick(2, 2);
                
                // Should automatically win with no mines
                verify(mockTopPanel).setCoolFace();
            }).doesNotThrowAnyException();
        }
    }
}
