package dev.sqwat.vela;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NoFrameGenerationTest {
    @Test void retiredFeatureCannotBeBundledOrRegistered() throws Exception {
        assertFalse(Files.exists(Path.of("src/main/java/dev/sqwat/vela/render/fg")));
        assertFalse(Files.exists(Path.of("native")));
        String build = Files.readString(Path.of("build.gradle"));
        assertFalse(build.contains("vela.fg"));
        assertFalse(build.contains("FidelityFX"));
        try (var input = getClass().getResourceAsStream("/velaclient.mixins.json")) {
            assertNotNull(input);
            assertFalse(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("\"Fg"));
        }
    }
}
