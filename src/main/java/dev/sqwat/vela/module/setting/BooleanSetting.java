package dev.sqwat.vela.module.setting;

public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String id, String name, String description, boolean defaultValue) {
        super(id, name, description, defaultValue);
    }

    public void toggle() { set(!value()); }
}
