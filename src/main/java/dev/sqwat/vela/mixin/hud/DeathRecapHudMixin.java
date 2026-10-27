package dev.sqwat.vela.mixin.hud;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.module.visual.DeathReplay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** During a Death Recap replay the whole HUD pass is skipped: vanilla elements, the crosshair and Vela's own layers. */
@Mixin(Gui.class)
abstract class DeathRecapHudMixin {
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void vela$hideDuringReplay(Hud hud,GuiGraphicsExtractor g,DeltaTracker delta,Operation<Void> original){
        if(!DeathReplay.active()||DeathReplay.showsHud())original.call(hud,g,delta);
    }
}
