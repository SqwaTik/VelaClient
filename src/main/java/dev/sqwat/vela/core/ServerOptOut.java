package dev.sqwat.vela.core;

import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.module.ServerRules;
import dev.sqwat.vela.ui.Lang;
import dev.sqwat.vela.ui.Notifications;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
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
 * <p>A server without a plugin sends the same request as a system message, {@code velaclient:disable freelook hitboxes}
 * (a {@code /tellraw}, a join message): it is hidden from chat. Only system messages count, and only when the whole
 * message starts with the request, so a player's chat line ({@code <nick> ...}) cannot switch anything off for others.
 */
public final class ServerOptOut {
    public record Disable(List<String> modules) implements CustomPacketPayload {
        public static final Type<Disable> TYPE=new Type<>(VelaClient.id("disable"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Disable> CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(256)),Disable::modules,Disable::new);
        @Override public Type<Disable> type(){return TYPE;}
    }
    private static final String CHAT_PREFIX="velaclient:disable";
    private static final Pattern ID=Pattern.compile("\\*|[a-z0-9_]{1,64}");
    private ServerOptOut(){}

    /** The module ids of a chat request ("velaclient:disable a b,c"), or null when the text is not one. */
    public static List<String> chatRequest(String text){
        if(text==null)return null;
        String t=text.strip();
        if(!t.startsWith(CHAT_PREFIX))return null;
        String rest=t.substring(CHAT_PREFIX.length());
        if(!rest.isEmpty()&&!Character.isWhitespace(rest.charAt(0)))return null;
        List<String> ids=new ArrayList<>();
        for(var id:rest.strip().split("[\\s,]+")){
            if(id.isEmpty())continue;
            if(!ID.matcher(id).matches()||ids.size()>=256)return null;
            ids.add(id);
        }
        return ids.isEmpty()?null:ids;
    }

    public static void register(ModuleRegistry modules){
        PayloadTypeRegistry.clientboundPlay().register(Disable.TYPE,Disable.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Disable.TYPE,(payload,context)->apply(payload.modules(),modules));
        ClientReceiveMessageEvents.ALLOW_GAME.register((message,overlay)->{
            var ids=chatRequest(message.getString());
            if(ids==null)return true;
            apply(ids,modules);
            return false;
        });
    }

    private static void apply(List<String> ids,ModuleRegistry modules){
        var off=ServerRules.serverRequest(ids,modules);
        if(!off.isEmpty())Notifications.show(Lang.tr("Сервер")+": "+Lang.tr("правила сервера"),
            Lang.tr("Отключено")+": "+String.join(", ",off),"settings",0xFFFFA24D,"",null,()->true,0);
    }
}
