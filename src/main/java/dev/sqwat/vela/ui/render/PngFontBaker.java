package dev.sqwat.vela.ui.render;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.zip.*;

/**
 * Bakes the glyph atlases from the bundled Inter TTFs instead of shipping 57 PNGs: 4x supersampled, area filtered
 * white masks with the same metrics format {@link PngFontMetrics} reads. Pure AWT, so tests bake exactly what the game does.
 */
public final class PngFontBaker {
    /** Bump when the output changes; cached atlases from another version are baked again. */
    public static final int VERSION=1;
    static final int SS=4,GAP=2;
    public record Baked(int ppem,int width,int height,byte[] alpha,String csv){
        public PngFontMetrics metrics(String weight)throws IOException{
            return new PngFontMetrics(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),PngFontBaker.class.getResourceAsStream("/assets/velaclient/ui/kerning_"+weight+".csv"));
        }
    }
    private record Raster(int cp,int phase,int width,int height,byte[] alpha,float dx,float dy,float advance){}
    private PngFontBaker(){}

    /** English, Russian (with Ukrainian and Belarusian letters), Latin-1 and the interface symbols. Anything else is drawn
     *  with Minecraft's own font, see PngTextRenderer#baked. */
    public static int[] alphabet(){
        StringBuilder b=new StringBuilder();
        for(int cp=32;cp<=126;cp++)b.appendCodePoint(cp);
        for(int cp=160;cp<=255;cp++)b.appendCodePoint(cp);
        for(int cp=0x400;cp<=0x45F;cp++)b.appendCodePoint(cp);
        b.append("\u0490\u0491\u2026\u2014\u2013\u2018\u2019\u201C\u201D\u201E\u2116\u20AC\u20BD\u00B7\u2022\u2194\u2195\u2191\u2193\u2190\u2192\u2713\u2715\u00D7\u2605\u2606\u25B6\u25C0\u25BC\u25B2\u25CF\u25CB\u00B0"); // Ukrainian G, punctuation, currency and interface symbols
        return b.toString().codePoints().distinct().toArray();
    }
    /** Below 16 px glyphs keep four quarter-pixel phases; above that the offset is invisible. */
    public static boolean phased(int ppem){return ppem<16;}

    private static final Map<String,Font> FONTS=new HashMap<>();
    public static Font font(String weight)throws IOException{
        synchronized(FONTS){var cached=FONTS.get(weight);if(cached!=null)return cached;}
        Font font;
        try(var in=PngFontBaker.class.getResourceAsStream("/assets/velaclient/font/inter_"+weight+".ttf")){
            if(in==null)throw new FileNotFoundException("inter_"+weight+".ttf");
            font=Font.createFont(Font.TRUETYPE_FONT,in);
        }catch(FontFormatException e){throw new IOException(e);}
        synchronized(FONTS){FONTS.putIfAbsent(weight,font);return FONTS.get(weight);}
    }

    /** The atlas from the cache directory, baked and stored there first if it is missing or from another version. */
    public static Baked cached(java.nio.file.Path dir,String weight,int ppem)throws IOException{
        var file=dir.resolve("font_"+weight+"_"+ppem+".bin");
        if(java.nio.file.Files.isRegularFile(file))try(var in=java.nio.file.Files.newInputStream(file)){var baked=read(in);if(baked.ppem==ppem)return baked;}catch(IOException stale){}
        var baked=bake(font(weight),ppem);
        try{
            java.nio.file.Files.createDirectories(dir);
            var temp=java.nio.file.Files.createTempFile(dir,"font",".tmp");
            try(var out=java.nio.file.Files.newOutputStream(temp)){write(baked,out);}
            java.nio.file.Files.move(temp,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        }catch(IOException unwritable){/* baked again next launch */}
        return baked;
    }
    /** Fills the cache on a background thread so that no atlas is baked on the render thread on first use. */
    public static void prewarm(java.nio.file.Path dir,String[] weights,int[] sizes){
        var thread=new Thread(()->{
            for(int size:sizes)for(String weight:weights){
                if(java.nio.file.Files.isRegularFile(dir.resolve("font_"+weight+"_"+size+".bin")))continue;
                try{cached(dir,weight,size);}catch(Throwable ignored){}
            }
        },"Vela font baker");
        thread.setDaemon(true);thread.setPriority(Thread.MIN_PRIORITY);thread.start();
    }

    public static Baked bake(Font source,int ppem){
        Font font=source.deriveFont((float)ppem*SS);var frc=new FontRenderContext(null,true,true);
        int phases=phased(ppem)?4:1;List<Raster> rasters=new ArrayList<>();
        for(int cp:alphabet()){
            if(!font.canDisplay(cp))continue;
            GlyphVector vector=font.createGlyphVector(frc,new String(Character.toChars(cp)));
            float advance=(float)vector.getGlyphPosition(vector.getNumGlyphs()).getX()/SS;
            for(int phase=0;phase<phases;phase++)rasters.add(raster(vector,frc,cp,phase,ppem,advance));
        }
        // Shelf packing, tallest first.
        int width=ppem>=28?2048:1024;
        var order=new ArrayList<>(rasters);order.sort(Comparator.comparingInt(r->-r.height));
        int[] xs=new int[order.size()],ys=new int[order.size()];int x=0,y=0,shelf=0;
        for(int i=0;i<order.size();i++){
            var r=order.get(i);if(x+r.width>width){x=0;y+=shelf+GAP;shelf=0;}
            xs[i]=x;ys[i]=y;x+=r.width+GAP;shelf=Math.max(shelf,r.height);
        }
        int height=y+shelf;byte[] alpha=new byte[width*height];
        StringBuilder csv=new StringBuilder(width+","+height+","+ppem+","+(ppem*.82f)+"\n");
        for(int i=0;i<order.size();i++){
            var r=order.get(i);
            for(int row=0;row<r.height;row++)System.arraycopy(r.alpha,row*r.width,alpha,(ys[i]+row)*width+xs[i],r.width);
            csv.append(String.format(Locale.ROOT,"%d,%d,%d,%d,%d,%.5f,%.5f,%.5f",r.cp,xs[i],ys[i],r.width,r.height,r.dx,r.dy,r.advance));
            if(phases>1)csv.append(',').append(r.phase);
            csv.append('\n');
        }
        return new Baked(ppem,width,height,alpha,csv.toString());
    }
    private static Raster raster(GlyphVector vector,FontRenderContext frc,int cp,int phase,int ppem,float advance){
        float offset=phase/4f*SS;
        Rectangle bounds=vector.getPixelBounds(frc,offset,0);
        int bx=(int)Math.floor(bounds.x/(float)SS)-1,by=(int)Math.floor(bounds.y/(float)SS)-1;
        int w=Math.max(3,(int)Math.ceil((bounds.x+bounds.width)/(float)SS)-bx+1);
        int h=Math.max(3,(int)Math.ceil((bounds.y+bounds.height)/(float)SS)-by+1);
        var high=new BufferedImage(w*SS,h*SS,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=high.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(Color.WHITE);g.drawGlyphVector(vector,-bx*SS+offset,-by*SS);g.dispose();
        int[] pixels=((DataBufferInt)high.getRaster().getDataBuffer()).getData();int stride=w*SS;
        byte[] alpha=new byte[w*h];
        for(int py=0;py<h;py++)for(int px=0;px<w;px++){
            int sum=0;
            for(int yy=0;yy<SS;yy++){int base=(py*SS+yy)*stride+px*SS;for(int xx=0;xx<SS;xx++)sum+=pixels[base+xx]>>>24;}
            // Area filter, then a gentle coverage correction for light text over dark, non-linear GUI targets.
            int a=Math.round(sum/(float)(SS*SS));
            alpha[py*w+px]=(byte)Math.round(Math.pow(a/255.0,.88)*255);
        }
        return new Raster(cp,phase,w,h,alpha,bx,ppem*.82f+by,advance);
    }

    /** Compact cache file: header, metrics text, deflated alpha mask. */
    public static void write(Baked baked,OutputStream target)throws IOException{
        var out=new DataOutputStream(new BufferedOutputStream(target));
        out.writeInt(0x56464E54);out.writeInt(VERSION);out.writeInt(baked.ppem);out.writeInt(baked.width);out.writeInt(baked.height);
        byte[] csv=baked.csv.getBytes(StandardCharsets.UTF_8);out.writeInt(csv.length);out.write(csv);
        var deflater=new Deflater(Deflater.BEST_SPEED);
        try(var z=new DeflaterOutputStream(out,deflater)){z.write(baked.alpha);}finally{deflater.end();}
    }
    public static Baked read(InputStream source)throws IOException{
        var in=new DataInputStream(new BufferedInputStream(source));
        if(in.readInt()!=0x56464E54||in.readInt()!=VERSION)throw new IOException("Stale font cache");
        int ppem=in.readInt(),width=in.readInt(),height=in.readInt();
        byte[] csv=new byte[in.readInt()];in.readFully(csv);
        byte[] alpha=new byte[width*height];
        try(var z=new InflaterInputStream(in)){new DataInputStream(z).readFully(alpha);}
        return new Baked(ppem,width,height,alpha,new String(csv,StandardCharsets.UTF_8));
    }
}
