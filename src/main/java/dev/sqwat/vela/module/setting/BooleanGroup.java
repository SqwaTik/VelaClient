package dev.sqwat.vela.module.setting;

import java.util.List;

/** A reusable presentation of independent flags; the flags remain the persisted values. */
public final class BooleanGroup extends ChoiceSetting {
    private final List<BooleanSetting> flags;
    private final boolean segmented;
    public BooleanGroup(String id, String name, List<BooleanSetting> flags, boolean segmented) {
        super(id, name, "", flags.getFirst().name(), flags.stream().map(Setting::name).toList());
        this.flags = List.copyOf(flags);
        this.segmented = segmented;
    }
    public List<BooleanSetting> flags() { return flags; }
    public boolean segmented() { return segmented; }
    public boolean selected(String name) { return flag(name).value(); }
    public void toggle(String name) { flag(name).toggle(); }
    private BooleanSetting flag(String name) {
        return flags.stream().filter(f -> f.name().equals(name)).findFirst().orElseThrow();
    }
    public String label() { return String.join(", ", flags.stream().filter(f -> f.value()).map(Setting::name).toList()); }
}
