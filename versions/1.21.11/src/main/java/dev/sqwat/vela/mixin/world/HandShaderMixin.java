package dev.sqwat.vela.mixin.world;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.render.shader.HandShaderRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** 1.21.11: the dispatcher renders its own submit storage. */
@Mixin(GameRenderer.class)
abstract class HandShaderMixin {
    @WrapOperation(method="renderItemInHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures()V"))
    private void vela$hands(FeatureRenderDispatcher dispatcher,Operation<Void> original){
        HandShaderRenderer.render(()->original.call(dispatcher));
    }
}
