package dev.sqwat.vela.ui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Size-specific glyph atlases baked from the Inter TTFs (see {@link PngFontBaker}) and cached on disk. Layouts are cached. */
final class PngTextRenderer {
    private static final int[] SIZES={8,9,10,11,12,13,14,15,16,18,19,20,22,24,28,32,36,40,48};
    private static final class Atlas{
        private final Identifier id;final PngFontMetrics metrics;private PngFontBaker.Baked pending;
        Atlas(Identifier id,PngFontMetrics metrics,PngFontBaker.Baked pending){this.id=id;this.metrics=metrics;this.pending=pending;}
        /** Uploads the mask on first use (render thread) and frees the CPU copy. */
        Identifier texture(){
            if(pending!=null){
                var baked=pending;pending=null;
                net.minecraft.client.Minecraft.getInstance().getTextureManager().register(id,new AtlasTexture(id.toString(),baked));
            }return id;
        }
    }
    private static final class AtlasTexture extends net.minecraft.client.renderer.texture.AbstractTexture{
        AtlasTexture(String label,PngFontBaker.Baked baked){
            var device=com.mojang.blaze3d.systems.RenderSystem.getDevice();
            texture=device.createTexture(()->label,5,com.mojang.blaze3d.GpuFormat.RGBA8_UNORM,baked.width(),baked.height(),1,1);
            textureView=device.createTextureView(texture);
            sampler=com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getSampler(com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE,
                com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE,com.mojang.blaze3d.textures.FilterMode.LINEAR,com.mojang.blaze3d.textures.FilterMode.LINEAR,false);
            try(var image=new com.mojang.blaze3d.platform.NativeImage(baked.width(),baked.height(),false)){
                byte[] alpha=baked.alpha();int w=baked.width();
                for(int y=0;y<baked.height();y++)for(int x=0;x<w;x++)image.setPixelABGR(x,y,(alpha[y*w+x]&255)<<24|0xFFFFFF);
                device.createCommandEncoder().writeToTexture(texture,image);
            }
        }
    }
    private static final String[] WEIGHTS={"medium","semibold","regular"};
    static java.nio.file.Path cacheDir(){return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("velaclient/cache/fonts-v"+PngFontBaker.VERSION);}
    /** Bakes every atlas missing from the cache in the background, most used sizes first. */
    static void prewarm(){
        int[] order={18,13,12,14,11,16,10,15,20,24,9,8,19,22,28,32,36,40,48};
        PngFontBaker.prewarm(cacheDir(),WEIGHTS,order);
    }
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
        var texture=net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(atlas.texture());
        var pose=new org.joml.Matrix3x2f(g.pose()).translate(center,y).scale(em/18);
        g.guiRenderState.addGuiElement(new PngTextRenderState(pose,quads,
            net.minecraft.client.gui.render.TextureSetup.singleTexture(texture.getTextureView(),texture.getSampler()),color,g.scissorStack.peek()));
    }
    private PngTextRenderer(){}
    static int closest(float pixels){int best=SIZES[0];for(int size:SIZES)if(Math.abs(size-pixels)<Math.abs(best-pixels))best=size;return best;}
    private static synchronized Atlas atlas(VelaDraw.Weight weight,int size){
        Atlas cached=ATLASES[weight.ordinal()][size];
        if(cached!=null)return cached;
        String name=weight.name().toLowerCase(Locale.ROOT);
        try{
            var baked=PngFontBaker.cached(cacheDir(),name,size);
            return ATLASES[weight.ordinal()][size]=new Atlas(Identifier.fromNamespaceAndPath("velaclient","font_atlas/"+name+"_"+size),baked.metrics(name),baked);
        }catch(Exception e){throw new IllegalStateException("Cannot bake font atlas "+name+"_"+size,e);}
    }
    static float width(String text,VelaDraw.Weight weight,float em){
        text=dev.sqwat.vela.ui.Lang.tr(text);
        text=dev.sqwat.vela.module.utility.StreamerMode.display(text);
        var metrics=atlas(weight,18).metrics;float maximum=0;
        if(text.indexOf('\n')<0)return lineWidth(metrics,text,em);
        for(String line:text.split("\n",-1))maximum=Math.max(maximum,lineWidth(metrics,line,em));return maximum;
    }
    private static float lineWidth(PngFontMetrics metrics,String line,float em){
        if(!foreign(metrics,line)||vanillaFont()==null)return metrics.line(line).width()*em/18;
        float width=0;
        for(int start=0;start<line.length();){int end=runEnd(metrics,line,start);String run=line.substring(start,end);
            width+=baked(metrics,line.codePointAt(start))?metrics.line(run).width()*em/18:vanillaFont().width(run)*em/VANILLA_EM;start=end;}
        return width;
    }
    /** Code points the PNG atlases don't have (Japanese, Chinese, Korean, emoji…) are drawn with Minecraft's own font,
     *  which falls back to GNU Unifont, instead of as '?'. */
    private static boolean baked(PngFontMetrics metrics,int cp){return cp<0x20||metrics.contains(cp)||cp==0x2212||cp==0x2012||cp==0x2010||cp==0x2011;}
    private static boolean foreign(PngFontMetrics metrics,String text){
        for(int i=0;i<text.length();){int cp=text.codePointAt(i);if(!baked(metrics,cp))return true;i+=Character.charCount(cp);}return false;
    }
    /** End of the run starting at start whose code points are all baked or all not. */
    private static int runEnd(PngFontMetrics metrics,String text,int start){
        boolean kind=baked(metrics,text.codePointAt(start));int i=start;
        while(i<text.length()){int cp=text.codePointAt(i);if(baked(metrics,cp)!=kind)break;i+=Character.charCount(cp);}
        return i;
    }
    /** Minecraft's 9-unit line scaled so its 7-unit capitals match Inter's cap height. */
    private static final float VANILLA_EM=9.5f;
    private static net.minecraft.client.gui.Font vanillaFont(){var mc=net.minecraft.client.Minecraft.getInstance();return mc==null?null:mc.font;}
    /** Width of one line drawn as is: no interface translation, no streamer-mode masking (source code). */
    static float literalWidth(String line,VelaDraw.Weight weight,float em){return atlas(weight,18).metrics.line(line).width()*em/18;}
    /** x of every UTF-16 offset of one literal line (length + 1 entries), kerning included: caret, selection, clicks. */
    static float[] literalOffsets(String line,VelaDraw.Weight weight,float em){
        var placed=atlas(weight,18).metrics.line(line);float k=em/18;float[] out=new float[line.length()+1];int i=0;
        for(var glyph:placed.glyphs()){out[i]=glyph.x()*k;int n=Character.charCount(line.codePointAt(i));if(n==2)out[i+1]=out[i];i+=n;}
        out[line.length()]=placed.width()*k;return out;
    }
    /** Draws one line as is, see {@link #literalWidth}. */
    static void drawLiteral(GuiGraphicsExtractor g,String line,float x,float y,int color,VelaDraw.Weight weight,float em){drawRaw(g,line,x,y,color,weight,em);}
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
        var coverage=atlas(weight,18).metrics;
        if(foreign(coverage,text)&&vanillaFont()!=null){
            for(int start=0;start<text.length();){int end=runEnd(coverage,text,start);String run=text.substring(start,end);
                if(baked(coverage,text.codePointAt(start))){drawRaw(g,run,x,y,color,weight,em);x+=coverage.line(run).width()*em/18;}
                else{
                    float scale=em/VANILLA_EM;g.pose().pushMatrix();
                    try{g.pose().translate(x,y+em*.82f-7*scale);g.pose().scale(scale);g.text(vanillaFont(),run,0,0,color,false);}
                    finally{g.pose().popMatrix();}
                    x+=vanillaFont().width(run)*scale;
                }start=end;
            }return;
        }
        // The layout is made for y's position within one physical pixel and moved by whole pixels with the pose: text
        // that scrolls or slides keeps hitting the cache instead of being laid out again for every new y.
        float density=VelaDraw.pixelDensity(g);
        float shift=density>0?(float)Math.floor(y*density)/density:0;
        var placed=layout(g,text,x,y-shift,weight,em);
        var texture=net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(placed.texture);
        g.pose().pushMatrix();
        try{
            if(shift!=0)g.pose().translate(0,shift);
            g.guiRenderState.addGuiElement(new PngTextRenderState(g.pose(),placed.layout.quads,
                    net.minecraft.client.gui.render.TextureSetup.singleTexture(texture.getTextureView(),texture.getSampler()),color,g.scissorStack.peek(),placed.layout.bounds));
        }finally{g.pose().popMatrix();}
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
        if(services!=null&&!services.optimization().textCache())return new Placed(atlas.texture(),layout(metrics,text,x,density,offsetX,scale,baseline));
        Layout cached=LAYOUTS.get(key);
        if(cached==null){cached=layout(metrics,text,x,density,offsetX,scale,baseline);LAYOUTS.put(key,cached);}
        return new Placed(atlas.texture(),cached);
    }
    /** Every input of the glyph layout. Static HUD labels repeat the same key each frame; moving text simply misses. */
    private record LayoutKey(String text,VelaDraw.Weight weight,float em,float x,float y,float density,float offsetX,float offsetY,boolean subpixel){}
    /** Quads are shared between frames: render states only read them. */
    record Layout(float[] quads,float[] bounds){}
    private static final Map<LayoutKey,Layout> LAYOUTS=new LinkedHashMap<>(256,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<LayoutKey,Layout> entry){return size()>2048;}
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
