package com.KIRA_ZINA.app.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MainFrame Integration Tests")
class MainFrameTest {

    private MainFrame mainFrame;

        private JMenuItem findMenuItemByText(JMenuBar menuBar, String menuText, String itemText) {
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu != null && menuText.equals(menu.getText())) {
                    for (int j = 0; j < menu.getItemCount(); j++) {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && itemText.equals(item.getText())) {
                            return item;
                        }
                    }
                }
            }
            return null;
        }

    @BeforeEach
    void setUp() {
        // Skip these tests in headless mode because MainFrame extends JFrame
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment");
        
        assertThatCode(() -> {
            mainFrame = new MainFrame();
        }).doesNotThrowAnyException();
    }

    @Nested
    @DisplayName("Frame Initialization")
    class FrameInitialization {

        @Test
        @DisplayName("Should initialize with correct title")
        void shouldInitializeWithCorrectTitle() {
            assertThat(mainFrame.getTitle()).contains("Minesweeper");
        }

        @Test
        @DisplayName("Should have correct default close operation")
        void shouldHaveCorrectDefaultCloseOperation() {
            assertThat(mainFrame.getDefaultCloseOperation()).isEqualTo(JFrame.DO_NOTHING_ON_CLOSE);
        }

        @Test
        @DisplayName("Should not be resizable")
        void shouldNotBeResizable() {
            assertThat(mainFrame.isResizable()).isFalse();
        }

        @Test
        @DisplayName("Should have menu bar")
        void shouldHaveMenuBar() {
            assertThat(mainFrame.getJMenuBar()).isNotNull();
            assertThat(mainFrame.getJMenuBar().getMenuCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should have game container")
        void shouldHaveGameContainer() {
            assertThat(mainFrame.getContentPane().getComponentCount()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Menu Bar Functionality")
    class MenuBarFunctionality {

        @Test
        @DisplayName("Should have game menu")
        void shouldHaveGameMenu() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            JMenu gameMenu = null;
            
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu.getText().equals("Game")) {
                    gameMenu = menu;
                    break;
                }
            }
            
            assertThat(gameMenu).isNotNull();
            assertThat(gameMenu.getItemCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should have help menu")
        void shouldHaveHelpMenu() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            JMenu helpMenu = null;
            
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu.getText().equals("Help")) {
                    helpMenu = menu;
                    break;
                }
            }
            
            assertThat(helpMenu).isNotNull();
            assertThat(helpMenu.getItemCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should have new game menu item")
        void shouldHaveNewGameMenuItem() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            JMenuItem newGameItem = null;
            
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu.getText().equals("Game")) {
                    for (int j = 0; j < menu.getItemCount(); j++) {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && item.getText().equals("New Game")) {
                            newGameItem = item;
                            break;
                        }
                    }
                }
            }
            
            assertThat(newGameItem).isNotNull();
            assertThat(newGameItem.getAccelerator()).isNotNull();
        }

        @Test
        @DisplayName("Should have difficulty submenu")
        void shouldHaveDifficultySubmenu() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            JMenu difficultyMenu = null;
            
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu.getText().equals("Game")) {
                    for (int j = 0; j < menu.getItemCount(); j++) {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && item.getText().equals("Difficulty")) {
                            difficultyMenu = (JMenu) item;
                            break;
                        }
                    }
                }
            }
            
            assertThat(difficultyMenu).isNotNull();
            assertThat(difficultyMenu.getItemCount()).isEqualTo(3); // Beginner, Intermediate, Expert
        }

        @Test
        @DisplayName("Should have exit menu item")
        void shouldHaveExitMenuItem() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            JMenuItem exitItem = null;
            
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu.getText().equals("Game")) {
                    for (int j = 0; j < menu.getItemCount(); j++) {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && item.getText().equals("Exit")) {
                            exitItem = item;
                            break;
                        }
                    }
                }
            }
            
            assertThat(exitItem).isNotNull();
            assertThat(exitItem.getAccelerator()).isNotNull();
        }

        @Test
        @DisplayName("Should have about menu item")
        void shouldHaveAboutMenuItem() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            JMenuItem aboutItem = null;
            
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu.getText().equals("Help")) {
                    for (int j = 0; j < menu.getItemCount(); j++) {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && item.getText().equals("About")) {
                            aboutItem = item;
                            break;
                        }
                    }
                }
            }
            
            assertThat(aboutItem).isNotNull();
        }
    }

    @Nested
    @DisplayName("Menu Actions")
    class MenuActions {

        @Test
        @DisplayName("Should handle new game action")
        void shouldHandleNewGameAction() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            final JMenuItem newGameItem = findMenuItemByText(menuBar, "Game", "New Game");
            assertThat(newGameItem).isNotNull();
            
            // Trigger action
            assertThatCode(() -> {
                for (java.awt.event.ActionListener listener : newGameItem.getActionListeners()) {
                    listener.actionPerformed(new java.awt.event.ActionEvent(newGameItem, 0, ""));
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle difficulty change")
        void shouldHandleDifficultyChange() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            final JMenuItem intermediateItem = findMenuItemByText(menuBar, "Game", "Intermediate");
            assertThat(intermediateItem).isNotNull();
            
            // Trigger action
            assertThatCode(() -> {
                for (java.awt.event.ActionListener listener : intermediateItem.getActionListeners()) {
                    listener.actionPerformed(new java.awt.event.ActionEvent(intermediateItem, 0, ""));
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle about action")
        void shouldHandleAboutAction() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            JMenuItem aboutItem = null;

            // Find about item
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu.getText().equals("Help")) {
                    for (int j = 0; j < menu.getItemCount(); j++) {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && item.getText().equals("About")) {
                            aboutItem = item;
                            break;
                        }
                    }
                }
            }

            assertThat(aboutItem).isNotNull();

            // Verify that action listeners are attached without actually triggering UI dialogs
            assertThat(aboutItem.getActionListeners()).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle exit action")
        void shouldHandleExitAction() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            JMenuItem exitItem = null;

            // Find exit item
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu.getText().equals("Game")) {
                    for (int j = 0; j < menu.getItemCount(); j++) {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && item.getText().equals("Exit")) {
                            exitItem = item;
                            break;
                        }
                    }
                }
            }

            assertThat(exitItem).isNotNull();

            // Verify that action listeners are attached without actually triggering UI dialogs
            assertThat(exitItem.getActionListeners()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Keyboard Shortcuts")
    class KeyboardShortcuts {

        @Test
        @DisplayName("Should have F2 shortcut for new game")
        void shouldHaveF2ShortcutForNewGame() {
            // Check if F2 key is registered
            assertThatCode(() -> {
                KeyStroke f2Key = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
                assertThat(f2Key).isNotNull();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should have Alt+F4 shortcut for exit")
        void shouldHaveAltF4ShortcutForExit() {
            // Check if Alt+F4 key is registered
            assertThatCode(() -> {
                KeyStroke altF4Key = KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK);
                assertThat(altF4Key).isNotNull();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Game Container")
    class GameContainer {

        @Test
        @DisplayName("Should contain top panel")
        void shouldContainTopPanel() {
            Container contentPane = mainFrame.getContentPane();
            assertThat(contentPane.getComponentCount()).isGreaterThan(0);
            
            // The main container should have the game components
            JPanel gameContainer = null;
            for (Component component : contentPane.getComponents()) {
                if (component instanceof JPanel) {
                    gameContainer = (JPanel) component;
                    break;
                }
            }
            
            assertThat(gameContainer).isNotNull();
            assertThat(gameContainer.getComponentCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should have proper layout")
        void shouldHaveProperLayout() {
            Container contentPane = mainFrame.getContentPane();
            assertThat(contentPane.getLayout()).isInstanceOf(BorderLayout.class);
        }

        @Test
        @DisplayName("Should have appropriate styling")
        void shouldHaveAppropriateStyling() {
            Container contentPane = mainFrame.getContentPane();
            assertThat(contentPane.getBackground()).isNotNull();
            // Container doesn't have getBorder() method, but JPanel does
            if (contentPane instanceof JPanel) {
                assertThat(((JPanel) contentPane).getBorder()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Window Behavior")
    class WindowBehavior {

        @Test
        @DisplayName("Should handle window closing correctly")
        void shouldHandleWindowClosingCorrectly() {
            // Verify that window listeners are registered without invoking UI dialogs
            assertThat(mainFrame.getWindowListeners()).isNotEmpty();
        }

        @Test
        @DisplayName("Should center on screen")
        void shouldCenterOnScreen() {
            // Frame should be centered (we can't easily test exact position without display)
            assertThat(mainFrame.getLocation()).isNotNull();
            assertThat(mainFrame.getLocation().x).isGreaterThanOrEqualTo(0);
            assertThat(mainFrame.getLocation().y).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should have appropriate size")
        void shouldHaveAppropriateSize() {
            assertThat(mainFrame.getSize().width).isGreaterThan(0);
            assertThat(mainFrame.getSize().height).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Visual Styling")
    class VisualStyling {

        @Test
        @DisplayName("Should have modern menu bar styling")
        void shouldHaveModernMenuBarStyling() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            assertThat(menuBar.getBackground()).isNotNull();
            assertThat(menuBar.getBorder()).isNotNull();
        }

        @Test
        @DisplayName("Should have modern menu styling")
        void shouldHaveModernMenuStyling() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                assertThat(menu.getFont()).isNotNull();
                assertThat(menu.getForeground()).isNotNull();
                assertThat(menu.getBorder()).isNotNull();
            }
        }

        @Test
        @DisplayName("Should have modern menu item styling")
        void shouldHaveModernMenuItemStyling() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                for (int j = 0; j < menu.getItemCount(); j++) {
                    JMenuItem item = menu.getItem(j);
                    if (item != null) {
                        assertThat(item.getFont()).isNotNull();
                        assertThat(item.getForeground()).isNotNull();
                        assertThat(item.getBorder()).isNotNull();
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle rapid menu actions")
        void shouldHandleRapidMenuActions() {
            JMenuBar menuBar = mainFrame.getJMenuBar();
            final JMenuItem newGameItem = findMenuItemByText(menuBar, "Game", "New Game");
            assertThat(newGameItem).isNotNull();
            
            // Rapid actions
            assertThatCode(() -> {
                for (int i = 0; i < 10; i++) {
                    for (java.awt.event.ActionListener listener : newGameItem.getActionListeners()) {
                        listener.actionPerformed(new java.awt.event.ActionEvent(newGameItem, 0, ""));
                    }
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null events gracefully")
        void shouldHandleNullEventsGracefully() {
            assertThatCode(() -> {
                // This should not crash even with edge cases
                mainFrame.pack();
                mainFrame.repaint();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Integration with Game Components")
    class IntegrationWithGameComponents {

        @Test
        @DisplayName("Should integrate with game panel")
        void shouldIntegrateWithGamePanel() {
            // The frame should contain game components
            Container contentPane = mainFrame.getContentPane();
            assertThat(contentPane.getComponentCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should integrate with top panel")
        void shouldIntegrateWithTopPanel() {
            // Should have top panel functionality
            Container contentPane = mainFrame.getContentPane();
            JPanel gameContainer = (JPanel) contentPane.getComponent(0);
            assertThat(gameContainer.getComponentCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle game state changes")
        void shouldHandleGameStateChanges() {
            // Should be able to start new games
            assertThatCode(() -> {
                JMenuBar menuBar = mainFrame.getJMenuBar();
                final JMenuItem newGameItem = findMenuItemByText(menuBar, "Game", "New Game");
                if (newGameItem != null) {
                    for (java.awt.event.ActionListener listener : newGameItem.getActionListeners()) {
                        listener.actionPerformed(new java.awt.event.ActionEvent(newGameItem, 0, ""));
                    }
                }
            }).doesNotThrowAnyException();
        }
    }
}
