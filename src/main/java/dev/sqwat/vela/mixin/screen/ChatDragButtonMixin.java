package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.ui.screen.HudPositionScreen;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ChatScreen.class)
public abstract class ChatDragButtonMixin {
    @org.spongepowered.asm.mixin.Unique private static final int VELA_SIZE=dev.sqwat.vela.ui.render.ChatDragGeometry.SIZE;
    @org.spongepowered.asm.mixin.Shadow protected net.minecraft.client.gui.components.EditBox input;
    @org.spongepowered.asm.mixin.Unique private final dev.sqwat.vela.ui.animation.FrameClock vela$clock=new dev.sqwat.vela.ui.animation.FrameClock();
    @org.spongepowered.asm.mixin.Unique private float vela$hover,vela$privacyHover;
    @org.spongepowered.asm.mixin.Unique private final dev.sqwat.vela.ui.animation.FiniteMotion vela$motion=new dev.sqwat.vela.ui.animation.FiniteMotion(),vela$privacyMotion=new dev.sqwat.vela.ui.animation.FiniteMotion();
    @org.spongepowered.asm.mixin.Unique private static int vela$labelWidth(){return Math.round(VelaDraw.textWidth("Dragmode",VelaDraw.Weight.MEDIUM,8))+4;}
    @org.spongepowered.asm.mixin.Unique private static int vela$privacyWidth(){return Math.round(VelaDraw.textWidth(dev.sqwat.vela.ui.ChatPrivacy.label(),VelaDraw.Weight.MEDIUM,8))+4;}
    @org.spongepowered.asm.mixin.Unique private static dev.sqwat.vela.ui.render.UiViewport vela$view(){
        Minecraft client=Minecraft.getInstance();
        return dev.sqwat.vela.ui.render.UiViewport.fit(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight(),client.getWindow().getGuiScale(),VelaClient.services().configs().current().uiScale);
    }
    /** Dragmode at the right above the input and the privacy toggle just left of it; each opens by its own hover. */
    @org.spongepowered.asm.mixin.Unique private dev.sqwat.vela.ui.render.Rect[] vela$buttons(dev.sqwat.vela.ui.render.UiViewport view,float dragReveal,float privacyReveal){
        var anchor=dev.sqwat.vela.ui.render.ChatDragGeometry.place(view,input.getX(),input.getY(),input.getWidth());
        int extension=Math.round(vela$labelWidth()*dragReveal);
        var drag=new dev.sqwat.vela.ui.render.Rect(anchor.x()-extension,anchor.y(),VELA_SIZE+extension,VELA_SIZE);
        return new dev.sqwat.vela.ui.render.Rect[]{drag,dev.sqwat.vela.ui.ChatPrivacy.place(drag,Math.round(vela$privacyWidth()*privacyReveal))};
    }
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void vela$drawDragButton(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
        var view=vela$view();
        double localX=view.local(mx),localY=view.local(my);
        var before=vela$buttons(view,vela$hover,vela$privacyHover);
        boolean hovered=before[0].contains(localX,localY),privacyHovered=!hovered&&before[1].contains(localX,localY);
        g.pose().pushMatrix();g.pose().scale(view.scale());
        var config=VelaClient.services().configs().current();
        float dt=vela$clock.step(),duration=config.reducedMotion||!config.animations?0:.2f;
        vela$hover=vela$motion.update(hovered?1:0,dt,duration);
        vela$privacyHover=vela$privacyMotion.update(privacyHovered?1:0,dt,duration);
        var buttons=vela$buttons(view,vela$hover,vela$privacyHover);
        var drag=buttons[0];int x=drag.x()+drag.width()-VELA_SIZE,y=drag.y();
        VelaDraw.roundedRect(g,drag.x(),y,drag.width(),VELA_SIZE,5,VelaTheme.mix(VelaTheme.WINDOW,VelaTheme.SURFACE_HOVER,vela$hover));
        if(vela$hover>.01f){dev.sqwat.vela.ui.render.UiClip.enable(g,drag.x(),y,x,y+VELA_SIZE);
            VelaDraw.text(g,"Dragmode",x-vela$labelWidth()+4,y+(VELA_SIZE-8)/2,(VelaTheme.TEXT_SECONDARY&0xFFFFFF)|(Math.round(255*vela$hover)<<24),VelaDraw.Weight.MEDIUM,8);g.disableScissor();}
        dev.sqwat.vela.ui.render.VelaIcons.draw(g,"move",x+(VELA_SIZE-14)/2,y+(VELA_SIZE-14)/2,14,VelaTheme.TEXT_SECONDARY);
        dev.sqwat.vela.ui.ChatPrivacy.track(input);
        dev.sqwat.vela.ui.ChatPrivacy.draw(g,buttons[1],vela$privacyHover,vela$privacyWidth());
        g.pose().popMatrix();
        VelaClient.services().hudLayout().musicMouse(mx,my,0,false);
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void vela$dragButton(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if(event.button()!=0)return;
        var view=vela$view();
        var buttons=vela$buttons(view,vela$hover,vela$privacyHover);
        double lx=view.local(event.x()),ly=view.local(event.y());
        if(buttons[1].contains(lx,ly)){dev.sqwat.vela.ui.ChatPrivacy.toggle();cir.setReturnValue(true);return;}
        if(buttons[0].contains(lx,ly)){
            Minecraft.getInstance().gui.setScreen(new HudPositionScreen((Screen)(Object)this, VelaClient.services().hudLayout()));
            cir.setReturnValue(true);return;
        }
        // Play controls of the Music panel.
        if(VelaClient.services().hudLayout().musicMouse(event.x(),event.y(),0,true))cir.setReturnValue(true);
    }
    /** The wheel over the Music panel's lyrics steps through the lines instead of scrolling the chat. */
    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void vela$musicScroll(double x,double y,double horizontal,double vertical,CallbackInfoReturnable<Boolean> cir){
        if(VelaClient.services().hudLayout().musicMouse(x,y,vertical,false))cir.setReturnValue(true);
    }
}
