package dev.sqwat.vela.mixin;

import dev.sqwat.vela.module.Far;
import de.johni0702.minecraft.bobby.FakeChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=FakeChunkManager.class,remap=false)
abstract class FarCacheMixin {
    @Inject(method="load",at=@At("HEAD"),cancellable=true)
    private void vela$rejectDisabledLoad(int x,int z,net.minecraft.world.level.chunk.LevelChunk chunk,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){if(!Far.active())ci.cancel();}
    @Inject(method="getChunk",at=@At("HEAD"),cancellable=true)
    private void vela$hideDisabled(int x,int z,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.level.chunk.LevelChunk> ci){if(!Far.active())ci.setReturnValue(null);}
    @Inject(method="shouldBeLoaded",at=@At("HEAD"),cancellable=true)
    private void vela$noDisabledReplacement(int x,int z,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){if(!Far.active())ci.setReturnValue(false);}
    @ModifyVariable(method="update(ZLjava/util/function/BooleanSupplier;I)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private int vela$cachedDistance(int distance){return Far.active()?Math.min(64,distance):0;}
}
