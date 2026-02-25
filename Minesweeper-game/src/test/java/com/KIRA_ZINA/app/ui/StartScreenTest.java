package com.KIRA_ZINA.app.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StartScreen Tests")
class StartScreenTest {

    @Mock
    private Runnable onStartGameMock;

    @Mock
    private Runnable onExitMock;

    private StartScreen startScreen;

    private JButton findButtonByText(JPanel panel, String textFragment) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                if (button.getText() != null && button.getText().contains(textFragment)) {
                    return button;
                }
            }
        }
        return null;
    }

    @BeforeEach
    void setUp() {
        startScreen = new StartScreen(onStartGameMock, onExitMock);
    }

    @Nested
    @DisplayName("Screen Initialization")
    class ScreenInitialization {

        @Test
        @DisplayName("Should initialize with correct layout")
        void shouldInitializeWithCorrectLayout() {
            assertThat(startScreen.getLayout()).isInstanceOf(BorderLayout.class);
        }

        @Test
        @DisplayName("Should have correct background and borders")
        void shouldHaveCorrectBackgroundAndBorders() {
            assertThat(startScreen.getBackground()).isNotNull();
            assertThat(startScreen.getBorder()).isNotNull();
        }

        @Test
        @DisplayName("Should contain three main sections")
        void shouldContainThreeMainSections() {
            assertThat(startScreen.getComponentCount()).isEqualTo(3);
            
            // Check that all components are JPanels
            for (Component component : startScreen.getComponents()) {
                assertThat(component).isInstanceOf(JPanel.class);
            }
        }

        @Test
        @DisplayName("Should have proper component layout")
        void shouldHaveProperComponentLayout() {
            BorderLayout layout = (BorderLayout) startScreen.getLayout();
            assertThat(layout.getLayoutComponent(BorderLayout.NORTH)).isNotNull();
            assertThat(layout.getLayoutComponent(BorderLayout.CENTER)).isNotNull();
            assertThat(layout.getLayoutComponent(BorderLayout.SOUTH)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Title Panel")
    class TitlePanel {

        @Test
        @DisplayName("Should contain title and subtitle")
        void shouldContainTitleAndSubtitle() {
            JPanel titlePanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
            
            // Should have multiple components (mine icon, title, subtitle)
            assertThat(titlePanel.getComponentCount()).isGreaterThan(0);
            
            // Check for title label
            boolean hasTitle = false;
            boolean hasSubtitle = false;
            boolean hasMineIcon = false;
            
            for (Component component : titlePanel.getComponents()) {
                if (component instanceof JLabel) {
                    JLabel label = (JLabel) component;
                    String text = label.getText();
                    if (text != null) {
                        if (text.contains("MINESWEEPER")) {
                            hasTitle = true;
                        } else if (text.contains("Modern Edition")) {
                            hasSubtitle = true;
                        } else if (text.contains("💣")) {
                            hasMineIcon = true;
                        }
                    }
                }
            }
            
            assertThat(hasTitle).isTrue();
            assertThat(hasSubtitle).isTrue();
            assertThat(hasMineIcon).isTrue();
        }

        @Test
        @DisplayName("Should have correct title styling")
        void shouldHaveCorrectTitleStyling() {
            JPanel titlePanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
            
            for (Component component : titlePanel.getComponents()) {
                if (component instanceof JLabel) {
                    JLabel label = (JLabel) component;
                    if (label.getText().contains("MINESWEEPER")) {
                        assertThat(label.getFont()).isNotNull();
                        assertThat(label.getForeground()).isNotNull();
                        assertThat(label.getHorizontalAlignment()).isEqualTo(SwingConstants.CENTER);
                        break;
                    }
                }
            }
        }

        @Test
        @DisplayName("Should have correct subtitle styling")
        void shouldHaveCorrectSubtitleStyling() {
            JPanel titlePanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
            
            for (Component component : titlePanel.getComponents()) {
                if (component instanceof JLabel) {
                    JLabel label = (JLabel) component;
                    if (label.getText().contains("Modern Edition")) {
                        assertThat(label.getFont()).isNotNull();
                        assertThat(label.getForeground()).isNotNull();
                        assertThat(label.getHorizontalAlignment()).isEqualTo(SwingConstants.CENTER);
                        break;
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Center Panel")
    class CenterPanel {

        @Test
        @DisplayName("Should contain instructions")
        void shouldContainInstructions() {
            JPanel centerPanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
            
            // Should have instruction components
            assertThat(centerPanel.getComponentCount()).isGreaterThan(0);
            
            boolean hasInstructions = false;
            boolean hasDecorativeMines = false;
            
            for (Component component : centerPanel.getComponents()) {
                if (isLabelOrPanelWithLabels(component, "How to Play")) {
                    hasInstructions = true;
                }
                if (isLabelOrPanelWithLabels(component, "🚩") || isLabelOrPanelWithLabels(component, "💣")) {
                    hasDecorativeMines = true;
                }
            }
            
            assertThat(hasInstructions).isTrue();
            assertThat(hasDecorativeMines).isTrue();
        }

        private boolean isLabelOrPanelWithLabels(Component comp, String textToFind) {
            if (comp instanceof JLabel) {
                String text = ((JLabel) comp).getText();
                return text != null && text.contains(textToFind);
            } else if (comp instanceof JPanel) {
                for (Component child : ((JPanel) comp).getComponents()) {
                    if (isLabelOrPanelWithLabels(child, textToFind)) return true;
                }
            }
            return false;
        }

        @Test
        @DisplayName("Should have correct instruction formatting")
        void shouldHaveCorrectInstructionFormatting() {
            JPanel centerPanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
            
            for (Component component : centerPanel.getComponents()) {
                if (component instanceof JLabel) {
                    JLabel label = (JLabel) component;
                    if (label.getText() != null && label.getText().contains("How to Play")) {
                        assertThat(label.getFont()).isNotNull();
                        assertThat(label.getForeground()).isNotNull();
                        assertThat(label.getHorizontalAlignment()).isEqualTo(SwingConstants.CENTER);
                        break;
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Button Panel")
    class ButtonPanel {

        @Test
        @DisplayName("Should contain start and exit buttons")
        void shouldContainStartAndExitButtons() {
            JPanel buttonPanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.SOUTH);
            
            // Should have buttons and version label
            assertThat(buttonPanel.getComponentCount()).isGreaterThanOrEqualTo(2);
            
            boolean hasStartButton = false;
            boolean hasExitButton = false;
            boolean hasVersionLabel = false;
            
            for (Component component : buttonPanel.getComponents()) {
                if (component instanceof JButton) {
                    JButton button = (JButton) component;
                    String text = button.getText();
                    if (text != null) {
                        if (text.contains("Start Game")) {
                            hasStartButton = true;
                        } else if (text.contains("Exit")) {
                            hasExitButton = true;
                        }
                    }
                } else if (component instanceof JLabel) {
                    JLabel label = (JLabel) component;
                    if (label.getText() != null && label.getText().contains("v2.0")) {
                        hasVersionLabel = true;
                    }
                }
            }
            
            assertThat(hasStartButton).isTrue();
            assertThat(hasExitButton).isTrue();
            assertThat(hasVersionLabel).isTrue();
        }

        @Test
        @DisplayName("Should have correct button styling")
        void shouldHaveCorrectButtonStyling() {
            JPanel buttonPanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.SOUTH);
            
            // Find start and exit buttons to check their cursors specifically
            final JButton startButton = findButtonByText(buttonPanel, "Start Game");
            final JButton exitButton = findButtonByText(buttonPanel, "Exit");

            for (Component component : buttonPanel.getComponents()) {
                if (component instanceof JButton) {
                    JButton button = (JButton) component;
                    assertThat(button.getFont()).isNotNull();
                    assertThat(button.getPreferredSize()).isNotNull();
                    // Original line: assertThat(button.getCursor()).isEqualTo(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    // Applying the requested fix for cursor check
                    assertThat(startButton.getCursor()).satisfies(c -> assertThat(c.getType()).isEqualTo(Cursor.HAND_CURSOR));
                    assertThat(exitButton.getCursor()).satisfies(c -> assertThat(c.getType()).isEqualTo(Cursor.HAND_CURSOR));
                    assertThat(button.isFocusPainted()).isFalse();
                }
            }
        }

        @Test
        @DisplayName("Should have correct version label styling")
        void shouldHaveCorrectVersionLabelStyling() {
            JPanel buttonPanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.SOUTH);
            
            for (Component component : buttonPanel.getComponents()) {
                if (component instanceof JLabel) {
                    JLabel label = (JLabel) component;
                    if (label.getText() != null && label.getText().contains("v2.0")) {
                        assertThat(label.getFont()).isNotNull();
                        assertThat(label.getForeground()).isNotNull();
                        assertThat(label.getHorizontalAlignment()).isEqualTo(SwingConstants.CENTER);
                        break;
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Button Functionality")
    class ButtonFunctionality {

        @Test
        @DisplayName("Should trigger start game on button click")
        void shouldTriggerStartGameOnButtonClick() {
            JPanel buttonPanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.SOUTH);

            // Find start button
            final JButton startButton = findButtonByText(buttonPanel, "Start Game");
            assertThat(startButton).isNotNull();
            
            // Simulate button click
            assertThatCode(() -> {
                for (java.awt.event.ActionListener listener : startButton.getActionListeners()) {
                    listener.actionPerformed(new java.awt.event.ActionEvent(startButton, 0, ""));
                }
            }).doesNotThrowAnyException();
            
            // Verify start game was called
            verify(onStartGameMock, atLeastOnce()).run();
        }

        @Test
        @DisplayName("Should trigger exit on button click")
        void shouldTriggerExitOnButtonClick() {
            JPanel buttonPanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.SOUTH);

            // Find exit button
            final JButton exitButton = findButtonByText(buttonPanel, "Exit");
            assertThat(exitButton).isNotNull();
            
            // Simulate button click
            assertThatCode(() -> {
                for (java.awt.event.ActionListener listener : exitButton.getActionListeners()) {
                    listener.actionPerformed(new java.awt.event.ActionEvent(exitButton, 0, ""));
                }
            }).doesNotThrowAnyException();
            
            // Verify exit was called
            verify(onExitMock, atLeastOnce()).run();
        }

        @Test
        @DisplayName("Should have keyboard shortcuts")
        void shouldHaveKeyboardShortcuts() {
            // Test Enter key for start game
            assertThatCode(() -> {
                startScreen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
                startScreen.getActionMap().get("startGame");
            }).doesNotThrowAnyException();
            
            // Test Escape key for exit
            assertThatCode(() -> {
                startScreen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
                startScreen.getActionMap().get("exit");
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should respond to Enter key")
        void shouldRespondToEnterKey() {
            // Simulate Enter key press
            assertThatCode(() -> {
                startScreen.getActionMap().get("startGame")
                    .actionPerformed(new java.awt.event.ActionEvent(startScreen, 0, ""));
            }).doesNotThrowAnyException();
            
            verify(onStartGameMock, atLeastOnce()).run();
        }

        @Test
        @DisplayName("Should respond to Escape key")
        void shouldRespondToEscapeKey() {
            // Simulate Escape key press
            assertThatCode(() -> {
                startScreen.getActionMap().get("exit")
                    .actionPerformed(new java.awt.event.ActionEvent(startScreen, 0, ""));
            }).doesNotThrowAnyException();
            
            verify(onExitMock, atLeastOnce()).run();
        }
    }

    @Nested
    @DisplayName("Visual Styling")
    class VisualStyling {

        @Test
        @DisplayName("Should have consistent color scheme")
        void shouldHaveConsistentColorScheme() {
            assertThat(startScreen.getBackground()).isNotNull();
            
            // Check that all panels have backgrounds
            for (Component component : startScreen.getComponents()) {
                if (component instanceof JPanel) {
                    JPanel panel = (JPanel) component;
                    assertThat(panel.getBackground()).isNotNull();
                }
            }
        }

        @Test
        @DisplayName("Should have proper component sizing")
        void shouldHaveProperComponentSizing() {
            JPanel buttonPanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.SOUTH);
            
            // Check button sizing
            for (Component component : buttonPanel.getComponents()) {
                if (component instanceof JButton) {
                    JButton button = (JButton) component;
                    assertThat(button.getPreferredSize().width).isGreaterThan(100);
                    assertThat(button.getPreferredSize().height).isGreaterThan(30);
                }
            }
        }

        @Test
        @DisplayName("Should have proper text alignment")
        void shouldHaveProperTextAlignment() {
            // All labels should be center-aligned
            for (Component component : startScreen.getComponents()) {
                if (component instanceof JPanel) {
                    JPanel panel = (JPanel) component;
                    for (Component child : panel.getComponents()) {
                        if (child instanceof JLabel) {
                            JLabel label = (JLabel) child;
                            assertThat(label.getHorizontalAlignment()).isEqualTo(SwingConstants.CENTER);
                        }
                    }
                }
            }
        }

        @Test
        @DisplayName("Should have appropriate fonts")
        void shouldHaveAppropriateFonts() {
            // All text components should have fonts
            for (Component component : startScreen.getComponents()) {
                if (component instanceof JPanel) {
                    JPanel panel = (JPanel) component;
                    for (Component child : panel.getComponents()) {
                        if (child instanceof JLabel || child instanceof JButton) {
                            JComponent textComponent = (JComponent) child;
                            assertThat(textComponent.getFont()).isNotNull();
                        }
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle null callbacks gracefully")
        void shouldHandleNullCallbacksGracefully() {
            assertThatCode(() -> {
                StartScreen nullCallbackScreen = new StartScreen(null, null);
                
                // Try to trigger callbacks
                JPanel buttonPanel = (JPanel) ((BorderLayout) nullCallbackScreen.getLayout())
                    .getLayoutComponent(BorderLayout.SOUTH);
                
                for (Component component : buttonPanel.getComponents()) {
                    if (component instanceof JButton) {
                        JButton button = (JButton) component;
                        for (java.awt.event.ActionListener listener : button.getActionListeners()) {
                            listener.actionPerformed(new java.awt.event.ActionEvent(button, 0, ""));
                        }
                    }
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle rapid button clicks")
        void shouldHandleRapidButtonClicks() {
            JPanel buttonPanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.SOUTH);

            final JButton startButton = findButtonByText(buttonPanel, "Start Game");
            assertThat(startButton).isNotNull();
            
            // Rapid clicks
            assertThatCode(() -> {
                for (int i = 0; i < 10; i++) {
                    for (java.awt.event.ActionListener listener : startButton.getActionListeners()) {
                        listener.actionPerformed(new java.awt.event.ActionEvent(startButton, 0, ""));
                    }
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle rapid keyboard shortcuts")
        void shouldHandleRapidKeyboardShortcuts() {
            // Rapid Enter key presses
            assertThatCode(() -> {
                for (int i = 0; i < 10; i++) {
                    startScreen.getActionMap().get("startGame")
                        .actionPerformed(new java.awt.event.ActionEvent(startScreen, 0, ""));
                }
            }).doesNotThrowAnyException();
            
            // Rapid Escape key presses
            assertThatCode(() -> {
                for (int i = 0; i < 10; i++) {
                    startScreen.getActionMap().get("exit")
                        .actionPerformed(new java.awt.event.ActionEvent(startScreen, 0, ""));
                }
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Component Integration")
    class ComponentIntegration {

        @Test
        @DisplayName("Should integrate all panels correctly")
        void shouldIntegrateAllPanelsCorrectly() {
            BorderLayout layout = (BorderLayout) startScreen.getLayout();
            
            assertThat(layout.getLayoutComponent(BorderLayout.NORTH)).isInstanceOf(JPanel.class);
            assertThat(layout.getLayoutComponent(BorderLayout.CENTER)).isInstanceOf(JPanel.class);
            assertThat(layout.getLayoutComponent(BorderLayout.SOUTH)).isInstanceOf(JPanel.class);
        }

        @Test
        @DisplayName("Should maintain component hierarchy")
        void shouldMaintainComponentHierarchy() {
            // StartScreen should be the root container
            assertThat(startScreen.getComponentCount()).isEqualTo(3);
            
            // Each panel should contain its expected components
            JPanel titlePanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
            JPanel centerPanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
            JPanel buttonPanel = (JPanel) ((BorderLayout) startScreen.getLayout())
                .getLayoutComponent(BorderLayout.SOUTH);
            
            assertThat(titlePanel.getComponentCount()).isGreaterThan(0);
            assertThat(centerPanel.getComponentCount()).isGreaterThan(0);
            assertThat(buttonPanel.getComponentCount()).isGreaterThan(0);
        }
    }
}
