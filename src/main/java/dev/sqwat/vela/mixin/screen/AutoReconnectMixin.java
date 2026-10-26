package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.module.utility.AutoReconnect;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Adds the reconnect button (and countdown) under the vanilla disconnect layout. */
@Mixin(DisconnectedScreen.class)
abstract class AutoReconnectMixin extends Screen {
    @Shadow @Final private Screen parent;
    @Shadow @Final private LinearLayout layout;
    @Unique private Button vela$reconnect;
    @Unique private long vela$deadline=-1;
    private AutoReconnectMixin(Component title){super(title);}
    @Inject(method="init",at=@At("TAIL"))
    private void vela$addButton(CallbackInfo ci){
        var module=AutoReconnect.current();if(module==null)return;
        if(vela$deadline<0){long wait=module.countdownNanos();vela$deadline=wait<0?-1:System.nanoTime()+wait;}
        vela$reconnect=Button.builder(vela$label(module),b->module.reconnect(parent)).bounds(width/2-100,0,200,20).build();
        addRenderableWidget(vela$reconnect);
        vela$place();
    }
    @Inject(method="repositionElements",at=@At("TAIL"))
    private void vela$reposition(CallbackInfo ci){vela$place();}
    @Unique private void vela$place(){if(vela$reconnect!=null)vela$reconnect.setPosition(width/2-100,layout.getY()+layout.getHeight()+6);}
    @Unique private Component vela$label(AutoReconnect module){
        if(vela$deadline<0)return Component.literal(dev.sqwat.vela.ui.Lang.tr("Подключиться к ")+module.serverIp());
        long left=Math.max(0,(vela$deadline-System.nanoTime()+999_999_999L)/1_000_000_000L);
        return Component.literal(dev.sqwat.vela.ui.Lang.tr("Подключение к ")+module.serverIp()+dev.sqwat.vela.ui.Lang.tr(" через ")+left+dev.sqwat.vela.ui.Lang.tr(" с"));
    }
    @Override public void tick(){
        super.tick();
        var module=AutoReconnect.current();if(module==null||vela$reconnect==null)return;
        vela$reconnect.setMessage(vela$label(module));
        if(vela$deadline>=0&&System.nanoTime()>=vela$deadline){vela$deadline=-1;module.reconnect(parent);}
    }
}
