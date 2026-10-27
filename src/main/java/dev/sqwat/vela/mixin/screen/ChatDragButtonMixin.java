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
    @org.spongepowered.asm.mixin.Unique private final dev.sqwat.vela.ui.animation.FiniteMotion vela$motion=new dev.sqwat.vela.ui.animation.FiniteMotion();
    @org.spongepowered.asm.mixin.Unique private static int vela$labelWidth(){return Math.round(VelaDraw.textWidth("Dragmode",VelaDraw.Weight.MEDIUM,8))+4;}
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void vela$drawDragButton(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        var view=dev.sqwat.vela.ui.render.UiViewport.fit(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight(),client.getWindow().getGuiScale(),VelaClient.services().configs().current().uiScale);
        var anchor=dev.sqwat.vela.ui.render.ChatDragGeometry.place(view,input.getX(),input.getY(),input.getWidth());int x=anchor.x(),y=anchor.y();
        double localX=view.local(mx),localY=view.local(my);
        boolean hovered=localX>=x-vela$labelWidth()*vela$hover&&localX<x+VELA_SIZE&&localY>=y&&localY<y+VELA_SIZE;
        g.pose().pushMatrix();g.pose().scale(view.scale());
        var config=VelaClient.services().configs().current();
        vela$hover=vela$motion.update(hovered?1:0,vela$clock.step(),config.reducedMotion||!config.animations?0:.2f);
        int extension=Math.round(vela$labelWidth()*vela$hover);
        VelaDraw.roundedRect(g,x-extension,y,VELA_SIZE+extension,VELA_SIZE,5,VelaTheme.mix(VelaTheme.WINDOW,VelaTheme.SURFACE_HOVER,vela$hover));
        if(vela$hover>.01f){dev.sqwat.vela.ui.render.UiClip.enable(g,x-extension,y,x,y+VELA_SIZE);
            VelaDraw.text(g,"Dragmode",x-vela$labelWidth()+4,y+(VELA_SIZE-8)/2,(VelaTheme.TEXT_SECONDARY&0xFFFFFF)|(Math.round(255*vela$hover)<<24),VelaDraw.Weight.MEDIUM,8);g.disableScissor();}
        dev.sqwat.vela.ui.render.VelaIcons.draw(g,"move",x+(VELA_SIZE-14)/2,y+(VELA_SIZE-14)/2,14,VelaTheme.TEXT_SECONDARY);
        // Privacy toggle, mirrored at the left above the input.
        dev.sqwat.vela.ui.ChatPrivacy.track(input);
        var privacy=dev.sqwat.vela.ui.ChatPrivacy.place(view,input.getX(),input.getY());
        vela$privacyHover=dev.sqwat.vela.ui.animation.Motion.approach(vela$privacyHover,privacy.contains(localX,localY)?1:0,config.reducedMotion?60:14,1/60f);
        dev.sqwat.vela.ui.ChatPrivacy.draw(g,privacy,vela$privacyHover);
        g.pose().popMatrix();
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void vela$dragButton(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        var view=dev.sqwat.vela.ui.render.UiViewport.fit(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight(),client.getWindow().getGuiScale(),VelaClient.services().configs().current().uiScale);
        var anchor=dev.sqwat.vela.ui.render.ChatDragGeometry.place(view,input.getX(),input.getY(),input.getWidth());int x=anchor.x(),y=anchor.y();
        if(event.button()==0&&dev.sqwat.vela.ui.ChatPrivacy.place(view,input.getX(),input.getY()).contains(view.local(event.x()),view.local(event.y()))){
            dev.sqwat.vela.ui.ChatPrivacy.toggle();cir.setReturnValue(true);return;
        }
        if (event.button() == 0 && view.local(event.x()) >= x-vela$labelWidth()*vela$hover && view.local(event.x()) < x + VELA_SIZE && view.local(event.y()) >= y && view.local(event.y()) < y + VELA_SIZE) {
            client.gui.setScreen(new HudPositionScreen((Screen)(Object)this, VelaClient.services().hudLayout()));
            cir.setReturnValue(true);
        }
    }
}
