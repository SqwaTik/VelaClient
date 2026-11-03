package dev.sqwat.vela.mixin.perf;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** HUD cache: the parts that blend with the world, drawn live every frame. */
@Mixin(Hud.class)
public interface HudLiveInvoker {
    @Invoker("extractCameraOverlays") void vela$extractCameraOverlays(GuiGraphicsExtractor g,DeltaTracker delta);
    @Invoker("extractCrosshair") void vela$extractCrosshair(GuiGraphicsExtractor g,DeltaTracker delta);
}
