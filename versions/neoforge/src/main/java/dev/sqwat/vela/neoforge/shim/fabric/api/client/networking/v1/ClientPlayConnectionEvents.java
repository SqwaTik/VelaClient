package dev.sqwat.vela.neoforge.shim.fabric.api.client.networking.v1;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import dev.sqwat.vela.neoforge.shim.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class ClientPlayConnectionEvents {
    /** NeoForge's ClientPlayerNetworkEvent.LoggingIn. */
    public static final Event<Join> JOIN = new Event<>();
    /** NeoForge's ClientPlayerNetworkEvent.LoggingOut. */
    public static final Event<Disconnect> DISCONNECT = new Event<>();

    private ClientPlayConnectionEvents() {}

    public interface Join { void onPlayReady(ClientPacketListener handler, PacketSender sender, Minecraft client); }

    public interface Disconnect { void onPlayDisconnect(ClientPacketListener handler, Minecraft client); }
}
