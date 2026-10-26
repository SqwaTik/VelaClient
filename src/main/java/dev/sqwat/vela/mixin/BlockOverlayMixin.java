package dev.sqwat.vela.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
abstract class BlockOverlayMixin {
    @Inject(method="submitBlockOutline",at=@At("HEAD"),cancellable=true)
    private void vela$blockOverlay(PoseStack pose,SubmitNodeCollector collector,LevelRenderState state,CallbackInfo ci){
        var overlay=dev.sqwat.vela.module.BlockOverlay.active();
        if(overlay==null)return;
        try(var collection=((LevelRenderer)(Object)this).collectPerFrameRenderThreadGizmos()){
            if(overlay!=null)overlay.submit(state.blockOutlineRenderState);
        }
        if(overlay!=null)ci.cancel();
    }
}
