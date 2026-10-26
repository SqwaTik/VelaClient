package dev.sqwat.vela;

import dev.sqwat.vela.config.JsonFileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileStoreTest {
    @TempDir Path directory;

    @Test
    void writesAndReadsUtf8JsonAtomically() throws Exception {
        Path file = directory.resolve("nested/config.json");
        JsonFileStore<Value> store = new JsonFileStore<>(file, Value.class, Value::new);
        store.save(new Value("Привет", 7));

        assertEquals("Привет", store.load().name);
        assertEquals(7, store.load().count);
        assertFalse(Files.exists(file.resolveSibling("config.json.tmp")));
    }

    @Test
    void quarantinesMalformedJsonAndReturnsDefaults() throws Exception {
        Path file = directory.resolve("config.json");
        Files.writeString(file, "{broken");
        JsonFileStore<Value> store = new JsonFileStore<>(file, Value.class, () -> new Value("safe", 1));

        assertEquals("safe", store.load().name);
        assertTrue(Files.exists(directory.resolve("config.json.broken")));
    }

    static final class Value {
        String name = "";
        int count;
        Value() {}
        Value(String name, int count) { this.name = name; this.count = count; }
    }
}
