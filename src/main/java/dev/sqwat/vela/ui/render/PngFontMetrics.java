package dev.sqwat.vela.ui.render;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** CPU-only font metrics shared by the game and the offline visual regression renderer. */
public final class PngFontMetrics {
    public record Glyph(int codepoint,int x,int y,int width,int height,float bearingX,float bearingY,float advance) {}
    public record PlacedGlyph(Glyph glyph,float x) {}
    public record Line(List<PlacedGlyph> glyphs,float width) {}
    private final Map<Integer,Glyph> glyphs;
    private final it.unimi.dsi.fastutil.ints.Int2ObjectMap<Glyph[]> phases;
    private final Map<Long,Float> kerning;
    private final Map<String,Line> lines=new LinkedHashMap<>(128,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,Line> e){return size()>512;}
    };
    public final int width,height,ppem;
    public final float baseline;
    public PngFontMetrics(InputStream data,InputStream pairs)throws IOException {
        try(var reader=new BufferedReader(new InputStreamReader(Objects.requireNonNull(data),StandardCharsets.UTF_8))) {
            String[] header=reader.readLine().split(",");
            width=Integer.parseInt(header[0]);height=Integer.parseInt(header[1]);ppem=Integer.parseInt(header[2]);baseline=Float.parseFloat(header[3]);
            Map<Integer,Glyph> parsed=new HashMap<>();Map<Long,Glyph> variants=new HashMap<>();
            for(String line;(line=reader.readLine())!=null;){
                String[] v=line.split(",");int cp=Integer.parseInt(v[0]);
                Glyph glyph=new Glyph(cp,Integer.parseInt(v[1]),Integer.parseInt(v[2]),Integer.parseInt(v[3]),Integer.parseInt(v[4]),Float.parseFloat(v[5]),Float.parseFloat(v[6]),Float.parseFloat(v[7]));
                if(glyph.x<0||glyph.y<0||glyph.x+glyph.width>width||glyph.y+glyph.height>height||glyph.advance<0)throw new IOException("Invalid PNG glyph "+cp);
                int phase=v.length>8?Integer.parseInt(v[8]):0;
                if(phase==0)parsed.put(cp,glyph);variants.put(pair(cp,phase),glyph);
            }glyphs=Map.copyOf(parsed);
            phases=new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
            for(var entry:variants.entrySet()){
                int cp=(int)(entry.getKey()>>>32),phase=(int)(long)entry.getKey();
                if(phase<0||phase>3)throw new IOException("Invalid PNG glyph phase "+phase);
                Glyph[] array=phases.get(cp);if(array==null){array=new Glyph[4];phases.put(cp,array);}array[phase]=entry.getValue();
            }
        }
        Map<Long,Float> parsedPairs=new HashMap<>();
        if(pairs!=null)try(var reader=new BufferedReader(new InputStreamReader(pairs,StandardCharsets.UTF_8))){
            for(String line;(line=reader.readLine())!=null;){String[] v=line.split(",");parsedPairs.put(pair(Integer.parseInt(v[0]),Integer.parseInt(v[1])),Float.parseFloat(v[2])*ppem);}
        }
        kerning=Map.copyOf(parsedPairs);
        if(!glyphs.containsKey((int)'?'))throw new IOException("PNG font requires a fallback glyph");
    }
    public synchronized Line line(String text){
        return lines.computeIfAbsent(text,value->{
            var result=new ArrayList<PlacedGlyph>(value.length());float x=0;int previous=-1;
            for(int offset=0;offset<value.length();){
                int cp=value.codePointAt(offset);offset+=Character.charCount(cp);
                Glyph glyph=glyphs.getOrDefault(cp,glyphs.get((int)'?'));
                if(previous>=0)x+=kerning.getOrDefault(pair(previous,cp),0f);
                result.add(new PlacedGlyph(glyph,x));x+=glyph.advance;previous=cp;
            }return new Line(List.copyOf(result),x);
        });
    }
    public float centeredTop(String text,float em,float top,float height){
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(var placed:line(text).glyphs()){
            var glyph=placed.glyph();if(Character.isWhitespace(glyph.codepoint()))continue;
            min=Math.min(min,glyph.bearingY());max=Math.max(max,glyph.bearingY()+glyph.height());
        }
        return !Float.isFinite(min)?top+(height-em)/2:top+height/2-(min+max)*em/(2*ppem);
    }
    public boolean contains(int cp){return glyphs.containsKey(cp);}
    public Glyph phase(Glyph base,int phase){var variants=phases.get(base.codepoint);return variants!=null&&phase>=0&&phase<4&&variants[phase]!=null?variants[phase]:base;}
    public int cachedLines(){synchronized(this){return lines.size();}}
    private static long pair(int left,int right){return ((long)left<<32)|(right&0xffffffffL);}
}
