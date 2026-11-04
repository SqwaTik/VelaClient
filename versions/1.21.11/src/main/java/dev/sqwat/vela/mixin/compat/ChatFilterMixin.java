package dev.sqwat.vela.mixin.compat;
import dev.sqwat.vela.compat.ChatFilterAccess;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Predicate;

/** Lines the filter rejects stay in the history but are not laid out for display. */
@Mixin(ChatComponent.class)
abstract class ChatFilterMixin implements ChatFilterAccess {
    @Unique private Predicate<GuiMessage> vela$filter=message->true;
    @Shadow private void refreshTrimmedMessages(){}
    @Override public void vela$setVisibleMessageFilter(Predicate<GuiMessage> filter){vela$filter=filter;refreshTrimmedMessages();}
    @Inject(method="addMessageToDisplayQueue",at=@At("HEAD"),cancellable=true)
    private void vela$filter(GuiMessage message,CallbackInfo ci){if(!vela$filter.test(message))ci.cancel();}
}
