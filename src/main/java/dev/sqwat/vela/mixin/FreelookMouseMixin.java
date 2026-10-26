package dev.sqwat.vela.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.module.Freelook;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** While Freelook is on, mouse movement turns the camera instead of the player. */
@Mixin(MouseHandler.class)
abstract class FreelookMouseMixin {
    @WrapOperation(method="turnPlayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void vela$freelook(LocalPlayer player,double yaw,double pitch,Operation<Void> original){
        if(!Freelook.turn(yaw,pitch))original.call(player,yaw,pitch);
    }
}
