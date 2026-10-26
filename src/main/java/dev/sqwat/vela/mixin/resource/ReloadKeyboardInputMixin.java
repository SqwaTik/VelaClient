package dev.sqwat.vela.mixin.resource;
import dev.sqwat.vela.render.resource.InteractiveReload;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(KeyboardHandler.class)
abstract class ReloadKeyboardInputMixin {
    @Redirect(method={"charTyped","preeditCallback"},at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;"))
    private Overlay vela$input(Gui gui){return InteractiveReload.inputOverlay(gui.overlay());}
}
