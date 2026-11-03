package dev.sqwat.vela.core;

import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.module.ServerRules;
import dev.sqwat.vela.ui.Lang;
import dev.sqwat.vela.ui.Notifications;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Lets any server switch Vela functions off: a plugin message on {@code velaclient:disable} carrying a list of
 * module ids (VarInt count, then VarInt-prefixed UTF-8 strings), or "*" for every function that can give an edge
 * (see {@link ServerRules#COMPETITIVE}). The modules stay off and hidden until the player leaves the server.
 */
public final class ServerOptOut {
    public record Disable(List<String> modules) implements CustomPacketPayload {
        public static final Type<Disable> TYPE=new Type<>(VelaClient.id("disable"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Disable> CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(256)),Disable::modules,Disable::new);
        @Override public Type<Disable> type(){return TYPE;}
    }
    private ServerOptOut(){}

    public static void register(ModuleRegistry modules){
        PayloadTypeRegistry.clientboundPlay().register(Disable.TYPE,Disable.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Disable.TYPE,(payload,context)->{
            var off=ServerRules.serverRequest(payload.modules(),modules);
            if(!off.isEmpty())Notifications.show(Lang.tr("Сервер")+": "+Lang.tr("правила сервера"),
                Lang.tr("Отключено")+": "+String.join(", ",off),"settings",0xFFFFA24D,"",null,()->true,0);
        });
    }
}
