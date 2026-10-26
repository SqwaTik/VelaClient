package dev.sqwat.vela.mixin;

import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.ui.HudLayoutService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the offsets configured by Vela Drag Mode to vanilla HUD groups. */
@Mixin(Hud.class)
public abstract class VanillaHudPositionMixin {
    @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
    private void vela$screenVisibility(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo ci){if(!dev.sqwat.vela.ui.HudVisibility.allowed(net.minecraft.client.Minecraft.getInstance()))ci.cancel();}
    private boolean vela$bossbarPushed;
    private boolean vela$scoreboardPushed;
    private boolean vela$tabPushed;
    private boolean vela$hotbarPushed;

    @Inject(method="extractHotbarAndDecorations",at=@At("HEAD"))
    private void vela$beforeHotbar(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo ci){
        if(VelaClient.services()==null)return;
        var layout=VelaClient.services().hudLayout();
        layout.viewport(net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale());
        var player=net.minecraft.client.Minecraft.getInstance().player;
        int halfWidth=player!=null&&!player.getOffhandItem().isEmpty()?120:92;
        int rows=player==null?1:Math.max(1,(int)Math.ceil((player.getMaxHealth()+player.getAbsorptionAmount())/20f));
        int height=player!=null&&player.getAbilities().instabuild?24:50+(rows-1)*Math.max(3,12-rows);
        int x=g.guiWidth()/2-halfWidth,y=g.guiHeight()-height;
        layout.recordNative("hotbar",new dev.sqwat.vela.ui.render.Rect(x,y,halfWidth*2,height));
        var offset=layout.translation("hotbar",g.guiWidth(),g.guiHeight());
        g.pose().pushMatrix();g.pose().translate(offset.x()+x,offset.y()+y);g.pose().scale(layout.scale("hotbar"));g.pose().translate(-x,-y);
        vela$hotbarPushed=true;
    }
    @Inject(method="extractHotbarAndDecorations",at=@At("RETURN"))
    private void vela$afterHotbar(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo ci){if(vela$hotbarPushed)g.pose().popMatrix();vela$hotbarPushed=false;}

    @Inject(method="extractChat",at=@At("HEAD"),cancellable=true)
    private void vela$chatOnlyOnTab(GuiGraphicsExtractor graphics,DeltaTracker delta,CallbackInfo ci){
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(mc.gui.screen() instanceof dev.sqwat.vela.ui.screen.HudPositionScreen&&org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().handle(),org.lwjgl.glfw.GLFW.GLFW_KEY_TAB)!=org.lwjgl.glfw.GLFW.GLFW_PRESS)ci.cancel();
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(method="extractChat",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V"),index=5)
    private net.minecraft.client.gui.components.ChatComponent.DisplayMode vela$chatPreview(net.minecraft.client.gui.components.ChatComponent.DisplayMode mode){
        return net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof dev.sqwat.vela.ui.screen.HudPositionScreen?net.minecraft.client.gui.components.ChatComponent.DisplayMode.FOREGROUND:mode;
    }

    @Inject(method = "extractBossOverlay", at = @At("HEAD"),cancellable=true)
    private void vela$beforeBossbar(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if(dev.sqwat.vela.module.RenderTweaks.hide("bossbar")){ci.cancel();return;}
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(mc.gui.screen() instanceof dev.sqwat.vela.ui.screen.HudPositionScreen&&mc.options.keyPlayerList.isDown()){ci.cancel();return;}
        vela$bossbarPushed = push(graphics, "bossbar");
    }

    @Inject(method = "extractBossOverlay", at = @At("RETURN"))
    private void vela$afterBossbar(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (vela$bossbarPushed) graphics.pose().popMatrix();
        vela$bossbarPushed = false;
        dev.sqwat.vela.ui.NativeHudCapture.finish();
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"),cancellable=true)
    private void vela$beforeScoreboard(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if(dev.sqwat.vela.module.RenderTweaks.hide("scoreboard")){ci.cancel();return;}
        vela$scoreboardPushed = push(graphics, "scoreboard");
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("RETURN"))
    private void vela$afterScoreboard(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (vela$scoreboardPushed) graphics.pose().popMatrix();
        vela$scoreboardPushed = false;
        dev.sqwat.vela.ui.NativeHudCapture.finish();
    }

    @Inject(method="extractTabList",at=@At("HEAD"))
    private void vela$beforeTab(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo ci){vela$tabPushed=push(g,"tablist");}
    @Inject(method="extractTabList",at=@At("RETURN"))
    private void vela$afterTab(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo ci){
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(mc.gui.screen() instanceof dev.sqwat.vela.ui.screen.HudPositionScreen&&mc.options.keyPlayerList.isDown()&&mc.isLocalServer()&&mc.player.connection.getListedOnlinePlayers().size()<=1){
            var board=mc.level.getScoreboard();var tab=((Hud)(Object)this).getTabList();
            tab.setVisible(true);g.nextStratum();tab.extractRenderState(g,g.guiWidth(),board,board.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.LIST));
        }
        if(vela$tabPushed)g.pose().popMatrix();vela$tabPushed=false;dev.sqwat.vela.ui.NativeHudCapture.finish();
    }

    private static boolean push(GuiGraphicsExtractor graphics, String id) {
        dev.sqwat.vela.ui.NativeHudCapture.begin(id,graphics.guiWidth());
        if (VelaClient.services() == null) return false;
        HudLayoutService layout = VelaClient.services().hudLayout();
        var client = net.minecraft.client.Minecraft.getInstance();
        layout.viewport(client.getWindow().getGuiScale());
        var offset = layout.translation(id, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        float scale=layout.scale(id);
        if (offset.x() == 0 && offset.y() == 0&&scale==1) return false;
        graphics.pose().pushMatrix();
        graphics.pose().translate(offset.x(), offset.y());
        var position=layout.position(id,graphics.guiWidth(),graphics.guiHeight());
        int originX=position.x()-offset.x(),originY=position.y()-offset.y();
        graphics.pose().translate(originX,originY);graphics.pose().scale(scale);graphics.pose().translate(-originX,-originY);
        return true;
    }
}
