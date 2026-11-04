package dev.sqwat.vela.mixin.camera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 1.21.11: Camera.setup places the camera (26.x: alignWithEntity, update); FOV and zoom are a GameRenderer mixin. */
@Mixin(Camera.class)
public abstract class BuiltinCameraMixin {
    @Unique private float vela$distance;
    @Unique private long vela$last;
    @Unique private Object vela$perspective;
    @Inject(method="setup",at=@At("HEAD"))
    private void vela$perspective(Level level,Entity entity,boolean detached,boolean mirror,float partialTicks,CallbackInfo ci){
        var mode=Minecraft.getInstance().options.getCameraType();
        if(mode!=vela$perspective){vela$distance=0;vela$perspective=mode;vela$last=System.nanoTime();}
    }
    @Inject(method="setup",at=@At("RETURN"))
    private void vela$visibilityFrame(Level level,Entity entity,boolean detached,boolean mirror,float partialTicks,CallbackInfo ci){
        if((Object)this==Minecraft.getInstance().gameRenderer.getMainCamera())dev.sqwat.vela.render.entity.AutomaticVisibility.beginFrame();
    }
    // Ease the desired distance before vanilla's collision test, preserving wall protection.
    @ModifyArg(method="setup",at=@At(value="INVOKE",target="Lnet/minecraft/client/Camera;getMaxZoom(F)F"),index=0)
    private float vela$distance(float desired){
        long now=System.nanoTime();float dt=Math.min(.05f,(now-vela$last)/1e9f);vela$last=now;
        vela$distance+=(desired-vela$distance)*(float)(1-Math.exp(-9*dt));return vela$distance;
    }
}
