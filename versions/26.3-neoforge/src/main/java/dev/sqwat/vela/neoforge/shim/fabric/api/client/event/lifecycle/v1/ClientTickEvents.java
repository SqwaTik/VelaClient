package dev.sqwat.vela.neoforge.shim.fabric.api.client.event.lifecycle.v1;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import net.minecraft.client.Minecraft;

public final class ClientTickEvents {
    public static final Event<EndTick> END_CLIENT_TICK = new Event<>();

    private ClientTickEvents() {}

    public interface EndTick { void onEndTick(Minecraft client); }
}
