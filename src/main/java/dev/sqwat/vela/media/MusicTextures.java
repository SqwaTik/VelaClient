package dev.sqwat.vela.media;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.Map;

/**
 * GPU side of the Music HUD: the cover (scaled down, rounded, with its average colour as the accent), one icon per
 * player app, and a drawn note glyph for songs without a cover. Client thread only.
 */
public final class MusicTextures {
    public static final int COVER=96;
    public record Icon(Identifier id,int width,int height){}
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("Vela/Music");
    private Icon cover;
    private int coverVersion=-1,serial;
    private int accent=0xFFE8ECF4;
    private final Map<String,Icon> apps=new HashMap<>();
    private final java.util.Set<String> failedApps=new java.util.HashSet<>();
    private Icon note;
    private final Map<MusicBrand,Icon> brands=new java.util.EnumMap<>(MusicBrand.class);
    private final Map<MusicGlyph,Icon> glyphs=new java.util.EnumMap<>(MusicGlyph.class);

    public Icon cover(){return cover;}
    public int accent(){return accent;}
    public Icon app(String app){return apps.get(app);}

    /** Picks up a new cover or app icon from the watcher. */
    public void sync(MediaWatcher watcher,MediaTrack track){
        if(watcher.coverVersion()!=coverVersion){
            coverVersion=watcher.coverVersion();
            release(cover);cover=null;accent=0xFFE8ECF4;
            byte[] png=watcher.cover();
            if(png!=null)try{
                NativeImage source=NativeImage.read(png);NativeImage scaled;
                try{scaled=square(source,COVER);}finally{source.close();}
                accent=accent(scaled);round(scaled,COVER*.2f);
                cover=upload("music/cover_"+(serial++),scaled);
            }catch(Exception error){LOG.debug("Bad cover",error);}
        }
        if(track!=null&&!apps.containsKey(track.app())&&!failedApps.contains(track.app())){
            byte[] png=watcher.appIcon(track.app());
            if(png!=null)try{
                NativeImage image=NativeImage.read(png);
                if(image.getWidth()>64||image.getHeight()>64){NativeImage small;try{small=square(image,64);}finally{image.close();}image=small;}
                round(image,Math.min(image.getWidth(),image.getHeight())/2f);
                apps.put(track.app(),upload("music/app_"+(serial++),image));
            }catch(Exception error){failedApps.add(track.app());LOG.debug("Bad app icon",error);}
        }
    }
    public Icon note(){
        if(note==null)note=upload("music/note",noteGlyph(64));
        return note;
    }
    /** The service's own logo cut to a circle, for the badge; null when it can't be read (the panel then shows the app icon or a letter). */
    public Icon brand(MusicBrand brand){
        if(!brands.containsKey(brand))brands.put(brand,logo(brand,64,.5f,"brand_"));
        return brands.get(brand);
    }
    private static Icon logo(MusicBrand brand,int size,float corner,String prefix){
        try(var in=MusicTextures.class.getResourceAsStream(brand.logo())){
            if(in==null)return null;
            NativeImage source=NativeImage.read(in),scaled;
            try{scaled=square(source,size);}finally{source.close();}
            round(scaled,size*corner);
            return upload("music/"+prefix+brand.name().toLowerCase(java.util.Locale.ROOT),scaled);
        }catch(Exception error){LOG.debug("Bad logo "+brand,error);return null;}
    }
    /** A white control glyph (play, pause, next…), to be tinted when drawn. */
    public Icon glyph(MusicGlyph glyph){
        return glyphs.computeIfAbsent(glyph,g->image("music/glyph_"+g.name().toLowerCase(java.util.Locale.ROOT),g.paint(48),48));
    }
    private static Icon image(String path,int[] pixels,int size){
        var image=new NativeImage(size,size,false);
        for(int y=0;y<size;y++)for(int x=0;x<size;x++)image.setPixel(x,y,pixels[y*size+x]);
        return upload(path,image);
    }
    public void clear(){
        release(cover);cover=null;coverVersion=-1;accent=0xFFE8ECF4;
        apps.values().forEach(MusicTextures::release);apps.clear();failedApps.clear();
    }

    private static Icon upload(String path,NativeImage image){
        var id=Identifier.fromNamespaceAndPath("velaclient",path);
        int w=image.getWidth(),h=image.getHeight();
        try{
            Minecraft.getInstance().getTextureManager().register(id,new DynamicTexture(()->"Vela music",image){
                // Covers are drawn far smaller than they are stored: smooth them instead of picking pixels.
                {sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);}
            });
        }catch(RuntimeException error){image.close();throw error;}
        return new Icon(id,w,h);
    }
    private static void release(Icon icon){if(icon!=null)Minecraft.getInstance().getTextureManager().release(icon.id);}

    /** Centre square of {@code source}, box-filtered to {@code size}×{@code size}. */
    static NativeImage square(NativeImage source,int size){
        int side=Math.min(source.getWidth(),source.getHeight());
        int ox=(source.getWidth()-side)/2,oy=(source.getHeight()-side)/2;
        var out=new NativeImage(size,size,false);
        double step=side/(double)size;
        for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            int x0=ox+(int)(x*step),x1=Math.max(x0+1,ox+(int)((x+1)*step)),y0=oy+(int)(y*step),y1=Math.max(y0+1,oy+(int)((y+1)*step));
            long a=0,r=0,g=0,b=0;int n=0;
            for(int sy=y0;sy<y1;sy++)for(int sx=x0;sx<x1;sx++){
                int c=source.getPixel(sx,sy);int ca=c>>>24;
                a+=ca;r+=(c>>16&255)*ca;g+=(c>>8&255)*ca;b+=(c&255)*ca;n++;
            }
            int alpha=(int)(a/n);
            out.setPixel(x,y,a==0?0:alpha<<24|(int)(r/a)<<16|(int)(g/a)<<8|(int)(b/a));
        }
        return out;
    }
    /** Clears the corners outside a rounded rectangle, anti-aliased over one pixel. */
    static void round(NativeImage image,float radius){
        int w=image.getWidth(),h=image.getHeight();
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            float px=x+.5f,py=y+.5f;
            float dx=Math.max(0,Math.max(radius-px,px-(w-radius))),dy=Math.max(0,Math.max(radius-py,py-(h-radius)));
            float d=(float)Math.sqrt(dx*dx+dy*dy)-radius;
            if(d<=-.5f)continue;
            float keep=Math.clamp(.5f-d,0,1);int c=image.getPixel(x,y);
            image.setPixel(x,y,Math.round((c>>>24)*keep)<<24|c&0xFFFFFF);
        }
    }
    /** Average colour of the cover, pushed towards a bright, saturated tone that reads on the dark panel. */
    static int accent(NativeImage image){
        double r=0,g=0,b=0,weight=0;
        for(int y=0;y<image.getHeight();y+=2)for(int x=0;x<image.getWidth();x+=2){
            int c=image.getPixel(x,y);
            float cr=(c>>16&255)/255f,cg=(c>>8&255)/255f,cb=(c&255)/255f;
            float max=Math.max(cr,Math.max(cg,cb)),min=Math.min(cr,Math.min(cg,cb));
            // Colourful pixels count more than grey ones, so a cover with a red logo on black gives red.
            double w=.08+(max-min)*(max-min)*4;
            r+=cr*w;g+=cg*w;b+=cb*w;weight+=w;
        }
        if(weight==0)return 0xFFE8ECF4;
        float[] hsb=java.awt.Color.RGBtoHSB((int)(r/weight*255),(int)(g/weight*255),(int)(b/weight*255),null);
        float saturation=hsb[1]<.12f?hsb[1]:Math.clamp(hsb[1]*1.25f,.35f,.8f);
        float brightness=Math.clamp(Math.max(hsb[2],.82f),0,.97f);
        return 0xFF000000|java.awt.Color.HSBtoRGB(hsb[0],saturation,brightness)&0xFFFFFF;
    }
    /** A white beamed eighth-note pair, anti-aliased by 4×4 supersampling. */
    static NativeImage noteGlyph(int size){
        var image=new NativeImage(size,size,false);
        for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            int hits=0;
            for(int sy=0;sy<4;sy++)for(int sx=0;sx<4;sx++)if(inNote((x+(sx+.5f)/4)/size,(y+(sy+.5f)/4)/size))hits++;
            image.setPixel(x,y,Math.round(hits/16f*255)<<24|0xFFFFFF);
        }
        return image;
    }
    static boolean inNote(float x,float y){
        return head(x,y,.30f,.76f)||head(x,y,.72f,.68f)
            ||(x>=.355f&&x<=.415f&&y>=.24f&&y<=.76f)||(x>=.775f&&x<=.835f&&y>=.16f&&y<=.68f)
            ||(x>=.355f&&x<=.835f&&y>=lerp(.20f,.12f,(x-.355f)/.48f)&&y<=lerp(.32f,.24f,(x-.355f)/.48f));
    }
    private static boolean head(float x,float y,float cx,float cy){
        // A tilted ellipse, like a printed note head.
        float dx=x-cx,dy=y-cy,c=.94f,s=-.34f;
        float u=dx*c-dy*s,v=dx*s+dy*c;
        return u*u/(.125f*.125f)+v*v/(.085f*.085f)<=1;
    }
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
}
