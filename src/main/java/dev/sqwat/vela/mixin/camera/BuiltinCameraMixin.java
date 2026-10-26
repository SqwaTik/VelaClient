package dev.sqwat.vela.mixin.camera;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class BuiltinCameraMixin {
    @org.spongepowered.asm.mixin.Shadow private float fovModifier;
    @org.spongepowered.asm.mixin.Shadow private float oldFovModifier;
    @Inject(method="tickFov",at=@At("RETURN"))
    private void vela$stableFov(CallbackInfo ci){if(dev.sqwat.vela.module.utility.RenderTweaks.hide("dynamic_fov"))fovModifier=oldFovModifier=1;}
    @Inject(method="update",at=@At("RETURN"))
    private void vela$visibilityFrame(net.minecraft.client.DeltaTracker delta,CallbackInfo ci){
        if((Object)this==Minecraft.getInstance().gameRenderer.mainCamera())dev.sqwat.vela.render.entity.AutomaticVisibility.beginFrame();
    }
    @Unique private float vela$distance;
    @Unique private long vela$last;
    @Unique private Object vela$perspective;
    @Inject(method="alignWithEntity",at=@At("HEAD"))
    private void vela$perspective(float partialTicks,CallbackInfo ci){
        var mode=Minecraft.getInstance().options.getCameraType();
        if(mode!=vela$perspective){vela$distance=0;vela$perspective=mode;vela$last=System.nanoTime();}
    }
    // Ease the desired distance before vanilla's collision test, preserving wall protection.
    @ModifyArg(method="alignWithEntity",at=@At(value="INVOKE",target="Lnet/minecraft/client/Camera;getMaxZoom(F)F"),index=0)
    private float vela$distance(float desired){
        long now=System.nanoTime();float dt=Math.min(.05f,(now-vela$last)/1e9f);vela$last=now;
        vela$distance+=(desired-vela$distance)*(float)(1-Math.exp(-9*dt));return vela$distance;
    }
    @Inject(method="calculateFov",at=@At("RETURN"),cancellable=true)
    private void vela$zoom(float partialTicks,CallbackInfoReturnable<Float> ci){
        ci.setReturnValue(ci.getReturnValue()*dev.sqwat.vela.render.camera.BuiltinZoom.factor());
    }
}
