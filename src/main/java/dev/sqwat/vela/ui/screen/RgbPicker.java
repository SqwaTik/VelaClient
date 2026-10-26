package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.module.setting.ColorSetting;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Compact RGB editor using the same saturation/value gradient as marker colors. */
final class RgbPicker {
    private ColorSetting setting;
    private Rect box=new Rect(0,0,136,132),sv=box,hueBar=box;
    private float hue,saturation,value,reveal;
    private int drag=-1;
    boolean open(){return setting!=null;}
    void close(){setting=null;drag=-1;}
    void show(ColorSetting target,Rect anchor,int width,int height){
        setting=target;reveal=0;
        float[] hsv=java.awt.Color.RGBtoHSB(target.value()>>16,(target.value()>>8)&255,target.value()&255,null);
        hue=hsv[0];saturation=hsv[1];value=hsv[2];
        box=new Rect(Math.clamp(anchor.x()+anchor.width()-136,4,Math.max(4,width-140)),Math.clamp(anchor.y()+anchor.height()+4,4,Math.max(4,height-136)),136,132);
        sv=new Rect(box.x()+9,box.y()+9,118,82);hueBar=new Rect(box.x()+9,box.y()+101,118,7);
    }
    void draw(GuiGraphicsExtractor g,float dt){
        if(!open())return;
        reveal=dev.sqwat.vela.ui.animation.Motion.approach(reveal,1,24,dt);
        g.nextStratum();UiClip.enable(g,box.x()-1,box.y()-1,box.x()+137,box.y()+Math.round(133*reveal));
        VelaDraw.roundedOutline(g,box.x(),box.y(),136,132,7,VelaTheme.BORDER,VelaTheme.SURFACE);
        for(int i=0;i<118;i++)g.fillGradient(sv.x()+i,sv.y(),sv.x()+i+1,sv.y()+82,java.awt.Color.HSBtoRGB(hue,i/117f,1),0xFF000000);
        VelaDraw.roundedOutline(g,sv.x()+Math.round(saturation*117)-3,sv.y()+Math.round((1-value)*81)-3,7,7,4,0xFFFFFFFF,0);
        for(int i=0;i<118;i++)g.fill(hueBar.x()+i,hueBar.y(),hueBar.x()+i+1,hueBar.y()+7,java.awt.Color.HSBtoRGB(i/117f,1,1));
        VelaDraw.roundedOutline(g,hueBar.x()+Math.round(hue*117)-2,hueBar.y()-2,5,11,2,0xFFFFFFFF,0);
        VelaDraw.roundedRect(g,box.x()+9,box.y()+116,8,8,4,0xFF000000|setting.value());
        VelaDraw.text(g,String.format("#%06X",setting.value()),box.x()+23,box.y()+117,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
        g.disableScissor();
    }
    boolean click(double x,double y){
        if(!open())return false;
        if(!box.contains(x,y)){close();return false;}
        drag=sv.contains(x,y)?0:new Rect(hueBar.x(),hueBar.y()-4,hueBar.width(),15).contains(x,y)?1:-1;
        return drag(x,y);
    }
    boolean drag(double x,double y){
        if(!open()||drag<0)return false;
        if(drag==0){saturation=(float)Math.clamp((x-sv.x())/117,0,1);value=1-(float)Math.clamp((y-sv.y())/81,0,1);}
        else hue=(float)Math.clamp((x-hueBar.x())/117,0,1);
        setting.set(java.awt.Color.HSBtoRGB(hue,saturation,value)&0xFFFFFF);return true;
    }
    void release(){drag=-1;}
}
