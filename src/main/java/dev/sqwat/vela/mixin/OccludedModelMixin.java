package dev.sqwat.vela.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sqwat.vela.render.OccludedRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** An occluded player skips model, armour, held items and cape; EntityRenderer.submit still adds name tag and leash. */
@Mixin(LivingEntityRenderer.class)
abstract class OccludedModelMixin<T extends LivingEntity,S extends LivingEntityRenderState,M extends EntityModel<? super S>> extends EntityRenderer<T,S> {
    private OccludedModelMixin(EntityRendererProvider.Context context){super(context);}
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("HEAD"),cancellable=true)
    private void vela$occludedModel(S state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci){
        if(!((OccludedRenderState)state).vela$occluded())return;
        super.submit(state,pose,collector,camera);
        ci.cancel();
    }
}
