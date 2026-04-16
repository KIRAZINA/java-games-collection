package com.KIRA_ZINA.launcher;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameHubLauncherTest {

    @Test
    void findsWorkspaceRootFromLauncherModule() {
        Path root = GameHubLauncher.findWorkspaceRoot();

        assertTrue(Files.exists(root.resolve("pom.xml")));
        assertTrue(Files.exists(root.resolve("2048-game")));
        assertTrue(Files.exists(root.resolve("Minesweeper-game")));
        assertTrue(Files.exists(root.resolve("Black-Jack-game")));
    }

    @Test
    void resolvesJavaExecutable() {
        String executable = GameHubLauncher.resolveJavaExecutable();

        assertNotNull(executable);
        assertFalse(executable.isBlank());
    }
}
