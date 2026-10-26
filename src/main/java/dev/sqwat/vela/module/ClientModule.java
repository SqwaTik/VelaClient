package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ClientModule {
    private final String id;
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private List<Setting<?>> settingSnapshot=List.of();
    private boolean enabled;
    private java.util.function.BooleanSupplier availability=()->true;
    public final boolean available(){return availability.getAsBoolean();}
    public final void require(java.util.function.BooleanSupplier availability){this.availability=Objects.requireNonNull(availability);if(!available())setEnabled(false);}
    private String keybind = "None";
    private int boundKey = -1;
    private BindMode bindMode = BindMode.TOGGLE;

    public enum BindMode { TOGGLE, HOLD }

    protected ClientModule(String id, String name, String description, ModuleCategory category) {
        if (id == null || !id.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid module id");
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.category = Objects.requireNonNull(category);
    }

    protected final <T extends Setting<?>> T setting(T setting) {
        if (settings.stream().anyMatch(s -> s.id().equals(setting.id()))) throw new IllegalArgumentException("Duplicate setting id");
        settings.add(setting);
        settingSnapshot=List.copyOf(settings);
        return setting;
    }

    public final void setEnabled(boolean enabled) {
        enabled=enabled&&available();
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public final void toggle() { setEnabled(!enabled); }
    protected void onEnable() {}
    protected void onDisable() {}
    public void onClientTick() {}
    public void normalizeSettings() {}

    public final String id() { return id; }
    public final String name() { return name; }
    public final String description() { return description; }
    public final ModuleCategory category() { return category; }
    public final List<Setting<?>> settings() { return settingSnapshot; }
    public final boolean enabled() { return enabled; }
    public final String keybind() { return keybind; }
    public final void setKeybind(String keybind) { this.keybind = Objects.requireNonNull(keybind); }
    public final int boundKey() { return boundKey; }
    public final BindMode bindMode() { return bindMode; }
    public final void bind(int key, String name, BindMode mode) {
        boundKey = key;
        keybind = key < 0 ? "None" : Objects.requireNonNull(name);
        bindMode = key < 0 ? BindMode.TOGGLE : Objects.requireNonNull(mode);
    }
}
