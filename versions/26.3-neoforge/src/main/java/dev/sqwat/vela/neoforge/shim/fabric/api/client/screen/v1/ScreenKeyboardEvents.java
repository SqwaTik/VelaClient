package dev.sqwat.vela.neoforge.shim.fabric.api.client.screen.v1;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public final class ScreenKeyboardEvents {
    private ScreenKeyboardEvents() {}

    public static Event<AllowKeyPress> allowKeyPress(Screen screen) { return ScreenHooks.of(screen).allowKeyPress; }

    public static Event<AllowCharType> allowCharType(Screen screen) { return ScreenHooks.of(screen).allowCharType; }

    public interface AllowKeyPress { boolean allowKeyPress(Screen screen, KeyEvent event); }

    public interface AllowCharType { boolean allowCharType(Screen screen, CharacterEvent event); }
}
