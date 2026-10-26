package dev.sqwat.vela.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Supplier;

public final class JsonFileStore<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path file;
    private final Type type;
    private final Supplier<T> defaults;
    private String lastSaved;

    public JsonFileStore(Path file, Type type, Supplier<T> defaults) {
        this.file = Objects.requireNonNull(file);
        this.type = Objects.requireNonNull(type);
        this.defaults = Objects.requireNonNull(defaults);
    }

    public synchronized T load() {
        if (!Files.isRegularFile(file)) return defaults.get();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            T value = GSON.fromJson(reader, type);
            if(value!=null)lastSaved=GSON.toJson(value,type);
            return value == null ? defaults.get() : value;
        } catch (Exception malformed) {
            quarantineBrokenFile();
            return defaults.get();
        }
    }

    public synchronized void save(T value) throws IOException {
        String serialized=GSON.toJson(value,type);
        if(serialized.equals(lastSaved)&&Files.isRegularFile(file))return;
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            writer.write(serialized);
        }
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
        lastSaved=serialized;
    }

    private void quarantineBrokenFile() {
        try {
            Path broken = file.resolveSibling(file.getFileName() + ".broken");
            Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Loading still returns safe defaults if the filesystem is read-only.
        }
    }
}
