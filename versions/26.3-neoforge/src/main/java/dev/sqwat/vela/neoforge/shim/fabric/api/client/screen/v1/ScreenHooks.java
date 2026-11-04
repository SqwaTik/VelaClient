package dev.sqwat.vela.neoforge.shim.fabric.api.client.screen.v1;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import net.minecraft.client.gui.screens.Screen;

import java.util.Map;
import java.util.WeakHashMap;

/** One screen's events; like Fabric, a screen starts with none each time it is initialized. */
public final class ScreenHooks {
    private static final Map<Screen, ScreenHooks> HOOKS = new WeakHashMap<>();

    public final Event<ScreenEvents.Remove> remove = new Event<>();
    public final Event<ScreenEvents.BeforeExtract> beforeExtract = new Event<>();
    public final Event<ScreenEvents.AfterExtract> afterExtract = new Event<>();
    public final Event<ScreenMouseEvents.AllowMouseClick> allowMouseClick = new Event<>();
    public final Event<ScreenKeyboardEvents.AllowKeyPress> allowKeyPress = new Event<>();
    public final Event<ScreenKeyboardEvents.AllowCharType> allowCharType = new Event<>();

    private ScreenHooks() {}

    public static synchronized ScreenHooks of(Screen screen) { return HOOKS.computeIfAbsent(screen, s -> new ScreenHooks()); }

    /** The screen's events, or null when nothing was registered on it. */
    public static synchronized ScreenHooks peek(Screen screen) { return HOOKS.get(screen); }

    public static synchronized void reset(Screen screen) { HOOKS.remove(screen); }
}
