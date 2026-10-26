package dev.sqwat.vela.ui.render;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public record GameUiCanvas(GuiGraphicsExtractor graphics) implements UiCanvas {
    private static final Identifier CHROME=Identifier.fromNamespaceAndPath("velaclient","textures/gui/chrome.png");
    public void fill(float x,float y,float w,float h,int color){VelaDraw.blit(graphics,CHROME,x,y,w,h,6,6,1,1,1024,1024,color);}
    public void round(float x,float y,float w,float h,float radius,int color){PngChrome.draw(graphics,x,y,w,h,radius,color,false);}
    public void border(float x,float y,float w,float h,float radius,int color){PngChrome.draw(graphics,x,y,w,h,radius,color,true);}
    public void image(String name,float x,float y,float size,int color){if(name.equals("logo"))PngAssets.logo(graphics,x,y,size,color);else PngAssets.icon(graphics,name,x,y,size,color);}
    public void texture(String name,Rect r,int sw,int sh,int color){VelaDraw.blit(graphics,Identifier.fromNamespaceAndPath("velaclient","textures/gui/"+name+".png"),r.x(),r.y(),r.width(),r.height(),0,0,sw,sh,sw,sh,color);}
    public void text(String value,float x,float y,float size,int color,Weight weight){VelaDraw.text(graphics,value,x,y,color,VelaDraw.Weight.valueOf(weight.name()),size);}
    public float textWidth(String value,float size,Weight weight){return VelaDraw.textWidth(value,VelaDraw.Weight.valueOf(weight.name()),size);}
    public float density(){return VelaDraw.pixelDensity(graphics);}
}
