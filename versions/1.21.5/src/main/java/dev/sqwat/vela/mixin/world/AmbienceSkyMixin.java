package dev.sqwat.vela.mixin.world;
import dev.sqwat.vela.module.visual.Ambience;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 1.21.5 has no sky render state: the colour the level hands to the sky renderer is tinted instead. */
@Mixin(ClientLevel.class)
abstract class AmbienceSkyMixin {
    @Inject(method="getSkyColor",at=@At("RETURN"),cancellable=true)
    private void vela$sky(net.minecraft.world.phys.Vec3 position,float partial,CallbackInfoReturnable<Integer> ci){
        var a=Ambience.current();if(a!=null)ci.setReturnValue(a.skyColor(ci.getReturnValueI()));
    }
}
