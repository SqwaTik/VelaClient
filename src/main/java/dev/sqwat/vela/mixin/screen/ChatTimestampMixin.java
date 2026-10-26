package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.module.utility.ChatTimestamps;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
/** Every chat line (player, server and client messages) passes through addMessage. */
@Mixin(ChatComponent.class)
abstract class ChatTimestampMixin {
    @ModifyVariable(method="addMessage",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Component vela$timestamp(Component message){return ChatTimestamps.decorate(message);}
}
