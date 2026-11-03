package dev.sqwat.vela.opt.mixin.entity;

import dev.sqwat.vela.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.sqwat.vela.opt.entity.PartIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

// every part's for-each over its children made an iterator per part per frame (a fifth of the render thread's garbage on a farm);
// each part keeps one, safe because a part is never inside its own subtree
@Mixin(ModelPart.class)
public class ModelPartChildrenMixin {
	@Shadow
	@Final
	private Map<String, ModelPart> children;
	@Unique
	private PartIterator velaopt$children;

	@WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
			at = @At(value = "INVOKE", target = "Ljava/util/Collection;iterator()Ljava/util/Iterator;"))
	private Iterator<ModelPart> velaopt$children(Collection<ModelPart> values, Operation<Iterator<ModelPart>> original) {
		if (!Features.FEWER_ALLOCATIONS.on || !RenderSystem.isOnRenderThread()) {
			return original.call(values);
		}
		PartIterator it = velaopt$children;
		// a mod may add children after baking
		if (it == null || it.parts.length != children.size()) {
			velaopt$children = it = new PartIterator(values.toArray(new ModelPart[0]));
		}
		it.next = 0;
		return it;
	}
}
