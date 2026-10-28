package dev.sqwat.vela.mixin.perf;

import dev.sqwat.vela.render.perf.GuiSortOrder;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.Comparator;

@Mixin(GuiRenderer.class)
public abstract class GuiSortAllocationMixin {
    @ModifyArg(method="prepare",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/state/gui/GuiRenderState;sortElements(Ljava/util/Comparator;)V"),index=0)
    private Comparator<GuiElementRenderState> vela$primitiveSortKeys(Comparator<GuiElementRenderState> original){
        return dev.sqwat.vela.module.Optimization.fewerAllocationsOn()?GuiSortOrder.ELEMENT:original;
    }
}
