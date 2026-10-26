package dev.sqwat.vela.ui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Immutable PNG atlas and glyph metrics, loaded once; no runtime rasterization. */
public final class VelaBitmapFont {
    private record Glyph(int x,int y,int w,int h,float dx,float dy,float advance) {}
    private final Map<Integer,Glyph> glyphs=new HashMap<>();
    private final Identifier texture;
    private final int width,height,scale,lineHeight;
    public VelaBitmapFont(String name) {
        texture=Identifier.fromNamespaceAndPath("velaclient","textures/gui/font_"+name+".png");
        try(var input=Objects.requireNonNull(getClass().getResourceAsStream("/assets/velaclient/ui/font_"+name+".csv"));
            var reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))) {
            String[] h=reader.readLine().split(","); width=Integer.parseInt(h[0]);height=Integer.parseInt(h[1]);scale=Integer.parseInt(h[2]);lineHeight=Integer.parseInt(h[3])+3;
            String line;while((line=reader.readLine())!=null){String[] v=line.split(",");glyphs.put(Integer.parseInt(v[0]),new Glyph(Integer.parseInt(v[1]),Integer.parseInt(v[2]),Integer.parseInt(v[3]),Integer.parseInt(v[4]),Float.parseFloat(v[5]),Float.parseFloat(v[6]),Float.parseFloat(v[7])));}
        }catch(Exception e){throw new IllegalStateException("Missing Vela PNG font: "+name,e);}
    }
    public float width(String text) {
        float pen=0,max=0;for(int i=0;i<text.length();){int cp=text.codePointAt(i);i+=Character.charCount(cp);
            if(cp=='\n'){max=Math.max(max,pen);pen=0;continue;}
            Glyph g=glyphs.getOrDefault(cp,glyphs.get((int)'?'));if(g!=null)pen+=g.advance;
        }return Math.max(max,pen);
    }
    public void draw(GuiGraphicsExtractor g,String text,float x,float y,int color) {
        g.pose().pushMatrix();g.pose().scale(1f/scale);
        try {float pen=x,line=y;
            for(int i=0;i<text.length();){int cp=text.codePointAt(i);i+=Character.charCount(cp);
                if(cp=='\n'){pen=x;line+=lineHeight;continue;}
                Glyph a=glyphs.getOrDefault(cp,glyphs.get((int)'?'));if(a==null)continue;
                g.blit(RenderPipelines.GUI_TEXTURED,texture,Math.round((pen+a.dx)*scale),Math.round((line+a.dy)*scale),a.x,a.y,a.w,a.h,a.w,a.h,width,height,color);
                pen+=a.advance;
            }
        }finally{g.pose().popMatrix();}
    }
}
