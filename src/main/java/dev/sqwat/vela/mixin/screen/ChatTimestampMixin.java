package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.module.utility.ChatStack;
import dev.sqwat.vela.module.utility.ChatTimestamps;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
/**
 * Every chat line (player, server and client messages) passes through addMessage. Stacking compares the raw text
 * first; the timestamp is added afterwards so it never breaks the comparison.
 */
@Mixin(ChatComponent.class)
abstract class ChatTimestampMixin {
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow private void refreshTrimmedMessages(){throw new AssertionError();}
    @Unique private final boolean[] vela$restack=new boolean[1];
    @ModifyVariable(method="addMessage",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Component vela$decorate(Component message){
        return ChatTimestamps.decorate(ChatStack.stack(message,allMessages,vela$restack));
    }
    @Inject(method="addMessage",at=@At("TAIL"))
    private void vela$afterAdd(CallbackInfo ci){
        if(vela$restack[0]){vela$restack[0]=false;refreshTrimmedMessages();}
        ChatStack.added(allMessages);
    }
}
