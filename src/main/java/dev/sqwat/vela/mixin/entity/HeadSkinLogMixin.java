package dev.sqwat.vela.mixin.entity;

import dev.sqwat.vela.render.entity.HeadSkinLog;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Logs each distinct head profile once, with how its skin lookup ended (see HeadSkinLog). */
@Mixin(PlayerSkinRenderCache.class)
abstract class HeadSkinLogMixin {
    @Inject(method="lookup",at=@At("RETURN"))
    private void vela$logLookup(ResolvableProfile profile,CallbackInfoReturnable<CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>> ci){
        HeadSkinLog.lookup(profile,ci.getReturnValue());
    }
    // Skulls and heads in hand go through createLookup, which reads the cache without calling lookup.
    @Inject(method="createLookup",at=@At("HEAD"))
    private void vela$logCreateLookup(ResolvableProfile profile,CallbackInfoReturnable<Supplier<PlayerSkinRenderCache.RenderInfo>> ci){
        HeadSkinLog.lookup(profile,((PlayerSkinRenderCache)(Object)this).lookup(profile));
    }
}
