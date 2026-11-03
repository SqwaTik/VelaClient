package dev.sqwat.vela.opt.mixin.hud;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.opt.hud.GuiItemRefresh;

@Mixin(GuiRenderer.class)
public class GuiRendererAnimatedMixin {
	@Inject(method = "prepareItemElements", at = @At("HEAD"))
	private void velaopt$decideRefresh(CallbackInfo ci) {
		GuiItemRefresh.decide();
	}
}
