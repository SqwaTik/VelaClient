package dev.sqwat.vela.mixin.upscale;

import com.mojang.blaze3d.textures.GpuSampler;
import dev.sqwat.vela.upscale.UpscalerRuntime;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Frame generation pacing: the real frame waiting to be shown goes out right after the solid
 * terrain is drawn, i.e. in the middle of the world's GPU work. On the GPU timeline that lands
 * between the two generated frames, which CPU-side check points could not do.
 */
@Mixin(ChunkSectionsToRender.class)
public abstract class ChunkSectionsToRenderMixin {
	@Inject(method = "renderGroup", at = @At("RETURN"))
	private void minesr$afterSolidTerrain(ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
		if (group == ChunkSectionLayerGroup.OPAQUE) {
			UpscalerRuntime.presentPendingFrameMidWorld();
		}
	}
}
