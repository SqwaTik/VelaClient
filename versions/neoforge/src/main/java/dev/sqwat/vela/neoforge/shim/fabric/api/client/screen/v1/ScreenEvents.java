package dev.sqwat.vela.neoforge.shim.fabric.api.client.screen.v1;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenEvents {
    public static final Event<AfterInit> AFTER_INIT = new Event<>();

    private ScreenEvents() {}

    public static Event<Remove> remove(Screen screen) { return ScreenHooks.of(screen).remove; }

    public static Event<BeforeExtract> beforeExtract(Screen screen) { return ScreenHooks.of(screen).beforeExtract; }

    public static Event<AfterExtract> afterExtract(Screen screen) { return ScreenHooks.of(screen).afterExtract; }

    public interface AfterInit { void afterInit(Minecraft client, Screen screen, int width, int height); }

    public interface Remove { void onRemove(Screen screen); }

    public interface BeforeExtract { void beforeExtract(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial); }

    public interface AfterExtract { void afterExtract(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial); }
}
