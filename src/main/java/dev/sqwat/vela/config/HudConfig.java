package dev.sqwat.vela.config;

import dev.sqwat.vela.ui.HudLayoutService.HudPosition;
import java.util.LinkedHashMap;
import java.util.Map;

/** One persisted document owns current and reset HUD geometry. */
public final class HudConfig {
    public int schema=1;
    public Map<String,HudPosition> positions=new LinkedHashMap<>();
    public Map<String,Float> scales=new LinkedHashMap<>();
    public Map<String,HudPosition> defaultPositions=new LinkedHashMap<>();
    public Map<String,Float> defaultScales=new LinkedHashMap<>();
}
