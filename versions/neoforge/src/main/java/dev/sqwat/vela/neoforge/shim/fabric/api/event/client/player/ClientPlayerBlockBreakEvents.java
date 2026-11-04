package dev.sqwat.vela.neoforge.shim.fabric.api.event.client.player;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class ClientPlayerBlockBreakEvents {
    /** Run by ClientInteractionMixin after the client breaks a block. */
    public static final Event<After> AFTER = new Event<>();

    private ClientPlayerBlockBreakEvents() {}

    public interface After { void afterBlockBreak(ClientLevel level, LocalPlayer player, BlockPos pos, BlockState state); }
}
