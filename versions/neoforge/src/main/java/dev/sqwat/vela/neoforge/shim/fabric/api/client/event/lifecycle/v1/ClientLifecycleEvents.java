package dev.sqwat.vela.neoforge.shim.fabric.api.client.event.lifecycle.v1;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import net.minecraft.client.Minecraft;

public final class ClientLifecycleEvents {
    /** Before the first client tick. */
    public static final Event<ClientStarted> CLIENT_STARTED = new Event<>();
    /** NeoForge's GameShuttingDownEvent. */
    public static final Event<ClientStopping> CLIENT_STOPPING = new Event<>();

    private ClientLifecycleEvents() {}

    public interface ClientStarted { void onClientStarted(Minecraft client); }

    public interface ClientStopping { void onClientStopping(Minecraft client); }
}
