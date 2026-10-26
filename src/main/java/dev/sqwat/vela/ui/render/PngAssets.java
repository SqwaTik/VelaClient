package dev.sqwat.vela.ui.render;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Cached identifiers for prefiltered images; choose a raster close to physical output size. */
final class PngAssets {
    private static final int[] ICON_SIZES=java.util.stream.IntStream.rangeClosed(8,64).toArray(),LOGO_SIZES={24,32,48,64,96,128,256,512,1024};
    private static final Map<String,Identifier[]> IDS=new HashMap<>();
    static void icon(GuiGraphicsExtractor g,String key,float x,float y,float size,int color){draw(g,key,x,y,size,color,ICON_SIZES);}
    static void logo(GuiGraphicsExtractor g,float x,float y,float size,int color){draw(g,"logo",x,y,size,color,LOGO_SIZES);}
    private static void draw(GuiGraphicsExtractor g,String key,float x,float y,float size,int color,int[] sizes){
        float density=VelaDraw.pixelDensity(g);
        float physical=size*density;
        if(sizes==ICON_SIZES&&Math.abs(g.pose().m01())<.00001f&&Math.abs(g.pose().m10())<.00001f){
            int gameScale=net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
            float ox=g.pose().m20()*gameScale,oy=g.pose().m21()*gameScale;
            physical=Math.max(1,Math.round(physical));
            x=(Math.round(x*density+ox)-ox)/density;
            y=(Math.round(y*density+oy)-oy)/density;
            size=physical/density;
        }
        int selected=sizes[sizes.length-1];
        for(int candidate:sizes)if(candidate>=physical-.1f){selected=candidate;break;}
        Identifier[] textures=IDS.computeIfAbsent(key,ignored->new Identifier[sizes[sizes.length-1]+1]);
        Identifier id=textures[selected];
        if(id==null)textures[selected]=id=Identifier.fromNamespaceAndPath("velaclient","textures/gui/"+(sizes==ICON_SIZES?"icon_":"")+key+"_"+selected+".png");
        VelaDraw.blit(g,id,x,y,size,size,0,0,selected,selected,selected,selected,color);
    }
}
