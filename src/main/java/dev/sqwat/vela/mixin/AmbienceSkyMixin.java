package dev.sqwat.vela.mixin;
import dev.sqwat.vela.module.Ambience;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SkyRenderer.class)
abstract class AmbienceSkyMixin {
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void vela$sky(ClientLevel level,float partial,Camera camera,SkyRenderState state,CallbackInfo ci){
        var a=Ambience.current();if(a!=null)state.skyColor=a.skyColor(state.skyColor);
    }
}
