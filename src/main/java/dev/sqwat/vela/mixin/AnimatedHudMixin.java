package dev.sqwat.vela.mixin;
import dev.sqwat.vela.module.Animations;
import dev.sqwat.vela.ui.animation.*;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(Hud.class)
abstract class AnimatedHudMixin {
    @Unique private final FiniteMotion vela$tab=new FiniteMotion();
    @Unique private final FrameClock vela$tabClock=new FrameClock();
    @Unique private float vela$tabValue;
    @Unique private Object vela$world;
    @Unique private final FrameClock vela$slotClock=new FrameClock();
    @Unique private float vela$slot;
    @Unique private Object vela$slotPlayer;
    @Unique private long vela$lastSlot;
    @WrapOperation(method="extractItemHotbar",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void vela$slot(GuiGraphicsExtractor g,com.mojang.blaze3d.pipeline.RenderPipeline pipeline,net.minecraft.resources.Identifier sprite,int x,int y,int width,int height,Operation<Void> original){
        var player=net.minecraft.client.Minecraft.getInstance().player;
        if(!sprite.getPath().equals("hud/hotbar_selection")||player==null){original.call(g,pipeline,sprite,x,y,width,height);return;}
        int selected=player.getInventory().getSelectedSlot();long now=System.nanoTime();
        if(!Animations.active("hotbar")||vela$slotPlayer!=player||now-vela$lastSlot>250_000_000){vela$slot=selected;vela$slotClock.reset();}
        vela$slotPlayer=player;vela$lastSlot=now;
        vela$slot=Motion.approach(vela$slot,selected,22,vela$slotClock.step());
        if(Math.abs(vela$slot-selected)<.001f)vela$slot=selected;
        g.pose().pushMatrix();try{g.pose().translate((vela$slot-selected)*20,0);original.call(g,pipeline,sprite,x,y,width,height);}finally{g.pose().popMatrix();}
    }
    @Inject(method="extractTabList",at=@At("HEAD"))
    private void vela$tabTick(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo ci){
        var mc=net.minecraft.client.Minecraft.getInstance();boolean down=mc.options.keyPlayerList.isDown();
        if(vela$world!=mc.level||!Animations.active("tab")){vela$tab.reset(down?1:0);vela$tabClock.reset();vela$world=mc.level;}
        vela$tabValue=vela$tab.update(down?1:0,vela$tabClock.step(),.3f,true);
    }
    @WrapOperation(method="extractTabList",at=@At(value="INVOKE",target="Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean vela$keepClosing(KeyMapping key,Operation<Boolean> original){return original.call(key)||Animations.active("tab")&&vela$tabValue>.001f;}
    @WrapOperation(method="extractTabList",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/PlayerTabOverlay;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V"))
    private void vela$tabDraw(net.minecraft.client.gui.components.PlayerTabOverlay tab,GuiGraphicsExtractor g,int width,net.minecraft.world.scores.Scoreboard board,net.minecraft.world.scores.Objective objective,Operation<Void> original){
        g.pose().pushMatrix();try{
            if(Animations.active("tab")&&!(net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof dev.sqwat.vela.ui.screen.HudPositionScreen)){float scale=Math.max(.01f,Math.min(1.15f,vela$tabValue));g.pose().translate(width/2f,10);g.pose().scale(scale,scale);g.pose().translate(-width/2f,-10);}
            original.call(tab,g,width,board,objective);
        }finally{g.pose().popMatrix();}
    }
}
