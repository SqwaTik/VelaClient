package dev.sqwat.vela.module.setting;

import java.util.Objects;

public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;
    private java.util.function.BooleanSupplier visibility=()->true;

    protected Setting(String id, String name, String description, T defaultValue) {
        this.id = requireId(id);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.value = defaultValue;
    }

    public final String id() { return id; }
    public final String name() { return name; }
    public final String description() { return description; }
    public final T value() { return value; }
    public final T defaultValue() { return defaultValue; }
    public final boolean visible() { return visibility.getAsBoolean(); }
    public final Setting<T> visibleWhen(java.util.function.BooleanSupplier condition) { visibility=Objects.requireNonNull(condition); return this; }

    public final void set(T next) {
        value = validate(Objects.requireNonNull(next));
    }

    public final void reset() { value = defaultValue; }
    protected T validate(T next) { return next; }

    private static String requireId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid setting id: " + id);
        }
        return id;
    }
}
