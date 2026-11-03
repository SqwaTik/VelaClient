package dev.sqwat.vela.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SpirvGlslTest {
    private static final Pattern BARE = Pattern.compile("^\\s*(?:flat\\s+)?(?:in|out)\\s+\\w+\\s+\\w+\\s*;", Pattern.MULTILINE);
    private static final Pattern VARYING = Pattern.compile("layout\\(location = (\\d+)\\) (?:flat )?(in|out)\\s+\\w+\\s+(\\w+)");

    @Test
    void everyShaderGetsLocations() throws IOException {
        try (var files = Files.walk(Path.of("src/main/resources/assets"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".vsh") || p.toString().endsWith(".fsh")).toList()) {
                String ported = SpirvGlsl.port(Files.readString(file), file.toString().endsWith(".vsh"));
                assertFalse(BARE.matcher(ported).find(), file + " still has an input or output without a location");
                assertFalse(ported.contains("gl_VertexID"), file.toString());
                assertTrue(ported.lines().skip(1).findFirst().orElse("").contains("GL_ARB_separate_shader_objects"), file.toString());
            }
        }
    }

    @Test
    void pairedStagesAgreeOnLocations() throws IOException {
        var dir = Path.of("src/main/resources/assets/velaclient/shaders/core");
        String vertex = SpirvGlsl.port(Files.readString(dir.resolve("glass.vsh")), true);
        var outs = new HashMap<String, String>();
        var m = VARYING.matcher(vertex);
        while (m.find()) if (m.group(2).equals("out")) outs.put(m.group(3), m.group(1));
        for (String fragment : new String[] {"glass.fsh", "motion_blur.fsh", "hud_cache.fsh"}) {
            var f = VARYING.matcher(SpirvGlsl.port(Files.readString(dir.resolve(fragment)), false));
            while (f.find()) if (f.group(2).equals("in")) assertEquals(outs.get(f.group(3)), f.group(1), fragment + " " + f.group(3));
        }
    }

    @Test
    void portedOnce() {
        String once = SpirvGlsl.port("#version 330\nout vec2 texCoord;\nvoid main(){}\n", true);
        assertEquals(once, SpirvGlsl.port(once, true));
        assertTrue(once.contains("layout(location = 0) out vec2 texCoord;"));
    }
}
