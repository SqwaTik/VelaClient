package dev.sqwat.vela.module;

public enum ModuleCategory {
    VISUALS("Visuals"),
    HUD("HUD"),
    UTILITIES("Utilities"),
    MARKERS("Markers");

    private final String title;

    ModuleCategory(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
