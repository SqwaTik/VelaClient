package dev.sqwat.vela.mixin.hud;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.module.utility.ChatTweaks;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Chat module "keep on disconnect": skips the clear and leaves a divider instead. */
@Mixin(Hud.class)
abstract class ChatKeepMixin {
    @WrapOperation(method="onDisconnected",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"))
    private void vela$keep(ChatComponent chat,boolean history,Operation<Void> original){
        if(!ChatTweaks.keepOnDisconnect()){original.call(chat,history);return;}
        chat.addClientSystemMessage(Component.literal(dev.sqwat.vela.ui.Lang.tr("──── Отключено от сервера ────")).withColor(0x5C6680));
    }
}
