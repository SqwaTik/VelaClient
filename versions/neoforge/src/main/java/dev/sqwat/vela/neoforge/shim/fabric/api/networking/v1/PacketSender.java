package dev.sqwat.vela.neoforge.shim.fabric.api.networking.v1;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public interface PacketSender {
    PacketSender SERVER = payload -> ClientPacketDistributor.sendToServer(payload);

    void sendPacket(CustomPacketPayload payload);
}
