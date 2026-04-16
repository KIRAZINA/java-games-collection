package com.KIRA_ZINA.launcher;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GameHubRuntimeResolverTest {

    @Test
    void returnsNullForMissingGameJar() {
        Path jar = GameHubLauncher.resolveGameJar(
                Path.of("missing-game", "target", "missing-game.jar")
        );

        assertNull(jar);
    }

    @Test
    void resolvesGuiJavaExecutable() {
        String executable = GameHubLauncher.resolveGuiJavaExecutable();

        assertNotNull(executable);
        assertFalse(executable.isBlank());
    }
}
