package dev.sqwat.vela.mixin.camera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.module.utility.Freelook;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Only the main camera following the local player uses the Freelook angles; vanilla collision and zoom stay. */
@Mixin(Camera.class)
abstract class FreelookCameraMixin {
    @WrapOperation(method="alignWithEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float vela$yaw(Entity entity,float partial,Operation<Float> original){
        return Freelook.active()&&entity==Minecraft.getInstance().player?Freelook.yaw():original.call(entity,partial);
    }
    @WrapOperation(method="alignWithEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float vela$pitch(Entity entity,float partial,Operation<Float> original){
        return Freelook.active()&&entity==Minecraft.getInstance().player?Freelook.pitch():original.call(entity,partial);
    }
}
