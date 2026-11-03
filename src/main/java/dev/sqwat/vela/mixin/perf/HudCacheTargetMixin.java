package dev.sqwat.vela.mixin.perf;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.sqwat.vela.render.perf.HudCache;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** HUD cache: the GUI renderer draws into the HUD texture while the cached HUD is drawn. */
@Mixin(GuiRenderer.class)
abstract class HudCacheTargetMixin {
    @WrapOperation(method="draw",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private RenderTarget vela$hudTarget(GameRenderer gameRenderer,Operation<RenderTarget> original){return HudCache.drawTarget(original.call(gameRenderer));}
}
