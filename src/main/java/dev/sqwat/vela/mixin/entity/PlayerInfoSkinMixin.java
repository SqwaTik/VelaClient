package dev.sqwat.vela.mixin.entity;

import dev.sqwat.vela.friend.LocalAppearanceService;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void vela$localAppearance(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerInfo info = (PlayerInfo) (Object) this;
        cir.setReturnValue(LocalAppearanceService.apply(info.getProfile().id(), cir.getReturnValue()));
    }
}
