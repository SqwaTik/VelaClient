package dev.sqwat.vela.opt.mixin.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderTarget.class)
public interface RenderTargetAccessor {
	@Accessor("colorTexture")
	GpuTexture velaopt$colorTexture();

	@Accessor("colorTexture")
	void velaopt$colorTexture(GpuTexture texture);

	@Accessor("colorTextureView")
	GpuTextureView velaopt$colorTextureView();

	@Accessor("colorTextureView")
	void velaopt$colorTextureView(GpuTextureView view);

	@Accessor("depthTexture")
	GpuTexture velaopt$depthTexture();

	@Accessor("depthTexture")
	void velaopt$depthTexture(GpuTexture texture);

	@Accessor("depthTextureView")
	GpuTextureView velaopt$depthTextureView();

	@Accessor("depthTextureView")
	void velaopt$depthTextureView(GpuTextureView view);
}
