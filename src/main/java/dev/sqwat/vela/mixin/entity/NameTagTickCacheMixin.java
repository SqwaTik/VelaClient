package dev.sqwat.vela.mixin.entity;

import dev.sqwat.vela.render.entity.NameTagTextCache;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Builds each entity's display name once per tick instead of once per frame. */
@Mixin(EntityRenderer.class)
abstract class NameTagTickCacheMixin {
    @Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
    private void vela$cached(Entity entity, CallbackInfoReturnable<Component> ci) {
        Component name = NameTagTextCache.name(entity);
        if (name != null) ci.setReturnValue(name);
    }
    @Inject(method = "getNameTag", at = @At("RETURN"))
    private void vela$remember(Entity entity, CallbackInfoReturnable<Component> ci) {
        NameTagTextCache.remember(entity, ci.getReturnValue());
    }
}
