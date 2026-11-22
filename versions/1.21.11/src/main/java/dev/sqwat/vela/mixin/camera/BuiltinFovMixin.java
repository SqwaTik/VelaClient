package dev.sqwat.vela.mixin.camera;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 1.21.11: the FOV modifier and FOV live in GameRenderer (26.x: Camera). Stable FOV and the built-in zoom. */
@Mixin(GameRenderer.class)
abstract class BuiltinFovMixin {
    @Shadow private float fovModifier;
    @Shadow private float oldFovModifier;
    @Inject(method="tickFov",at=@At("RETURN"))
    private void vela$stableFov(CallbackInfo ci){if(dev.sqwat.vela.module.utility.RenderTweaks.hide("dynamic_fov"))fovModifier=oldFovModifier=1;}
    @Inject(method="getFov",at=@At("RETURN"),cancellable=true)
    private void vela$zoom(Camera camera,float partialTicks,boolean useFovSetting,CallbackInfoReturnable<Float> ci){
        ci.setReturnValue(ci.getReturnValue()*dev.sqwat.vela.render.camera.BuiltinZoom.factor());
    }
}
