package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.ui.ChatPrivacy;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
/** Chat privacy: the visible text of the chat input is masked before formatting; the value itself is untouched. */
@Mixin(EditBox.class)
abstract class ChatPrivacyMixin {
    @Shadow private String value;
    @ModifyVariable(method="applyFormat",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private String vela$mask(String text,String original,int offset){
        int from=ChatPrivacy.hiddenFrom((EditBox)(Object)this,value);
        return from<0?text:ChatPrivacy.mask(text,offset,from);
    }
}
