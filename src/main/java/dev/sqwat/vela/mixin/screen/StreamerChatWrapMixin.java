package dev.sqwat.vela.mixin.screen;

import dev.sqwat.vela.module.utility.StreamerMode;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Replace before wrapping so a nickname cannot leak as fragments at a chat line boundary. */
@Mixin(ComponentRenderUtils.class)
abstract class StreamerChatWrapMixin {
    @ModifyVariable(method="wrapComponents",at=@At("HEAD"),argsOnly=true)
    private static FormattedText vela$wrap(FormattedText text){
        var mask=StreamerMode.mask();return mask==null?text:mask.formatted(text);
    }
}
