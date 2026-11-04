package dev.sqwat.vela.opt.mixin.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.opt.bench.MonitorScreen;

// 1.21.11 has no HUD cache (its Gui draws straight into the GUI render state): only the benchmark monitor is drawn over the frame
@Mixin(GameRenderer.class)
public class GameRendererHudMixin {
	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	@Final
	GuiRenderState guiRenderState;

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
	private void velaopt$monitor(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
		if (MonitorScreen.shown() != null && !minecraft.options.hideGui) {
			MonitorScreen.shown().drawOver(new GuiGraphics(minecraft, guiRenderState, 0, 0));
		}
	}
}
