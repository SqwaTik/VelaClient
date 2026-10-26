package dev.sqwat.vela.mixin.world;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LightmapRenderStateExtractor.class)
abstract class GammaMixin {
    @Inject(method="extract",at=@At("TAIL"))
    private void vela$gamma(LightmapRenderState state,float partial,CallbackInfo ci){if(state.needsUpdate)dev.sqwat.vela.module.utility.Gamma.apply(state);}
}
