package dev.sqwat.vela.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.sqwat.vela.opt.entity.UploadSort;

// the draw's quad sorting is decided once, when the draw is made: no sorting asked for, none done on upload
@Mixin(targets = "net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group")
public class FeatureGroupSortMixin {
	@WrapOperation(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderType;sortOnUpload()Z"))
	private boolean velaopt$unsorted(RenderType type, Operation<Boolean> original) {
		return original.call(type) && UploadSort.sorted(type);
	}
}
