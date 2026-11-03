package dev.sqwat.vela;

import dev.sqwat.vela.ui.render.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PngFontBakerTest {
    static final int[] SIZES={8,9,10,11,12,13,14,15,16,18,19,20,22,24,28,32,36,40,48};

    @Test void bakesEveryAtlasWithRussianEnglishAndSmallSizePhases()throws Exception{
        long start=System.nanoTime();
        for(String weight:List.of("regular","medium","semibold")){
            var source=PngFontBaker.font(weight);
            for(int size:SIZES){
                var baked=PngFontBaker.bake(source,size);var font=baked.metrics(weight);
                assertEquals(size,font.ppem);assertEquals(size>=28?2048:1024,font.width);
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя…№«»".codePoints()
                    .forEach(cp->assertTrue(font.contains(cp),weight+size+" missing "+cp));
                assertFalse(font.contains(0x4E00),"CJK falls back to Minecraft's font");
                var glyph=font.line("W").glyphs().getFirst().glyph();
                if(size<16)for(int phase=1;phase<4;phase++){var variant=font.phase(glyph,phase);assertNotEquals(glyph.x()+","+glyph.y(),variant.x()+","+variant.y());assertEquals(glyph.advance(),variant.advance(),.0001);}
                else assertSame(glyph,font.phase(glyph,2));
                boolean partial=false;for(byte a:baked.alpha())partial|=(a&255)>0&&(a&255)<255;
                assertTrue(partial,"glyph masks need fractional coverage");
            }
        }
        System.out.println("Baked 57 atlases in "+(System.nanoTime()-start)/1_000_000+" ms");
    }
    @Test void cacheRoundTripsAndRejectsOtherVersions()throws Exception{
        var baked=PngFontBaker.bake(PngFontBaker.font("medium"),13);
        var bytes=new ByteArrayOutputStream();PngFontBaker.write(baked,bytes);
        var read=PngFontBaker.read(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(baked.csv(),read.csv());assertArrayEquals(baked.alpha(),read.alpha());assertEquals(baked.height(),read.height());
        byte[] stale=bytes.toByteArray();stale[7]++;
        assertThrows(IOException.class,()->PngFontBaker.read(new ByteArrayInputStream(stale)));
        System.out.println("medium 13 cache: "+bytes.size()+" bytes");
    }
    @Test void kerningStillApplies()throws Exception{
        var font=PngFontBaker.bake(PngFontBaker.font("regular"),18).metrics("regular");
        assertTrue(font.line("AV").width()<font.line("A").width()+font.line("V").width());
    }
}
