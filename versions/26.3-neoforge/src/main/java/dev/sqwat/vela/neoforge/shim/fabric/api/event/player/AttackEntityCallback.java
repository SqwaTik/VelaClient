package dev.sqwat.vela.neoforge.shim.fabric.api.event.player;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.Event;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** Run by ClientInteractionMixin before the client attacks; anything but PASS stops the attack (no packet is sent). */
public interface AttackEntityCallback {
    Event<AttackEntityCallback> EVENT = new Event<>();

    InteractionResult interact(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hit);
}
