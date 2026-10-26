package dev.sqwat.vela.ui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Size-specific PNG glyphs. Layouts are cached; no fonts are rasterized on the render thread. */
final class PngTextRenderer {
    private static final int[] SIZES={8,9,10,11,12,13,14,15,16,18,19,20,22,24,28,32,36,40,48};
    private record Atlas(Identifier texture,PngFontMetrics metrics){}
    private static final Atlas[][] ATLASES=new Atlas[VelaDraw.Weight.values().length][49];
    private static final Map<String,float[]> FLOATING=new LinkedHashMap<>(128,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,float[]> entry){return size()>256;}
    };
    /** World labels keep immutable local glyph geometry; only their pose changes per frame. */
    static void floating(GuiGraphicsExtractor g,String text,float center,float y,int color,float em){
        text=dev.sqwat.vela.ui.Lang.tr(text);
        var mask=dev.sqwat.vela.module.utility.StreamerMode.mask();
        if(mask==null){floatingRaw(g,text,center,y,color,em);return;}
        boolean italic=mask.next(text,0)>=0||mask.containsAlias(text);
        text=mask.plain(text);
        g.pose().pushMatrix();
        try{
            if(italic){g.pose().translate(center,y);g.pose().mul(new org.joml.Matrix3x2f(1,0,-.2f,1,em*.2f,0));center=0;y=0;}
            floatingRaw(g,text,center,y,color,em);
        }finally{g.pose().popMatrix();}
    }
    private static void floatingRaw(GuiGraphicsExtractor g,String text,float center,float y,int color,float em){
        if(text.isEmpty()||(color>>>24)==0)return;
        var atlas=atlas(VelaDraw.Weight.MEDIUM,18);var metrics=atlas.metrics;
        float[] quads=FLOATING.computeIfAbsent(text,key->{
            var lines=key.split("\n",-1);int count=0;
            for(var line:lines)count+=metrics.line(line).glyphs().size();
            float[] result=new float[count*8];int out=0,row=0;
            for(var line:lines){
                var layout=metrics.line(line);
                for(var placed:layout.glyphs()){
                    var glyph=placed.glyph();float x=placed.x()-layout.width()/2+glyph.bearingX();
                    float top=row*27+18*.82f+glyph.bearingY()-metrics.baseline;
                    result[out++]=x;result[out++]=top;result[out++]=x+glyph.width();result[out++]=top+glyph.height();
                    result[out++]=(float)glyph.x()/metrics.width;result[out++]=(float)glyph.y()/metrics.height;
                    result[out++]=(float)(glyph.x()+glyph.width())/metrics.width;result[out++]=(float)(glyph.y()+glyph.height())/metrics.height;
                }row++;
            }return result;
        });
        var texture=net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(atlas.texture);
        var pose=new org.joml.Matrix3x2f(g.pose()).translate(center,y).scale(em/18);
        g.guiRenderState.addGuiElement(new PngTextRenderState(pose,quads,
            net.minecraft.client.gui.render.TextureSetup.singleTexture(texture.getTextureView(),texture.getSampler()),color,g.scissorStack.peek()));
    }
    private PngTextRenderer(){}
    static int closest(float pixels){int best=SIZES[0];for(int size:SIZES)if(Math.abs(size-pixels)<Math.abs(best-pixels))best=size;return best;}
    private static synchronized Atlas atlas(VelaDraw.Weight weight,int size){
        Atlas cached=ATLASES[weight.ordinal()][size];
        if(cached!=null)return cached;
        String key=weight.name().toLowerCase(Locale.ROOT)+"_"+size;
            String base="/assets/velaclient/ui/";
            try{return ATLASES[weight.ordinal()][size]=new Atlas(Identifier.fromNamespaceAndPath("velaclient","textures/gui/font_"+key+".png"),new PngFontMetrics(PngTextRenderer.class.getResourceAsStream(base+"font_"+key+".csv"),PngTextRenderer.class.getResourceAsStream(base+"kerning_"+weight.name().toLowerCase(Locale.ROOT)+".csv")));}
            catch(Exception e){throw new IllegalStateException("Cannot load packaged PNG font "+key,e);}
    }
    static float width(String text,VelaDraw.Weight weight,float em){
        text=dev.sqwat.vela.ui.Lang.tr(text);
        text=dev.sqwat.vela.module.utility.StreamerMode.display(text);
        var metrics=atlas(weight,18).metrics;float maximum=0;
        if(text.indexOf('\n')<0)return metrics.line(text).width()*em/18;
        for(String line:text.split("\n",-1))maximum=Math.max(maximum,metrics.line(line).width()*em/18);return maximum;
    }
    static float centeredY(String text,VelaDraw.Weight weight,float em,float top,float height){
        text=dev.sqwat.vela.ui.Lang.tr(text);
        text=dev.sqwat.vela.module.utility.StreamerMode.display(text);
        return atlas(weight,18).metrics.centeredTop(text,em,top,height);
    }
    static void draw(GuiGraphicsExtractor g,String text,float x,float y,int color,VelaDraw.Weight weight,float em){
        text=dev.sqwat.vela.ui.Lang.tr(text); // interface language: every Vela label passes through here
        var mask=dev.sqwat.vela.module.utility.StreamerMode.mask();
        if(mask==null){drawRaw(g,text,x,y,color,weight,em);return;}
        boolean italic=mask.next(text,0)>=0||mask.containsAlias(text);
        text=mask.plain(text);
        g.pose().pushMatrix();
        try{
            if(italic){g.pose().translate(x,y);g.pose().mul(new org.joml.Matrix3x2f(1,0,-.2f,1,em*.2f,0));x=0;y=0;}
            drawRaw(g,text,x,y,color,weight,em);
        }finally{g.pose().popMatrix();}
    }
    private static void drawRaw(GuiGraphicsExtractor g,String text,float x,float y,int color,VelaDraw.Weight weight,float em){
        if(text.isEmpty()||(color>>>24)==0)return;
        if(text.indexOf('\n')>=0){for(String line:text.split("\n",-1)){draw(g,line,x,y,color,weight,em);y+=em*1.4f;}return;}
        var placed=layout(g,text,x,y,weight,em);
        var texture=net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(placed.texture);
        g.guiRenderState.addGuiElement(new PngTextRenderState(g.pose(),placed.layout.quads,
                net.minecraft.client.gui.render.TextureSetup.singleTexture(texture.getTextureView(),texture.getSampler()),color,g.scissorStack.peek(),placed.layout.bounds));
    }
    /** Cached single-line glyph quads (x0,y0,x1,y1,u0,v0,u1,v1 in pose space) and the atlas they sample. */
    record Placed(Identifier texture,Layout layout){}
    static Placed layout(GuiGraphicsExtractor g,String text,float x,float y,VelaDraw.Weight weight,float em){
        float density=VelaDraw.pixelDensity(g);int size=closest(em*density);var atlas=atlas(weight,size);var metrics=atlas.metrics;
        int gameScale=net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        float offsetX=g.pose().m20()*gameScale,offsetY=g.pose().m21()*gameScale;
        float scale=em/size;
        // Snap baseline to physical pixels; keep horizontal advances fractional for kerning.
        boolean subpixel=VelaDraw.subpixelText();
        float baseline=subpixel?y*density+size*.82f:Math.round(y*density+offsetY+size*.82f)-offsetY;
        var key=new LayoutKey(text,weight,em,x,y,density,offsetX,offsetY,subpixel);
        var services=dev.sqwat.vela.VelaClient.services();
        if(services!=null&&!services.zakoOptimization().textCache())return new Placed(atlas.texture,layout(metrics,text,x,density,offsetX,scale,baseline));
        Layout cached=LAYOUTS.get(key);
        if(cached==null){cached=layout(metrics,text,x,density,offsetX,scale,baseline);LAYOUTS.put(key,cached);}
        return new Placed(atlas.texture,cached);
    }
    /** Every input of the glyph layout. Static HUD labels repeat the same key each frame; moving text simply misses. */
    private record LayoutKey(String text,VelaDraw.Weight weight,float em,float x,float y,float density,float offsetX,float offsetY,boolean subpixel){}
    /** Quads are shared between frames: render states only read them. */
    record Layout(float[] quads,float[] bounds){}
    private static final Map<LayoutKey,Layout> LAYOUTS=new LinkedHashMap<>(256,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<LayoutKey,Layout> entry){return size()>512;}
    };
    private static Layout layout(PngFontMetrics metrics,String text,float x,float density,float offsetX,float scale,float baseline){
        var glyphs=metrics.line(text).glyphs();
        float[] quads=new float[glyphs.size()*8];int out=0;
        for(var placed:glyphs){
            int quarter=Math.round(((x+placed.x()*scale)*density+offsetX)*4);
            var glyph=metrics.phase(placed.glyph(),Math.floorMod(quarter,4));
            float gx=(Math.floorDiv(quarter,4)+glyph.bearingX()-offsetX)/density;
            float gy=(baseline+glyph.bearingY()-metrics.baseline)/density;
            quads[out++]=gx;quads[out++]=gy;
            quads[out++]=gx+glyph.width()/density;quads[out++]=gy+glyph.height()/density;
            quads[out++]=(float)glyph.x()/metrics.width;quads[out++]=(float)glyph.y()/metrics.height;
            quads[out++]=(float)(glyph.x()+glyph.width())/metrics.width;quads[out++]=(float)(glyph.y()+glyph.height())/metrics.height;
        }
        return new Layout(quads,PngTextRenderState.localBounds(quads));
    }
}
