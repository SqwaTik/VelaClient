package dev.sqwat.vela.neoforge.shim.fabric.api.client.screen.v1;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public final class ScreenMouseEvents {
    private ScreenMouseEvents() {}

    public static Event<AllowMouseClick> allowMouseClick(Screen screen) { return ScreenHooks.of(screen).allowMouseClick; }

    public interface AllowMouseClick { boolean allowMouseClick(Screen screen, MouseButtonEvent event); }
}
