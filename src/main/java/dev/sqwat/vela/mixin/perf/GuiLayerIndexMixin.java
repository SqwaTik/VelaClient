package dev.sqwat.vela.mixin.perf;

import dev.sqwat.vela.render.perf.IndexedAreaList;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;

@Mixin(GuiRenderState.class)
public abstract class GuiLayerIndexMixin {
    @Redirect(method="navigateToAboveHighestElementWithIntersectingBounds",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/state/gui/GuiRenderState;hasIntersection(Lnet/minecraft/client/gui/navigation/ScreenRectangle;Ljava/util/List;)Z"),require=0)
    private boolean vela$indexedIntersection(GuiRenderState self,ScreenRectangle bounds,List<? extends ScreenArea> states){
        if(states==null)return false;
        if(states instanceof IndexedAreaList<?> indexed)return indexed.intersects(bounds);
        for(ScreenArea area:states){ScreenRectangle existing=area.bounds();if(existing!=null&&existing.intersects(bounds))return true;}
        return false;
    }
}
