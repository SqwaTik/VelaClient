package dev.sqwat.vela.neoforge.shim.fabric.api.client.message.v1;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import net.minecraft.network.chat.Component;

public final class ClientReceiveMessageEvents {
    /** NeoForge's ClientChatReceivedEvent.System; a listener returning false cancels the message and GAME is skipped. */
    public static final Event<AllowGame> ALLOW_GAME = new Event<>();
    /** NeoForge's ClientChatReceivedEvent.System. */
    public static final Event<Game> GAME = new Event<>();

    private ClientReceiveMessageEvents() {}

    public interface AllowGame { boolean allowReceiveGameMessage(Component message, boolean overlay); }

    public interface Game { void onReceiveGameMessage(Component message, boolean overlay); }
}
