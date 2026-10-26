package dev.sqwat.vela.mixin.perf;

import dev.sqwat.vela.render.entity.NameTagTextCache;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontManager.class)
abstract class FontCacheInvalidationMixin {
    @Inject(method = {"apply", "updateOptions", "close"}, at = @At("HEAD"))
    private void vela$invalidateLabels(CallbackInfo ci) { NameTagTextCache.clear(); dev.sqwat.vela.render.perf.SignTextWidths.clear(); }
}
