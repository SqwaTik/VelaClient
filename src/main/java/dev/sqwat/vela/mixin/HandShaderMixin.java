package dev.sqwat.vela.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.render.HandShaderRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(GameRenderer.class)
abstract class HandShaderMixin {
    @WrapOperation(method="renderItemInHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V"))
    private void vela$hands(FeatureRenderDispatcher dispatcher,SubmitNodeStorage nodes,Operation<Void> original){
        HandShaderRenderer.render(()->original.call(dispatcher,nodes));
    }
}
