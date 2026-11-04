package dev.sqwat.vela.neoforge.shim.fabric.api.client.networking.v1;

import dev.sqwat.vela.neoforge.shim.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPlayNetworking {
    public static final Map<CustomPacketPayload.Type<?>, PlayPayloadHandler<?>> RECEIVERS = new ConcurrentHashMap<>();

    private ClientPlayNetworking() {}

    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(CustomPacketPayload.Type<T> type, PlayPayloadHandler<T> handler) {
        return RECEIVERS.putIfAbsent(type, handler) == null;
    }

    public interface PlayPayloadHandler<T extends CustomPacketPayload> { void receive(T payload, Context context); }

    public interface Context {
        Minecraft client();

        LocalPlayer player();

        PacketSender responseSender();
    }
}
