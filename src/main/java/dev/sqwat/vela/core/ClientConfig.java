package dev.sqwat.vela.core;

import com.google.gson.JsonElement;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientConfig {
    public int schema = 1;
    public float uiScale = 1.0f;
    public boolean replaceMainMenu = true;
    public boolean reducedMotion = false;
    public boolean animations = true;
    public boolean alwaysShowBinds = false;
    public boolean friendVisualsExpanded = false;
    public boolean uiSounds = true;
    public float uiSoundVolume = 0.55f;
    public String uiSoundPreset = "Ритм";
    public String accentColor = "#756CFF";
    /** Interface language: "en" (default) or "ru". */
    public String language = "en";
    /** Chat privacy toggle: hide what is typed after /login and /register in the chat input. */
    public boolean chatPrivacy = true;
    /** Newest changelog version the player has opened; older means the menu shows a dot. */
    public String lastSeenChangelog = "";
    public String selectedTheme = "aurora";
    public String activeProfile = "default";
    public boolean friendVisuals=true;
    public boolean protectFriends=true;
    public boolean friendChinaHat=true;
    public boolean friendDashTrail=true;
    public boolean friendJumpCircle=true;
    public boolean friendOptionsOpen;
    public int settingsTab;
    public boolean friendFavoritesFirst=true;
    public String lastSection = "UTILITIES";
    public String expandedModule;
    public java.util.Set<String> expandedModules = new java.util.LinkedHashSet<>();
    public java.util.Set<String> favorites = new java.util.LinkedHashSet<>();
    public int searchKey = 75;
    public Map<String, ModuleState> modules = new LinkedHashMap<>();
    public Map<String, JsonElement> optimization;

    public static final class ModuleState {
        public boolean enabled;
        public String keybind = "None";
        public int boundKey = -1;
        public String bindMode = "TOGGLE";
        public Map<String, JsonElement> settings = new LinkedHashMap<>();
    }
}
