package dev.sqwat.vela.mixin;
import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.module.SlotSweep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractContainerScreen.class)
abstract class ItemScrollerMixin {
    @Shadow protected abstract Slot getHoveredSlot(double x,double y);
    @Shadow protected abstract void slotClicked(Slot slot,int index,int button,ContainerInput action);
    @Unique private final SlotSweep vela$sweep=new SlotSweep();
    @Unique private boolean vela$sweeping;
    @Unique private boolean vela$allowed(MouseButtonEvent e){
        var services=VelaClient.services();var c=Minecraft.getInstance();
        return services!=null&&services.modules().byId("item_scroller").enabled()&&e.button()==0&&(e.modifiers()&1)!=0&&c.player!=null&&c.gameMode!=null&&c.isWindowActive()&&c.player.containerMenu.getCarried().isEmpty()&&!((Object)this instanceof CreativeModeInventoryScreen);
    }
    @Unique private boolean vela$move(MouseButtonEvent e){
        var slot=getHoveredSlot(e.x(),e.y());var player=Minecraft.getInstance().player;
        if(slot==null||!slot.isActive()||!slot.hasItem()||!slot.mayPickup(player))return false;
        int delay=VelaClient.services().modules().byId("item_scroller") instanceof dev.sqwat.vela.module.ItemScroller scroller?scroller.delayMs():0;
        if(vela$sweep.visit(slot.index,System.nanoTime(),delay))slotClicked(slot,slot.index,0,ContainerInput.QUICK_MOVE);
        return true;
    }
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void vela$begin(MouseButtonEvent e,boolean doubled,CallbackInfoReturnable<Boolean> cir){
        vela$sweeping=false;vela$sweep.reset();
        if(vela$allowed(e)&&vela$move(e)){vela$sweeping=true;cir.setReturnValue(true);}
    }
    @Inject(method="mouseDragged",at=@At("HEAD"),cancellable=true)
    private void vela$drag(MouseButtonEvent e,double dx,double dy,CallbackInfoReturnable<Boolean> cir){
        if(!vela$sweeping)return;
        if(!vela$allowed(e)){vela$sweeping=false;vela$sweep.reset();cir.setReturnValue(true);return;}
        vela$move(e);cir.setReturnValue(true);
    }
    @Inject(method="mouseReleased",at=@At("HEAD"),cancellable=true)
    private void vela$end(MouseButtonEvent e,CallbackInfoReturnable<Boolean> cir){
        if(vela$sweeping){vela$sweeping=false;vela$sweep.reset();cir.setReturnValue(true);}
    }
}
