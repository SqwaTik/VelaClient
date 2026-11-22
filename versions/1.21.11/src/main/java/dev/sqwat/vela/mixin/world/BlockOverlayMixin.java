package dev.sqwat.vela.mixin.world;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 1.21.11: the outline is extracted, then drawn twice (solid and translucent pass); the overlay replaces both draws. */
@Mixin(LevelRenderer.class)
abstract class BlockOverlayMixin {
    @Inject(method="extractBlockOutline",at=@At("TAIL"))
    private void vela$blockOverlay(Camera camera,LevelRenderState state,CallbackInfo ci){
        var overlay=dev.sqwat.vela.module.utility.BlockOverlay.active();
        if(overlay==null)return;
        try(var collection=((LevelRenderer)(Object)this).collectPerFrameGizmos()){
            overlay.submit(state.blockOutlineRenderState);
        }
    }
    @Inject(method="renderBlockOutline",at=@At("HEAD"),cancellable=true)
    private void vela$hideVanilla(CallbackInfo ci){
        if(dev.sqwat.vela.module.utility.BlockOverlay.active()!=null)ci.cancel();
    }
}
