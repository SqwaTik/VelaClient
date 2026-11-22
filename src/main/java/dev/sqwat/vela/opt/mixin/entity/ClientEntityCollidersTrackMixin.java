package dev.sqwat.vela.opt.mixin.entity;

import dev.sqwat.vela.opt.entity.Colliders;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the client level's collidable entities ({@link Colliders}) as entities come into and leave tracking. */
@Mixin(targets="net.minecraft.client.multiplayer.ClientLevel$EntityCallbacks")
public abstract class ClientEntityCollidersTrackMixin {
    @Inject(method="onTrackingStart(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"))
    private void vela$start(Entity entity,CallbackInfo ci){Colliders.tracked(entity);}

    @Inject(method="onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"))
    private void vela$end(Entity entity,CallbackInfo ci){Colliders.untracked(entity);}
}
