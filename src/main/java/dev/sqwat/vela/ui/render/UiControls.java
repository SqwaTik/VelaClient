package dev.sqwat.vela.ui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.sqwat.vela.ui.animation.Motion;
import java.util.HashMap;
import java.util.Map;

/** Reusable controls: stable identity owns hover, selection and value interpolation. */
public final class UiControls {
    public static void drawAction(GuiGraphicsExtractor g,Rect rect,String label,float h){
int fill=VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.readableAccentFill(),h);
        float bx=rect.x(),bw=rect.width()+dev.sqwat.vela.ui.render.ActionGeometry.growth(h);
        var canvas=new dev.sqwat.vela.ui.render.GameUiCanvas(g);
        // Low-opacity PNG masks form a soft halo without a per-frame blur pass.
        for(int spread=6;spread>=1;spread--){
            int alpha=Math.round(h*(7-spread)*1.4f);
            if(alpha>0)canvas.round(bx-spread,rect.y()-spread,bw+spread*2,rect.height()+spread*2,8+spread,(VelaTheme.PRIMARY&0xFFFFFF)|(alpha<<24));
        }
        canvas.round(bx,rect.y(),bw,rect.height(),8,fill);
        canvas.border(bx,rect.y(),bw,rect.height(),8,VelaTheme.mix(VelaTheme.BORDER,VelaTheme.PRIMARY,h));
        float shift=dev.sqwat.vela.ui.render.ActionGeometry.textShift(h);
        VelaDraw.centeredText(g,label,rect.x()+rect.width()/2f+shift,rect.y()+(rect.height()-VelaDraw.BODY)/2f,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,VelaDraw.BODY);
        float labelLeft=rect.x()+(rect.width()-VelaDraw.textWidth(label,VelaDraw.Weight.MEDIUM,VelaDraw.BODY))/2;
        if(h>.02f){int tint=(VelaTheme.TEXT&0xFFFFFF)|(Math.round(255*h)<<24);VelaDraw.text(g,">",labelLeft+shift-10,rect.y()+(rect.height()-9)/2,tint,VelaDraw.Weight.MEDIUM,8);}
    }
    public void iconButton(GuiGraphicsExtractor g,String id,Rect rect,String icon,boolean open,boolean hovered){
        float h=animate(id+"hover",hovered?1:0);boolean close=icon.equals("x");
        VelaDraw.roundedRect(g,rect.x(),rect.y(),rect.width(),rect.height(),6,VelaTheme.mix(close?0x00141923:open?VelaTheme.PRIMARY_SOFT:VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,h));
        if(h>.02f)new GameUiCanvas(g).border(rect.x(),rect.y(),rect.width(),rect.height(),6,UiKit.hoverBorder(h));
        g.pose().pushMatrix();g.pose().translate(rect.x()+rect.width()/2f,rect.y()+rect.height()/2f);
        if(!close)g.pose().scale(1+h*.04f);
        if(icon.equals("chevron"))g.pose().rotate(ControlChrome.disclosureAngle(animate(id+"rotation",open?1:0)));
        if(icon.equals("settings"))g.pose().rotate((float)(Math.PI*animate(id+"gear",hovered?1:0,7)));
        VelaIcons.draw(g,icon,-6,-6,12,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,close?VelaTheme.TEXT:VelaTheme.PRIMARY,h));g.pose().popMatrix();
    }
    private final dev.sqwat.vela.ui.animation.AnimationState animations=new dev.sqwat.vela.ui.animation.AnimationState();
    public float animate(String id,float target){return animate(id,target,24);}
    public float animate(String id,float target,float speed){var services=dev.sqwat.vela.VelaClient.services();boolean immediate=services!=null&&(!services.configs().current().animations||services.configs().current().reducedMotion);return animations.value(id,target,immediate,speed);}
    public void button(GuiGraphicsExtractor g,String id,Rect r,String label,boolean hovered){
        float h=animate(id+"hover",hovered?1:0);
        boolean action=label.equals("Добавить")||label.equals("Создать")||label.startsWith("Назначить");
        VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),6,VelaTheme.mix(VelaTheme.SURFACE_DEEP,action?VelaTheme.readableAccentFill():VelaTheme.SURFACE_HOVER,h));
        new GameUiCanvas(g).border(r.x(),r.y(),r.width(),r.height(),6,VelaTheme.mix(VelaTheme.BORDER,action?VelaTheme.PRIMARY:VelaTheme.BORDER_HOVER,h));
        float shift=action?ActionGeometry.textShift(h):0;
        VelaDraw.centeredText(g,label,r.x()+r.width()/2f+shift,r.y()+(r.height()-VelaDraw.BODY)/2f,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,VelaDraw.BODY);
        if(action&&h>.02f)VelaDraw.text(g,">",r.x()+(r.width()-VelaDraw.textWidth(label,VelaDraw.Weight.MEDIUM))/2f+shift-10,r.y()+(r.height()-9)/2f,(Math.round(255*h)<<24)|(VelaTheme.TEXT&0xFFFFFF),VelaDraw.Weight.MEDIUM,8);
    }
    public void checkbox(GuiGraphicsExtractor g,String id,Rect r,boolean checked){ControlChrome.checkbox(new GameUiCanvas(g),r,animate(id+"selected",checked?1:0));}
    public void favorite(GuiGraphicsExtractor g,String id,Rect r,boolean selected,boolean hovered){
        float value=animate(id+"selected",selected?1:0),h=animate(id+"hover",hovered?1:0);
        int tint=VelaTheme.mix(VelaTheme.TEXT_MUTED,VelaTheme.PRIMARY,Math.max(value,h));
        VelaIcons.draw(g,"star",r.x(),r.y(),r.width(),tint);
        if(value>.001f)VelaIcons.draw(g,"star_filled",r.x(),r.y(),r.width(),(Math.round(value*255)<<24)|(tint&0xFFFFFF));
    }
    public void slider(GuiGraphicsExtractor g,String id,Rect r,float value){ControlChrome.slider(new GameUiCanvas(g),r,animate(id+"value",value));}
}
