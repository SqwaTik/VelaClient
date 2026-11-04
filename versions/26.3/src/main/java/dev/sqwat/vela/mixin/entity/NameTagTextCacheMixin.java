package dev.sqwat.vela.mixin.entity;

import dev.sqwat.vela.render.entity.NameTagTextCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 26.3: name tags are plain text submits drawn by {@link TextFeatureRenderer}; the friend background is chosen when
 * the tag is submitted ({@code NameTagFriendBackgroundMixin}). Layouts are reused for every shadowless world text.
 */
@Mixin(TextFeatureRenderer.class)
abstract class NameTagTextCacheMixin {
    @Redirect(method = "renderText", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"))
    private static Font.PreparedText vela$reuseLayout(Font font, FormattedCharSequence text, float x, float y, int color,
                                                      boolean dropShadow, boolean seeThrough, int background) {
        if (dropShadow || seeThrough) return font.prepareText(text, x, y, color, dropShadow, seeThrough, background);
        return NameTagTextCache.prepare(font, text, x, y, color, background);
    }
}
