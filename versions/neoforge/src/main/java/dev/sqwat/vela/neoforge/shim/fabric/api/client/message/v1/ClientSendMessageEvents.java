package dev.sqwat.vela.neoforge.shim.fabric.api.client.message.v1;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;

public final class ClientSendMessageEvents {
    /** NeoForge's ClientChatEvent, for chat messages (not commands); false cancels the message. */
    public static final Event<AllowChat> ALLOW_CHAT = new Event<>();

    private ClientSendMessageEvents() {}

    public interface AllowChat { boolean allowSendChatMessage(String message); }
}
