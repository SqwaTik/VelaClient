package dev.sqwat.vela.mixin.world;

import de.johni0702.minecraft.bobby.BobbyConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=BobbyConfig.class,remap=false)
abstract class FarConfigMixin {
    @Inject(method="getMaxRenderDistance",at=@At("HEAD"),cancellable=true)
    private void vela$maximumDistance(CallbackInfoReturnable<Integer> ci){ci.setReturnValue(64);}
    @Inject(method="getUnloadDelaySecs",at=@At("HEAD"),cancellable=true)
    private void vela$unloadPromptly(CallbackInfoReturnable<Integer> ci){ci.setReturnValue(0);}
    @Inject(method="isNoBlockEntities",at=@At("HEAD"),cancellable=true)
    private void vela$terrainOnly(CallbackInfoReturnable<Boolean> ci){ci.setReturnValue(true);}
    @Inject(method="isDynamicMultiWorld",at=@At("HEAD"),cancellable=true)
    private void vela$separateWorldCaches(CallbackInfoReturnable<Boolean> ci){ci.setReturnValue(true);}
}
