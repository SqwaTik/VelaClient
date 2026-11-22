package dev.sqwat.vela.neoforge.shim.fabric.api.client.rendering.v1.hud;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class HudElementRegistry {
    /** Handed to NeoForge's RegisterGuiLayersEvent: first = below every layer, last = above every layer. */
    public record Entry(Identifier id, HudElement element, boolean first) {}

    public static final List<Entry> PENDING = new CopyOnWriteArrayList<>();

    private HudElementRegistry() {}

    public static void addFirst(Identifier id, HudElement element) { PENDING.add(new Entry(id, element, true)); }

    public static void addLast(Identifier id, HudElement element) { PENDING.add(new Entry(id, element, false)); }
}
