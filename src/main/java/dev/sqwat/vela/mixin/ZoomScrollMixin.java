package dev.sqwat.vela.mixin;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
public abstract class ZoomScrollMixin {
    @org.spongepowered.asm.mixin.injection.ModifyArgs(method="turnPlayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void vela$zoomSensitivity(org.spongepowered.asm.mixin.injection.invoke.arg.Args args){
        double scale=dev.sqwat.vela.render.BuiltinZoom.sensitivityFactor();
        double yaw=(double)args.get(0)*scale,pitch=(double)args.get(1)*scale;
        args.set(0,yaw);args.set(1,pitch);
    }
    @Inject(method="onScroll",at=@At("HEAD"),cancellable=true)
    private void vela$zoomScroll(long handle,double horizontal,double vertical,CallbackInfo ci){
        if(handle==Minecraft.getInstance().getWindow().handle()&&dev.sqwat.vela.render.BuiltinZoom.held()){
            dev.sqwat.vela.render.BuiltinZoom.scroll(vertical);ci.cancel();
        }
    }
}
