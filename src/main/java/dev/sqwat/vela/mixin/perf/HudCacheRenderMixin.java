package dev.sqwat.vela.mixin.perf;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.render.perf.HudCache;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
/**
 * HUD cache: on refresh frames the extracted HUD draws into the texture; every frame the live parts and the cached
 * texture then draw onto the screen in a second, small GUI pass.
 */
@Mixin(GameRenderer.class)
abstract class HudCacheRenderMixin {
    @Shadow @Final private Minecraft minecraft;
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void vela$cachedGui(GuiRenderer renderer,Operation<Void> original,DeltaTracker delta,boolean advanceGameTime){
        if(!HudCache.active()){original.call(renderer);return;}
        if(HudCache.refresh())HudCache.drawIntoTexture(()->original.call(renderer));
        // After a refresh the GUI state is empty again; on reused frames it holds whatever was extracted outside the HUD.
        HudCache.extractLive(minecraft,new GuiGraphicsExtractor(minecraft,((GameRenderer)(Object)this).gameRenderState().guiRenderState,0,0),delta);
        original.call(renderer);
    }
}
