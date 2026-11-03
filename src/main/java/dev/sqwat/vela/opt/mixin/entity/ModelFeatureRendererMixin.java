package dev.sqwat.vela.opt.mixin.entity;

import dev.sqwat.vela.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.entity.Entity;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.opt.entity.AnimFreeze;
import dev.sqwat.vela.opt.entity.ParallelModelHook;
import dev.sqwat.vela.opt.entity.StateEntity;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {
	@Inject(method = "buildGroup", at = @At("HEAD"), cancellable = true)
	private void velaopt$parallel(FeatureFrameContext context, List<ModelFeatureRenderer.Submit<?>> submits, CallbackInfo ci) {
		if (ParallelModelHook.buildGroup((ModelFeatureRenderer) (Object) this, submits)) {
			ci.cancel();
		}
	}

	// the buffer may already be sprite-wrapped; the replayed vertices go through it, so Sodium's push remaps their UVs
	@WrapOperation(method = "prepareModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
	private void velaopt$frozenAnimation(Model<?> model, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color, Operation<Void> original,
										 ModelFeatureRenderer.Submit<?> submit) {
		if (Features.ENTITY_ANIM_FREEZE.on && submit.sheetedDecalPose() == null && submit.state() instanceof EntityRenderState state) {
			Entity entity = ((StateEntity) state).velaopt$entity();
			if (entity != null && AnimFreeze.render(entity, model, submit.renderType(), null, poseStack.last().pose(), consumer,
					target -> original.call(model, poseStack, target, light, overlay, color))) {
				return;
			}
		}
		original.call(model, poseStack, consumer, light, overlay, color);
	}
}
