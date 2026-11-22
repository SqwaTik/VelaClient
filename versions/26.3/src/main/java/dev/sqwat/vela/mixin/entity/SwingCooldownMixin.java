package dev.sqwat.vela.mixin.entity;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 26.3: the held item's swap height is ticked by FirstPersonHandsAndItems. */
@Mixin(FirstPersonHandsAndItems.class)
abstract class SwingCooldownMixin {
    /**
     * Hits on air and on mobs reset the attack cooldown, and vanilla then lowers the held item and raises it again.
     * That dip drowned out the custom swing (mining has no cooldown, so it looked right only there); while a custom
     * mode is on the item stays up. Switching items still plays the normal swap.
     */
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="tick",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    private float vela$noCooldownDip(float scale){
        var services=dev.sqwat.vela.VelaClient.services();
        return services!=null&&services.modules().byId("swing_animation") instanceof dev.sqwat.vela.module.visual.SwingAnimation module
            &&module.customMode()?1:scale;
    }
}
