package com.KIRA_ZINA.app.ui;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Top panel containing Timer, Mine Counter, and Reset Button (Smiley).
 */
public class TopPanel extends JPanel {
    private final JLabel timerLabel;
    private final JLabel mineCounterLabel;
    private final JButton smileyButton;

    private int timeElapsed;
    private Timer timer;
    private boolean timerRunning;

    // Smiley States
    private static final String SMILEY_NORMAL = "🙂";
    private static final String SMILEY_SCARED = "😮";
    private static final String SMILEY_SAD    = "😵";
    private static final String SMILEY_COOL   = "😎";

    private Runnable onResetListener;

    public TopPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        setBackground(Color.LIGHT_GRAY);

        timeElapsed = 0;
        timerRunning = false;

        // Mine Counter (Left)
        mineCounterLabel = createLedLabel("000");
        add(mineCounterLabel, BorderLayout.WEST);

        // Smiley Button (Center)
        smileyButton = new JButton(SMILEY_NORMAL);
        smileyButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        smileyButton.setFocusPainted(false);
        smileyButton.setPreferredSize(new Dimension(40, 40));
        smileyButton.addActionListener(e -> triggerReset());
        add(smileyButton, BorderLayout.CENTER);

        // Timer (Right)
        timerLabel = createLedLabel("000");
        add(timerLabel, BorderLayout.EAST);

        // Game Timer
        timer = new Timer(1000, e -> {
            timeElapsed++;
            updateTimerDisplay();
        });
    }

    private JLabel createLedLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Courier New", Font.BOLD, 24));
        label.setForeground(Color.RED);
        label.setBackground(Color.BLACK);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createLoweredBevelBorder());
        label.setPreferredSize(new Dimension(60, 30));
        return label;
    }

    public void setOnResetListener(Runnable listener) {
        this.onResetListener = listener;
    }

    private void triggerReset() {
        if (onResetListener != null) {
            onResetListener.run();
        }
    }

    public void startTimer() {
        if (!timerRunning) {
            timeElapsed = 0;
            timerRunning = true;
            updateTimerDisplay();
            timer.start();
        }
    }

    public void stopTimer() {
        timerRunning = false;
        timer.stop();
    }

    public void resetTimer() {
        stopTimer();
        timeElapsed = 0;
        updateTimerDisplay();
        setSmiley(SMILEY_NORMAL);
    }

    private void updateTimerDisplay() {
        // Cap at 999 seconds
        int displayTime = Math.min(timeElapsed, 999);
        timerLabel.setText(new DecimalFormat("000").format(displayTime));
    }

    public void updateMineCounter(int count) {
        // VISUAL-001: Fix negative number display
        int displayCount = Math.max(-99, Math.min(999, count));
        if (displayCount < 0) {
            mineCounterLabel.setText(String.format("%03d", displayCount));
        } else {
            mineCounterLabel.setText(new DecimalFormat("000").format(displayCount));
        }
    }

    public void setSmiley(String emoji) {
        smileyButton.setText(emoji);
    }

    public void setScaredFace() {
        // UX-005: Show scared face even before first click
        setSmiley(SMILEY_SCARED);
    }

    public void setNormalFace() {
        // UX-005: Show normal face even before first click
        setSmiley(SMILEY_NORMAL);
    }

    public void setSadFace() {
        stopTimer();
        setSmiley(SMILEY_SAD);
    }

    public void setCoolFace() {
        stopTimer();
        setSmiley(SMILEY_COOL);
    }
}
