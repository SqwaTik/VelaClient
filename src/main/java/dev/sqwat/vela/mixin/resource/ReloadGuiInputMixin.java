package dev.sqwat.vela.mixin.resource;
import dev.sqwat.vela.render.resource.InteractiveReload;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Gui.class)
abstract class ReloadGuiInputMixin {
    @Redirect(method="handleKeybinds",at=@At(value="FIELD",target="Lnet/minecraft/client/gui/Gui;overlay:Lnet/minecraft/client/gui/screens/Overlay;",opcode=180))
    private Overlay vela$input(Gui gui){return InteractiveReload.inputOverlay(gui.overlay());}
}
