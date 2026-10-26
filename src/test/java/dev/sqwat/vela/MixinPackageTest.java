package dev.sqwat.vela;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MixinPackageTest {
    @Test void reservedMixinPackageContainsOnlyRegisteredMixins() throws Exception {
        var config = com.google.gson.JsonParser.parseString(Files.readString(Path.of("src/main/resources/velaclient.mixins.json"))).getAsJsonObject();
        var registered = new java.util.HashSet<String>();
        for (String group : new String[]{"client", "mixins", "server"}) {
            if (config.has(group)) for (var entry : config.getAsJsonArray(group)) registered.add(entry.getAsString());
        }
        Path root = Path.of("src/main/java/dev/sqwat/vela/mixin");
        try (var files = Files.walk(root)) {
            for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = root.relativize(file).toString().replace('\\', '.').replace('/', '.').replaceAll("\\.java$", "");
                assertTrue(registered.contains(name), "Ordinary classes must live outside the reserved mixin package: " + name);
                assertTrue(Files.readString(file).contains("@Mixin("), "Missing @Mixin: " + name);
            }
        }
    }
}
