package dev.sqwat.vela.mixin.screen;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.sqwat.vela.friend.VelaUsers;
import net.minecraft.client.Options;
import net.minecraft.server.level.ClientInformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Sends the Vela mark with the client settings: the spare skin-parts bit, invisible to vanilla. */
@Mixin(Options.class)
abstract class VelaMarkMixin {
    @ModifyReturnValue(method="buildPlayerInformation",at=@At("RETURN"))
    private ClientInformation vela$mark(ClientInformation info){
        return new ClientInformation(info.language(),info.viewDistance(),info.chatVisibility(),info.chatColors(),info.modelCustomisation()|VelaUsers.MARK,
            info.mainHand(),info.textFilteringEnabled(),info.allowsListing(),info.particleStatus());
    }
}
