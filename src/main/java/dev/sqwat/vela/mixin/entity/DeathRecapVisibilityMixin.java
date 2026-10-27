package dev.sqwat.vela.mixin.entity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.sqwat.vela.module.visual.DeathReplay;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Real copies of replayed entities stay hidden while their ghosts play. Allocation-free: runs per entity per frame. */
@Mixin(EntityRenderDispatcher.class)
abstract class DeathRecapVisibilityMixin {
    @ModifyReturnValue(method="shouldRender",at=@At("RETURN"))
    private boolean vela$hideReplayed(boolean visible,Entity entity){return visible&&!(DeathReplay.active()&&DeathReplay.hides(entity));}
}
