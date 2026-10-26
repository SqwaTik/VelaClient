package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.module.visual.Animations;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerScreen.class)
abstract class AnimatedContainerMixin {
    @Shadow protected int topPos;
    @Unique private int vela$base;
    @Unique private long vela$opened;
    @Unique private boolean vela$pending,vela$moving;
    @Inject(method="init",at=@At("RETURN"))
    private void vela$open(CallbackInfo ci){vela$pending=true;vela$moving=false;}
    @Inject(method="extractRenderState",at=@At("HEAD"))
    private void vela$move(GuiGraphicsExtractor g,int mx,int my,float partial,CallbackInfo ci){
        // Capture after subclass init has finished; inactive mode never overrides its layout.
        if(vela$pending){vela$pending=false;vela$base=topPos;vela$opened=System.nanoTime();vela$moving=Animations.active("inventory");}
        if(!vela$moving)return;
        if(!Animations.active("inventory")){topPos=vela$base;vela$moving=false;return;}
        long elapsed=System.nanoTime()-vela$opened;
        topPos=vela$base+(int)(18*Animations.remaining(elapsed,220_000_000));
        if(elapsed>=220_000_000)vela$moving=false;
    }
}
