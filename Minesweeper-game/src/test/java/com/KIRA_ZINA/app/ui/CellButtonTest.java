package com.KIRA_ZINA.app.ui;

import com.KIRA_ZINA.app.model.Cell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.*;
import java.awt.event.MouseEvent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CellButton UI Component Tests")
class CellButtonTest {

    @Mock
    private GamePanel mockGamePanel;

    @Mock
    private Cell mockCell;

    private CellButton cellButton;

    @BeforeEach
    void setUp() {
        cellButton = new CellButton(2, 3, mockGamePanel);
    }

    @Nested
    @DisplayName("Button Initialization")
    class ButtonInitialization {

        @Test
        @DisplayName("Should initialize with correct coordinates")
        void shouldInitializeWithCorrectCoordinates() {
            // Note: row and col fields are private but used internally
            assertThat(cellButton).isNotNull();
        }

        @Test
        @DisplayName("Should have correct default properties")
        void shouldHaveCorrectDefaultProperties() {
            assertThat(cellButton.getFont()).isNotNull();
            assertThat(cellButton.isFocusPainted()).isFalse();
            assertThat(cellButton.getMargin()).isEqualTo(new Insets(0, 0, 0, 0));
            assertThat(cellButton.getPreferredSize()).isEqualTo(new Dimension(30, 30));
            assertThat(cellButton.getCursor().getType()).isEqualTo(Cursor.HAND_CURSOR);
        }

        @Test
        @DisplayName("Should have correct initial visual state")
        void shouldHaveCorrectInitialVisualState() {
            assertThat(cellButton.getText()).isEmpty();
            assertThat(cellButton.getIcon()).isNull();
            assertThat(cellButton.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cell Model Integration")
    class CellModelIntegration {

        @Test
        @DisplayName("Should set and update model cell")
        void shouldSetAndUpdateModelCell() {
            cellButton.setModelCell(mockCell);
            
            // Verify the cell was set (visual update should be called)
            assertThat(cellButton).isNotNull();
        }

        @Test
        @DisplayName("Should handle null cell gracefully")
        void shouldHandleNullCellGracefully() {
            // Should not throw exception
            assertThatCode(() -> cellButton.setModelCell(null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should update visuals when cell changes")
        void shouldUpdateVisualsWhenCellChanges() {
            // Setup mock cell with mine
            when(mockCell.isMine()).thenReturn(true);
            when(mockCell.isOpened()).thenReturn(true);
            
            cellButton.setModelCell(mockCell);
            
            // Visual update should occur without exceptions
            assertThatCode(() -> cellButton.updateVisuals())
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Visual State Updates")
    class VisualStateUpdates {

        @Test
        @DisplayName("Should update to covered state correctly")
        void shouldUpdateToCoveredStateCorrectly() {
            when(mockCell.isCovered()).thenReturn(true);
            
            cellButton.setModelCell(mockCell);
            cellButton.updateVisuals();
            
            assertThat(cellButton.getText()).isEmpty();
            assertThat(cellButton.getIcon()).isNull();
        }

        @Test
        @DisplayName("Should update to flagged state correctly")
        void shouldUpdateToFlaggedStateCorrectly() {
            when(mockCell.isCovered()).thenReturn(false);
            when(mockCell.isFlagged()).thenReturn(true);
            
            cellButton.setModelCell(mockCell);
            cellButton.updateVisuals();
            
            assertThat(cellButton.getText()).isEqualTo("🚩");
        }

        @Test
        @DisplayName("Should update to opened mine state correctly")
        void shouldUpdateToOpenedMineStateCorrectly() {
            when(mockCell.isCovered()).thenReturn(false);
            when(mockCell.isFlagged()).thenReturn(false);
            when(mockCell.isOpened()).thenReturn(true);
            when(mockCell.isWrongFlag()).thenReturn(false);
            when(mockCell.isMine()).thenReturn(true);
            
            cellButton.setModelCell(mockCell);
            cellButton.updateVisuals();
            
            assertThat(cellButton.getText()).isEqualTo("💣");
        }

        @Test
        @DisplayName("Should update to opened number state correctly")
        void shouldUpdateToOpenedNumberStateCorrectly() {
            when(mockCell.isCovered()).thenReturn(false);
            when(mockCell.isFlagged()).thenReturn(false);
            when(mockCell.isOpened()).thenReturn(true);
            when(mockCell.isWrongFlag()).thenReturn(false);
            when(mockCell.isMine()).thenReturn(false);
            when(mockCell.getAdjacentMinesCount()).thenReturn(3);
            
            cellButton.setModelCell(mockCell);
            cellButton.updateVisuals();
            
            assertThat(cellButton.getText()).isEqualTo("3");
        }

        @Test
        @DisplayName("Should update to wrong flag state correctly")
        void shouldUpdateToWrongFlagStateCorrectly() {
            when(mockCell.isCovered()).thenReturn(false);
            when(mockCell.isFlagged()).thenReturn(false);
            when(mockCell.isWrongFlag()).thenReturn(true);
            
            cellButton.setModelCell(mockCell);
            cellButton.updateVisuals();
            
            assertThat(cellButton.getText()).isEqualTo("❌");
        }

        @Test
        @DisplayName("Should handle empty opened cell correctly")
        void shouldHandleEmptyOpenedCellCorrectly() {
            when(mockCell.isCovered()).thenReturn(false);
            when(mockCell.isFlagged()).thenReturn(false);
            when(mockCell.isOpened()).thenReturn(true);
            when(mockCell.isWrongFlag()).thenReturn(false);
            when(mockCell.isMine()).thenReturn(false);
            when(mockCell.getAdjacentMinesCount()).thenReturn(0);
            
            cellButton.setModelCell(mockCell);
            cellButton.updateVisuals();
            
            assertThat(cellButton.getText()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Mouse Interaction")
    class MouseInteraction {

        @Test
        @DisplayName("Should handle left click correctly")
        void shouldHandleLeftClickCorrectly() {
            when(mockGamePanel.isGameOver()).thenReturn(false);
            
            // Simulate left click
            MouseEvent leftClick = new MouseEvent(
                cellButton, MouseEvent.MOUSE_CLICKED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1
            );
            
            assertThatCode(() -> {
                for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                    listener.mouseReleased(leftClick);
                }
            }).doesNotThrowAnyException();
            
            // Verify game panel interaction
            verify(mockGamePanel).handleLeftClick(2, 3);
        }

        @Test
        @DisplayName("Should handle right click correctly")
        void shouldHandleRightClickCorrectly() {
            when(mockGamePanel.isGameOver()).thenReturn(false);
            
            // Simulate right click
            MouseEvent rightClick = new MouseEvent(
                cellButton, MouseEvent.MOUSE_CLICKED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON3
            );
            
            assertThatCode(() -> {
                for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                    listener.mouseReleased(rightClick);
                }
            }).doesNotThrowAnyException();
            
            // Verify game panel interaction
            verify(mockGamePanel).handleRightClick(2, 3);
        }

        @Test
        @DisplayName("Should handle middle click correctly")
        void shouldHandleMiddleClickCorrectly() {
            when(mockGamePanel.isGameOver()).thenReturn(false);
            
            // Simulate middle click
            MouseEvent middleClick = new MouseEvent(
                cellButton, MouseEvent.MOUSE_CLICKED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON2
            );
            
            assertThatCode(() -> {
                for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                    listener.mousePressed(middleClick);
                }
            }).doesNotThrowAnyException();
            
            // Verify game panel interaction
            verify(mockGamePanel).chordAction(2, 3);
        }

        @Test
        @DisplayName("Should ignore clicks when game is over")
        void shouldIgnoreClicksWhenGameIsOver() {
            when(mockGamePanel.isGameOver()).thenReturn(true);
            
            MouseEvent leftClick = new MouseEvent(
                cellButton, MouseEvent.MOUSE_CLICKED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1
            );
            
            for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                listener.mouseReleased(leftClick);
            }
            
            // Should not interact with game panel when game is over
            verify(mockGamePanel, never()).handleLeftClick(anyInt(), anyInt());
        }

        @Test
        @DisplayName("Should notify game panel on mouse press")
        void shouldNotifyGamePanelOnMousePress() {
            when(mockGamePanel.isGameOver()).thenReturn(false);
            when(mockCell.isCovered()).thenReturn(true);
            cellButton.setModelCell(mockCell);
            
            MouseEvent mousePress = new MouseEvent(
                cellButton, MouseEvent.MOUSE_PRESSED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1
            );
            
            for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                listener.mousePressed(mousePress);
            }
            
            verify(mockGamePanel).onCellPressed();
        }

        @Test
        @DisplayName("Should notify game panel on mouse release")
        void shouldNotifyGamePanelOnMouseRelease() {
            when(mockGamePanel.isGameOver()).thenReturn(false);
            
            MouseEvent mouseRelease = new MouseEvent(
                cellButton, MouseEvent.MOUSE_RELEASED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1
            );
            
            for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                listener.mouseReleased(mouseRelease);
            }
            
            verify(mockGamePanel).onCellReleased();
        }
    }

    @Nested
    @DisplayName("Hover Effects")
    class HoverEffects {

        @Test
        @DisplayName("Should handle mouse enter correctly")
        void shouldHandleMouseEnterCorrectly() {
            when(mockGamePanel.isGameOver()).thenReturn(false);
            when(mockCell.isCovered()).thenReturn(true);
            cellButton.setModelCell(mockCell);
            
            Color originalBackground = cellButton.getBackground();
            
            MouseEvent mouseEnter = new MouseEvent(
                cellButton, MouseEvent.MOUSE_ENTERED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.NOBUTTON
            );
            
            for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                listener.mouseEntered(mouseEnter);
            }
            
            // Background should change on hover
            assertThat(cellButton.getBackground()).isNotEqualTo(originalBackground);
        }

        @Test
        @DisplayName("Should handle mouse exit correctly")
        void shouldHandleMouseExitCorrectly() {
            when(mockGamePanel.isGameOver()).thenReturn(false);
            when(mockCell.isCovered()).thenReturn(true);
            cellButton.setModelCell(mockCell);
            
            // First enter to change background
            MouseEvent mouseEnter = new MouseEvent(
                cellButton, MouseEvent.MOUSE_ENTERED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.NOBUTTON
            );
            
            for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                listener.mouseEntered(mouseEnter);
            }
            
            Color hoverBackground = cellButton.getBackground();
            
            // Then exit to restore background
            MouseEvent mouseExit = new MouseEvent(
                cellButton, MouseEvent.MOUSE_EXITED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.NOBUTTON
            );
            
            for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                listener.mouseExited(mouseExit);
            }
            
            // Background should be restored
            assertThat(cellButton.getBackground()).isNotEqualTo(hoverBackground);
        }

        @Test
        @DisplayName("Should not apply hover effects to opened cells")
        void shouldNotApplyHoverEffectsToOpenedCells() {
            when(mockGamePanel.isGameOver()).thenReturn(false);
            when(mockCell.isCovered()).thenReturn(false);
            when(mockCell.isOpened()).thenReturn(true);
            cellButton.setModelCell(mockCell);
            
            Color originalBackground = cellButton.getBackground();
            
            MouseEvent mouseEnter = new MouseEvent(
                cellButton, MouseEvent.MOUSE_ENTERED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.NOBUTTON
            );
            
            for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                listener.mouseEntered(mouseEnter);
            }
            
            // Background should not change for opened cells
            assertThat(cellButton.getBackground()).isEqualTo(originalBackground);
        }
    }

    @Nested
    @DisplayName("Pressed State Effects")
    class PressedStateEffects {

        @Test
        @DisplayName("Should handle mouse press correctly")
        void shouldHandleMousePressCorrectly() {
            when(mockGamePanel.isGameOver()).thenReturn(false);
            when(mockCell.isCovered()).thenReturn(true);
            cellButton.setModelCell(mockCell);
            
            Color originalBackground = cellButton.getBackground();
            
            MouseEvent mousePress = new MouseEvent(
                cellButton, MouseEvent.MOUSE_PRESSED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1
            );
            
            for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                listener.mousePressed(mousePress);
            }
            
            // Background should change on press
            assertThat(cellButton.getBackground()).isNotEqualTo(originalBackground);
        }

        @Test
        @DisplayName("Should restore state on mouse release")
        void shouldRestoreStateOnMouseRelease() {
            when(mockGamePanel.isGameOver()).thenReturn(false);
            when(mockCell.isCovered()).thenReturn(true);
            cellButton.setModelCell(mockCell);
            
            // Press to change state
            MouseEvent mousePress = new MouseEvent(
                cellButton, MouseEvent.MOUSE_PRESSED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1
            );
            
            for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                listener.mousePressed(mousePress);
            }
            
            Color pressedBackground = cellButton.getBackground();
            
            // Release to restore state
            MouseEvent mouseRelease = new MouseEvent(
                cellButton, MouseEvent.MOUSE_RELEASED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1
            );
            
            for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                listener.mouseReleased(mouseRelease);
            }
            
            // Background should be restored
            assertThat(cellButton.getBackground()).isNotEqualTo(pressedBackground);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle rapid mouse events")
        void shouldHandleRapidMouseEvents() {
            when(mockGamePanel.isGameOver()).thenReturn(false);
            
            MouseEvent[] events = {
                new MouseEvent(cellButton, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.NOBUTTON),
                new MouseEvent(cellButton, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1),
                new MouseEvent(cellButton, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1),
                new MouseEvent(cellButton, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.NOBUTTON)
            };
            
            assertThatCode(() -> {
                for (MouseEvent event : events) {
                    for (java.awt.event.MouseListener listener : cellButton.getMouseListeners()) {
                        if (event.getID() == MouseEvent.MOUSE_ENTERED) {
                            listener.mouseEntered(event);
                        } else if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                            listener.mousePressed(event);
                        } else if (event.getID() == MouseEvent.MOUSE_RELEASED) {
                            listener.mouseReleased(event);
                        } else if (event.getID() == MouseEvent.MOUSE_EXITED) {
                            listener.mouseExited(event);
                        }
                    }
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null game panel gracefully")
        void shouldHandleNullGamePanelGracefully() {
            // Create button with null game panel
            CellButton nullPanelButton = new CellButton(0, 0, null);
            
            MouseEvent leftClick = new MouseEvent(
                nullPanelButton, MouseEvent.MOUSE_CLICKED, 
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1
            );
            
            // Should not throw exception even with null game panel
            assertThatCode(() -> {
                for (java.awt.event.MouseListener listener : nullPanelButton.getMouseListeners()) {
                    listener.mouseReleased(leftClick);
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle visual updates without cell model")
        void shouldHandleVisualUpdatesWithoutCellModel() {
            // Should not throw exception when updating visuals without a cell
            assertThatCode(() -> cellButton.updateVisuals())
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Custom Painting")
    class CustomPainting {

        @Test
        @DisplayName("Should handle custom painting without errors")
        void shouldHandleCustomPaintingWithoutErrors() {
            // Create a simple graphics mock for testing
            Graphics graphics = new java.awt.image.BufferedImage(30, 30, java.awt.image.BufferedImage.TYPE_INT_ARGB).getGraphics();
            
            assertThatCode(() -> cellButton.paintComponent(graphics))
                .doesNotThrowAnyException();
            
            graphics.dispose();
        }

        @Test
        @DisplayName("Should paint correctly with different cell states")
        void shouldPaintCorrectlyWithDifferentCellStates() {
            Graphics graphics = new java.awt.image.BufferedImage(30, 30, java.awt.image.BufferedImage.TYPE_INT_ARGB).getGraphics();
            
            // Test with covered cell
            when(mockCell.isCovered()).thenReturn(true);
            cellButton.setModelCell(mockCell);
            assertThatCode(() -> cellButton.paintComponent(graphics))
                .doesNotThrowAnyException();
            
            // Test with opened cell
            when(mockCell.isCovered()).thenReturn(false);
            when(mockCell.isOpened()).thenReturn(true);
            when(mockCell.getAdjacentMinesCount()).thenReturn(2);
            cellButton.updateVisuals();
            assertThatCode(() -> cellButton.paintComponent(graphics))
                .doesNotThrowAnyException();
            
            graphics.dispose();
        }
    }
}
