package dev.sqwat.vela.neoforge.shim.fabric.api.event.player;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** Run by ClientInteractionMixin before the client uses a block; anything but PASS is the result (no packet is sent). */
public interface UseBlockCallback {
    Event<UseBlockCallback> EVENT = new Event<>();

    InteractionResult interact(Player player, Level level, InteractionHand hand, BlockHitResult hit);
}
