package dev.sqwat.vela.mixin;

import dev.sqwat.vela.render.NameTagTextCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NameTagFeatureRenderer.class)
abstract class NameTagTextCacheMixin {
    @Inject(method = "prepareText", at = @At("HEAD"), cancellable = true)
    private static void vela$reuseLayout(Font font, NameTagFeatureRenderer.Submit tag,
                                         CallbackInfoReturnable<Font.PreparedText> ci) {
        ci.setReturnValue(NameTagTextCache.prepare(font, tag.text().getVisualOrderText(),
            tag.x(), tag.y(), tag.color(), dev.sqwat.vela.render.FriendNameTags.background(tag.text(),tag.backgroundColor())));
    }
}
