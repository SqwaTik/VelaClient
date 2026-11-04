package dev.sqwat.vela.neoforge.shim.fabric.api.networking.v1;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Clientbound play payloads; NeoEvents registers each one that has a receiver, as optional (servers without it are fine). */
public final class PayloadTypeRegistry {
    private static final PayloadTypeRegistry CLIENTBOUND_PLAY = new PayloadTypeRegistry();

    public final Map<CustomPacketPayload.Type<?>, StreamCodec<? super RegistryFriendlyByteBuf, ?>> codecs = new ConcurrentHashMap<>();

    private PayloadTypeRegistry() {}

    public static PayloadTypeRegistry clientboundPlay() { return CLIENTBOUND_PLAY; }

    public <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        codecs.put(type, codec);
    }
}
