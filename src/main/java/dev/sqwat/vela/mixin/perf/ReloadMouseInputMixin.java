package dev.sqwat.vela.mixin.perf;
import dev.sqwat.vela.render.resource.InteractiveReload;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(MouseHandler.class)
abstract class ReloadMouseInputMixin {
    @Redirect(method={"onButton","onScroll","handleAccumulatedMovement"},at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;"))
    private Overlay vela$input(Gui gui){return InteractiveReload.inputOverlay(gui.overlay());}
}
