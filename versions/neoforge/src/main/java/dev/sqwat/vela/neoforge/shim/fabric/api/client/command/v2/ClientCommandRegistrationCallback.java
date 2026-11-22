package dev.sqwat.vela.neoforge.shim.fabric.api.client.command.v2;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;

/** Run by ClientCommandBridge from NeoForge's RegisterClientCommandsEvent. */
public interface ClientCommandRegistrationCallback {
    Event<ClientCommandRegistrationCallback> EVENT = new Event<>();

    void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registry);
}
