package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.module.utility.ChatTweaks;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
/**
 * Every chat line (player, server and client messages) passes through addMessage, so the Chat module decorates it
 * there. The history cap of 100 lives in the two queue methods.
 */
@Mixin(ChatComponent.class)
abstract class ChatTweaksMixin {
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow private void refreshTrimmedMessages(){throw new AssertionError();}
    @Unique private final boolean[] vela$restack=new boolean[1];
    @ModifyVariable(method="addMessage",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Component vela$decorate(Component message){
        return ChatTweaks.decorate(message,allMessages,vela$restack);
    }
    @Inject(method="addMessage",at=@At("TAIL"))
    private void vela$afterAdd(CallbackInfo ci){
        if(vela$restack[0]){vela$restack[0]=false;refreshTrimmedMessages();}
        ChatTweaks.added(allMessages);
    }
    @ModifyConstant(method={"addMessageToDisplayQueue","addMessageToQueue"},constant=@Constant(intValue=100))
    private int vela$history(int vanilla){return ChatTweaks.historyLimit();}
}
