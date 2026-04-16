package com.KIRA_ZINA.launcher;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GameHubLauncher {
    private static final String TITLE = "Java Games Hub";
    private static final Color PAGE_BACKGROUND = new Color(236, 240, 245);
    private static final Color TEXT_PRIMARY = new Color(25, 31, 38);
    private static final Color TEXT_MUTED = new Color(88, 97, 108);
    private static final Color CARD_SURFACE = new Color(252, 252, 250);
    private static final Color CARD_BORDER = new Color(216, 221, 228);
    private static final Color ACCENT_BLUE = new Color(32, 86, 196);
    private static final Color ACCENT_GOLD = new Color(228, 177, 67);
    private static final Color ACCENT_GREEN = new Color(45, 138, 98);
    private static final Font HERO_FONT = new Font("Georgia", Font.BOLD, 34);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font META_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Path GAME_2048_JAR = Path.of("2048-game", "target", "2048-game-1.0-SNAPSHOT.jar");
    private static final Path MINESWEEPER_JAR = Path.of("Minesweeper-game", "target", "Minesweeper-game-1.0-SNAPSHOT.jar");
    private static final Path BLACKJACK_JAR = Path.of("Black-Jack-game", "target", "Black-Jack-game-1.0-SNAPSHOT.jar");

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default look and feel if the system theme is unavailable.
        }

        SwingUtilities.invokeLater(GameHubLauncher::createAndShowUi);
    }

    private static void createAndShowUi() {
        JFrame frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(createContent(frame));
        frame.pack();
        frame.setMinimumSize(new Dimension(860, 610));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JPanel createContent(JFrame owner) {
        JPanel root = new JPanel(new BorderLayout(0, 22));
        root.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        root.setBackground(PAGE_BACKGROUND);

        root.add(createHeroSection(), BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        center.add(createGameCard(
                owner,
                "2048",
                "Classic number puzzle built with Swing.",
                "Sharp merges, fast rounds, clean keyboard play.",
                GAME_2048_JAR,
                ACCENT_GOLD
        ));
        center.add(Box.createVerticalStrut(16));
        center.add(createGameCard(
                owner,
                "Minesweeper",
                "Minesweeper with a dedicated difficulty selection screen.",
                "A calm strategy board with layered difficulty choices.",
                MINESWEEPER_JAR,
                ACCENT_GREEN
        ));
        center.add(Box.createVerticalStrut(16));
        center.add(createGameCard(
                owner,
                "Blackjack",
                "LibGDX desktop version packaged with all required dependencies.",
                "Arcade-style table feel with a richer desktop runtime.",
                BLACKJACK_JAR,
                ACCENT_BLUE
        ));
        center.add(Box.createVerticalStrut(18));
        center.add(createHintBlock());

        JScrollPane scrollPane = new JScrollPane(center);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);

        root.add(scrollPane, BorderLayout.CENTER);
        return root;
    }

    private static JComponent createHeroSection() {
        GradientPanel hero = new GradientPanel(new BorderLayout(0, 18), new Color(250, 233, 204), new Color(216, 229, 255));
        hero.setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel eyebrow = new JLabel("UNIFIED DESKTOP LAUNCHER");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        eyebrow.setForeground(new Color(69, 78, 92));
        topRow.add(eyebrow, BorderLayout.WEST);

        JLabel buildTag = createPill("3 games ready");
        topRow.add(buildTag, BorderLayout.EAST);

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(TITLE);
        title.setFont(HERO_FONT);
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        JTextArea subtitle = new JTextArea(
                "Launch every game from one polished home screen. Build once, click once, and jump straight into play."
        );
        subtitle.setEditable(false);
        subtitle.setLineWrap(true);
        subtitle.setWrapStyleWord(true);
        subtitle.setOpaque(false);
        subtitle.setFont(BODY_FONT);
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(JTextArea.LEFT_ALIGNMENT);

        textBlock.add(title);
        textBlock.add(Box.createVerticalStrut(8));
        textBlock.add(subtitle);

        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.add(createStatCard("3", "Games"));
        stats.add(createStatCard("1", "Hub"));
        stats.add(createStatCard("17+", "Java"));

        hero.add(topRow, BorderLayout.NORTH);
        hero.add(textBlock, BorderLayout.CENTER);
        hero.add(stats, BorderLayout.SOUTH);
        return hero;
    }

    private static JPanel createGameCard(
            JFrame owner,
            String gameName,
            String description,
            String detail,
            Path relativeJarPath,
            Color accentColor
    ) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(18, 10), 28, CARD_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 168));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel nameLabel = new JLabel(gameName);
        nameLabel.setFont(TITLE_FONT);
        nameLabel.setForeground(TEXT_PRIMARY);
        titleRow.add(nameLabel, BorderLayout.WEST);

        JLabel genreTag = createPill(detail);
        titleRow.add(genreTag, BorderLayout.EAST);

        JTextArea descriptionLabel = new JTextArea(description);
        descriptionLabel.setEditable(false);
        descriptionLabel.setLineWrap(true);
        descriptionLabel.setWrapStyleWord(true);
        descriptionLabel.setOpaque(false);
        descriptionLabel.setFont(BODY_FONT);
        descriptionLabel.setForeground(TEXT_MUTED);
        descriptionLabel.setAlignmentX(JTextArea.LEFT_ALIGNMENT);

        JPanel metaRow = new JPanel(new GridLayout(1, 2, 12, 0));
        metaRow.setOpaque(false);
        metaRow.add(createMetaLabel("Desktop-ready packaging"));
        metaRow.add(createMetaLabel("One-click launch from the hub"));

        textPanel.add(titleRow);
        textPanel.add(Box.createVerticalStrut(10));
        textPanel.add(descriptionLabel);
        textPanel.add(Box.createVerticalStrut(14));
        textPanel.add(metaRow);

        JButton launchButton = createLaunchButton(accentColor);
        launchButton.addActionListener(event -> launchGame(owner, gameName, relativeJarPath));

        JPanel accentStrip = new JPanel();
        accentStrip.setBackground(accentColor);
        accentStrip.setPreferredSize(new Dimension(10, 10));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.add(launchButton, BorderLayout.NORTH);

        card.add(accentStrip, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.EAST);
        return card;
    }

    private static JPanel createHintBlock() {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(), 24, new Color(247, 249, 252));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(217, 223, 230), 1, true),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        JTextArea help = new JTextArea(
                "If a game is missing, build the project with 'mvn clean package' from the repository root "
                        + "or use run-launcher.bat."
        );
        help.setWrapStyleWord(true);
        help.setLineWrap(true);
        help.setEditable(false);
        help.setFont(BODY_FONT);
        help.setForeground(TEXT_MUTED);
        help.setOpaque(false);

        panel.add(help, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel createPill(String text) {
        JLabel pill = new JLabel(text);
        pill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pill.setForeground(new Color(63, 72, 84));
        pill.setOpaque(true);
        pill.setBackground(new Color(255, 255, 255, 190));
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(216, 220, 227), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return pill;
    }

    private static JPanel createStatCard(String value, String label) {
        RoundedPanel stat = new RoundedPanel(new BorderLayout(0, 6), 18, new Color(255, 255, 255, 175));
        stat.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 223, 229), 1, true),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Georgia", Font.BOLD, 22));
        valueLabel.setForeground(TEXT_PRIMARY);

        JLabel textLabel = new JLabel(label);
        textLabel.setFont(META_FONT);
        textLabel.setForeground(TEXT_MUTED);

        stat.add(valueLabel, BorderLayout.CENTER);
        stat.add(textLabel, BorderLayout.SOUTH);
        return stat;
    }

    private static JLabel createMetaLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(META_FONT);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    private static JButton createLaunchButton(Color accentColor) {
        JButton button = new JButton("Launch");
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(accentColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(12, 22, 12, 22));
        button.setOpaque(true);
        return button;
    }

    private static void launchGame(JFrame owner, String gameName, Path relativeJarPath) {
        Path jarPath = resolveGameJar(relativeJarPath);
        if (jarPath == null) {
            JOptionPane.showMessageDialog(
                    owner,
                    "Launch file was not found for " + gameName + ".\n\nBuild the project with mvn clean package.",
                    "File Not Found",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        ProcessBuilder builder = new ProcessBuilder(List.of(
                resolveGuiJavaExecutable(),
                "-jar",
                jarPath.toString()
        ));
        builder.directory(jarPath.getParent().toFile());
        builder.inheritIO();

        try {
            builder.start();
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    owner,
                    "Failed to launch " + gameName + ":\n" + exception.getMessage(),
                    "Launch Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    static Path resolveGameJar(Path relativeJarPath) {
        for (Path start : getSearchRoots()) {
            Path candidate = start.resolve(relativeJarPath).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static Path findWorkspaceRoot() {
        for (Path start : getSearchRoots()) {
            Path current = start;
            while (current != null) {
                if (isWorkspaceRoot(current)) {
                    return current;
                }
                current = current.getParent();
            }
        }

        return Path.of("").toAbsolutePath().normalize();
    }

    static String resolveJavaExecutable() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        Path javaBinary = javaHome.resolve("bin").resolve(executable);
        return Files.exists(javaBinary) ? javaBinary.toString() : "java";
    }

    static String resolveGuiJavaExecutable() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String executable = isWindows ? "javaw.exe" : "java";
        Path javaBinary = javaHome.resolve("bin").resolve(executable);
        return Files.exists(javaBinary) ? javaBinary.toString() : resolveJavaExecutable();
    }

    private static boolean isWorkspaceRoot(Path path) {
        boolean hasSourceLayout = Files.exists(path.resolve("pom.xml"))
                && Files.exists(path.resolve("2048-game"))
                && Files.exists(path.resolve("Minesweeper-game"))
                && Files.exists(path.resolve("Black-Jack-game"));
        boolean hasRuntimeLayout = Files.exists(path.resolve(GAME_2048_JAR))
                && Files.exists(path.resolve(MINESWEEPER_JAR))
                && Files.exists(path.resolve(BLACKJACK_JAR));
        return hasSourceLayout || hasRuntimeLayout;
    }

    private static List<Path> getSearchRoots() {
        List<Path> roots = new ArrayList<>();
        roots.add(Path.of("").toAbsolutePath().normalize());

        try {
            Path codeSource = Path.of(GameHubLauncher.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()).toAbsolutePath().normalize();
            Path codeSourceRoot = Files.isRegularFile(codeSource) ? codeSource.getParent() : codeSource;
            roots.add(codeSourceRoot);
            if (codeSourceRoot != null && codeSourceRoot.getParent() != null) {
                roots.add(codeSourceRoot.getParent());
            }
        } catch (URISyntaxException | NullPointerException ignored) {
            // Ignore and fall back to the current working directory.
        }

        return roots;
    }

    private static final class RoundedPanel extends JPanel {
        private final int arc;
        private final Color fillColor;

        private RoundedPanel(BorderLayout layout, int arc, Color fillColor) {
            super(layout);
            this.arc = arc;
            this.fillColor = fillColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class GradientPanel extends JPanel {
        private final Color topColor;
        private final Color bottomColor;

        private GradientPanel(BorderLayout layout, Color topColor, Color bottomColor) {
            super(layout);
            this.topColor = topColor;
            this.bottomColor = bottomColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, topColor, getWidth(), getHeight(), bottomColor));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 34, 34);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }
}
