package dev.sqwat.vela.neoforge.shim.fabric.api.client.message.v1;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import net.minecraft.network.chat.Component;

public final class ClientReceiveMessageEvents {
    /** NeoForge's ClientChatReceivedEvent.System. */
    public static final Event<Game> GAME = new Event<>();

    private ClientReceiveMessageEvents() {}

    public interface Game { void onReceiveGameMessage(Component message, boolean overlay); }
}
