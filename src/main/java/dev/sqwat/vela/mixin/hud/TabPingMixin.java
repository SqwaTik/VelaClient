package dev.sqwat.vela.mixin.hud;
import dev.sqwat.vela.module.visual.TabPing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Tab Ping: the number replaces the bars, right-aligned in the same slot and scaled so three digits fit. */
@Mixin(PlayerTabOverlay.class)
abstract class TabPingMixin {
    @Inject(method="extractPingIcon",at=@At("HEAD"),cancellable=true)
    private void vela$ping(GuiGraphicsExtractor g,int width,int x,int y,PlayerInfo info,CallbackInfo ci){
        if(!TabPing.active())return;
        ci.cancel();
        int ping=info.getLatency();
        String text=ping<=0?"—":String.valueOf(Math.min(ping,9999));
        var font=Minecraft.getInstance().font;
        float scale=.75f;
        g.pose().pushMatrix();
        g.pose().translate(x+width-1-font.width(text)*scale,y+1.5f);g.pose().scale(scale);
        g.text(font,text,0,0,TabPing.color(ping),true);
        g.pose().popMatrix();
    }
}
