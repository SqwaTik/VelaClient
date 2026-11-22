package dev.sqwat.vela.neoforge.shim.fabric.api.client.command.v2;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public interface FabricClientCommandSource extends SharedSuggestionProvider {
    void sendFeedback(Component message);

    void sendError(Component message);

    Minecraft getClient();

    LocalPlayer getPlayer();

    ClientLevel getLevel();
}
