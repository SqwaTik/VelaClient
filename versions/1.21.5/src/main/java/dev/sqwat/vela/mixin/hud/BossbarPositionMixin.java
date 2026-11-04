package dev.sqwat.vela.mixin.hud;

import dev.sqwat.vela.VelaClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.5's Gui draws the boss bar from a lambda layer, so its Drag Mode offset is applied in BossHealthOverlay.render. */
@Mixin(BossHealthOverlay.class)
abstract class BossbarPositionMixin {
    @Unique private boolean vela$pushed;

    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void vela$beforeBossbar(GuiGraphics graphics,CallbackInfo ci){
        if(dev.sqwat.vela.module.utility.RenderTweaks.hide("bossbar")){ci.cancel();return;}
        var mc=Minecraft.getInstance();
        if(mc.screen instanceof dev.sqwat.vela.ui.screen.HudPositionScreen&&mc.options.keyPlayerList.isDown()){ci.cancel();return;}
        dev.sqwat.vela.ui.hud.NativeHudCapture.begin("bossbar",graphics.guiWidth());
        vela$pushed=false;
        if(VelaClient.services()==null)return;
        var layout=VelaClient.services().hudLayout();
        layout.viewport(mc.getWindow().calculateScale(mc.options.guiScale().get(),mc.isEnforceUnicode()));
        var offset=layout.translation("bossbar",mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
        float scale=layout.scale("bossbar");
        if(offset.x()==0&&offset.y()==0&&scale==1)return;
        var position=layout.position("bossbar",graphics.guiWidth(),graphics.guiHeight());
        int originX=position.x(),originY=position.y();
        graphics.pose().pushPose();
        graphics.pose().translate(originX,originY,0);graphics.pose().scale(scale,scale,1);graphics.pose().translate(offset.x()-originX,offset.y()-originY,0);
        vela$pushed=true;
    }

    @Inject(method="render",at=@At("RETURN"))
    private void vela$afterBossbar(GuiGraphics graphics,CallbackInfo ci){
        if(vela$pushed)graphics.pose().popPose();
        vela$pushed=false;
        dev.sqwat.vela.ui.hud.NativeHudCapture.finish();
    }
}
