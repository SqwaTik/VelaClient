package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.module.utility.ContainerSearch;
import dev.sqwat.vela.ui.render.VelaTheme;
import dev.sqwat.vela.ui.screen.ContainerSearchBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Container Search highlight. Drawn inside the window's translated pose, after the items and before the hovered-slot
 * highlight and the carried item, so neither of those is dimmed.
 */
@Mixin(AbstractContainerScreen.class)
abstract class ContainerSearchMixin extends Screen {
    @Shadow @Final protected AbstractContainerMenu menu;
    private ContainerSearchMixin(Component title){super(title);}

    @Inject(method="extractContents",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightFront(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void vela$highlight(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial,CallbackInfo ci){
        String query=ContainerSearchBox.query(this);
        if(query.isBlank())return;
        int accent=VelaTheme.PRIMARY|0xFF000000;
        for(var slot:menu.slots){
            if(!slot.isActive())continue;
            int x=slot.x,y=slot.y;
            if(ContainerSearch.matches(slot.getItem(),query)){
                g.fill(x-1,y-1,x+17,y,accent);g.fill(x-1,y+16,x+17,y+17,accent);
                g.fill(x-1,y,x,y+16,accent);g.fill(x+16,y,x+17,y+16,accent);
            }else g.fill(x,y,x+16,y+16,0xB80B0D12);
        }
    }
}
