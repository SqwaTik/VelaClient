package dev.sqwat.vela.mixin.screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Container window bounds; read every frame because the recipe book moves the window. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int vela$left();
    @Accessor("topPos") int vela$top();
    @Accessor("imageWidth") int vela$width();
}
