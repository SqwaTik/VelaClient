package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.module.utility.ContainerSearch;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/**
 * Container Search: a box above the container. While it is focused it keeps all keys except Escape, so typing "e" or a
 * number does not close the screen or swap hotbar items. Creative inventory has its own search and is left alone.
 */
@Mixin(AbstractContainerScreen.class)
abstract class ContainerSearchMixin extends Screen {
    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Unique private EditBox vela$search;
    private ContainerSearchMixin(Component title){super(title);}

    @Inject(method="init",at=@At("TAIL"))
    private void vela$addSearch(CallbackInfo ci){
        var module=ContainerSearch.current();
        if(module==null||(Object)this instanceof CreativeModeInventoryScreen){vela$search=null;return;}
        String previous=vela$search==null?module.initialQuery():vela$search.getValue();
        int w=Math.min(imageWidth,150);
        vela$search=new EditBox(font,leftPos+(imageWidth-w)/2,Math.max(2,topPos-16),w,13,Component.literal("Поиск"));
        vela$search.setHint(Component.literal("Поиск предметов…"));
        vela$search.setMaxLength(40);
        vela$search.setValue(previous);
        vela$search.setResponder(module::query);
        addRenderableWidget(vela$search);
    }
    @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
    private void vela$typeInSearch(KeyEvent event,CallbackInfoReturnable<Boolean> cir){
        if(vela$search==null||!vela$search.isFocused()||event.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)return;
        vela$search.keyPressed(event);
        cir.setReturnValue(true);
    }
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void vela$highlight(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial,CallbackInfo ci){
        if(vela$search==null||ContainerSearch.current()==null)return;
        String query=vela$search.getValue();
        if(query.isBlank())return;
        int accent=VelaTheme.PRIMARY|0xFF000000;
        for(var slot:menu.slots){
            if(!slot.isActive())continue;
            int x=leftPos+slot.x,y=topPos+slot.y;
            if(ContainerSearch.matches(slot.getItem(),query)){
                g.fill(x-1,y-1,x+17,y,accent);g.fill(x-1,y+16,x+17,y+17,accent);
                g.fill(x-1,y,x,y+16,accent);g.fill(x+16,y,x+17,y+16,accent);
            }else g.fill(x,y,x+16,y+16,0xB80B0D12);
        }
    }
}
