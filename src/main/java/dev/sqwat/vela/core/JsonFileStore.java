package dev.sqwat.vela.core;

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
    /** Stored as compressed text ({@link #ENCODED}) instead of readable JSON; plain JSON files still load. */
    private final boolean encoded;
    static final String ENCODED="VELAP1:";

    public JsonFileStore(Path file, Type type, Supplier<T> defaults) {this(file,type,defaults,false);}
    private JsonFileStore(Path file, Type type, Supplier<T> defaults, boolean encoded) {
        this.file = Objects.requireNonNull(file);
        this.type = Objects.requireNonNull(type);
        this.defaults = Objects.requireNonNull(defaults);
        this.encoded = encoded;
    }
    /** A store whose file is compact and not human-readable (config profiles). */
    public static <T> JsonFileStore<T> encoded(Path file, Type type, Supplier<T> defaults) {return new JsonFileStore<>(file,type,defaults,true);}

    public synchronized T load() {
        if (!Files.isRegularFile(file)) return defaults.get();
        try {
            String text=Files.readString(file, StandardCharsets.UTF_8);
            T value = GSON.fromJson(text.startsWith(ENCODED)?unpack(text):text, type);
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
            writer.write(encoded?pack(serialized):serialized);
        }
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
        lastSaved=serialized;
    }

    static String pack(String json) throws IOException {
        var deflater=new java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION);
        try{
            var out=new java.io.ByteArrayOutputStream();
            try(var zip=new java.util.zip.DeflaterOutputStream(out,deflater)){zip.write(json.getBytes(StandardCharsets.UTF_8));}
            return ENCODED+java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
        }finally{deflater.end();}
    }
    static String unpack(String text) throws IOException {
        byte[] bytes=java.util.Base64.getUrlDecoder().decode(text.substring(ENCODED.length()).strip());
        try(var zip=new java.util.zip.InflaterInputStream(new java.io.ByteArrayInputStream(bytes))){return new String(zip.readNBytes(4<<20),StandardCharsets.UTF_8);}
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
