package dev.sqwat.vela;

import dev.sqwat.vela.ui.render.*;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PngRenderingTest {
    @Test void iconsHaveExactSmallPhysicalSizes()throws Exception{
        for(String name:List.of("visuals","hud","utilities","markers","friends","configs","settings","search","arrow","back","x","plus","check","trash","copy","folder","key","star","star_filled","download","save","move","microsoft","chevron"))
            for(int size=8;size<=64;size++){
                String path="textures/gui/icon_"+name+"_"+size+".png";
                try(var stream=resource(path)){
                    var image=ImageIO.read(stream);
                    assertEquals(size,image.getWidth());assertEquals(size,image.getHeight());
                }
                try(var metadata=resource(path+".mcmeta")){assertTrue(metadata.readAllBytes().length>0);}
            }
    }
    private static final int[] SIZES={8,9,10,11,12,13,14,15,16,18,19,20,22,24,28,32,36,40,48};
    private InputStream resource(String name){return Objects.requireNonNull(getClass().getResourceAsStream("/assets/velaclient/"+name),name);}
    private PngFontMetrics font(String weight,int ppem)throws Exception{return new PngFontMetrics(resource("ui/font_"+weight+"_"+ppem+".csv"),resource("ui/kerning_"+weight+".csv"));}

    @Test void gameDecoderPreservesAllFontMaskPixels()throws Exception {
        for(String weight:List.of("regular","medium","semibold"))for(int size:SIZES){
            String path="textures/gui/font_"+weight+"_"+size+".png";
            try(var input=resource(path)){
                byte[] png=input.readAllBytes();var reference=ImageIO.read(new ByteArrayInputStream(png));
                try(var nativeImage=com.mojang.blaze3d.platform.NativeImage.read(png)){
                    assertEquals(reference.getWidth(),nativeImage.getWidth());assertEquals(reference.getHeight(),nativeImage.getHeight());
                    assertArrayEquals(reference.getRGB(0,0,reference.getWidth(),reference.getHeight(),null,0,reference.getWidth()),nativeImage.getPixels(),path);
                }
            }
        }
    }

    @Test void everyRasterSizeHasRussianEnglishAndFourSubpixelPhases()throws Exception {
        for(String weight:List.of("regular","medium","semibold"))for(int size:SIZES){
            var font=font(weight,size);
            assertEquals(size,font.ppem);
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя…".codePoints().forEach(cp->assertTrue(font.contains(cp),weight+size+" missing "+cp));
            var glyph=font.line("W").glyphs().getFirst().glyph();
            for(int phase=1;phase<4;phase++){var variant=font.phase(glyph,phase);assertNotEquals(glyph.x()+","+glyph.y(),variant.x()+","+variant.y());assertEquals(glyph.advance(),variant.advance(),.0001);}
            assertEquals(font.width,ImageIO.read(resource("textures/gui/font_"+weight+"_"+size+".png")).getWidth());
        }
    }
    @Test void fontMasksRetainWhiteRgbInTransparentAndAntialiasedPixels()throws Exception {
        for(String weight:List.of("regular","medium","semibold")){
            var image=ImageIO.read(resource("textures/gui/font_"+weight+"_13.png"));boolean coverage=false;
            for(int y=0;y<image.getHeight();y+=3)for(int x=0;x<image.getWidth();x+=3){int p=image.getRGB(x,y);assertEquals(0xffffff,p&0xffffff);coverage|=(p>>>24)>0&&(p>>>24)<255;}
            assertTrue(coverage,"Glyph masks need fractional coverage");
        }
    }
    @Test void kerningAndLayoutCachePreserveSpacingAndBoundMemory()throws Exception {
        var font=font("regular",18);
        assertTrue(font.line("AV").width()<font.line("A").width()+font.line("V").width());
        var line=font.line("Настройки Vela");assertSame(line,font.line("Настройки Vela"));
        assertTrue(font.line("Hello 🛰").width()>0);
        for(int i=0;i<4096;i++)font.line("Dynamic "+i);assertTrue(font.cachedLines()<=512);
    }
    @Test void windowAndPointerScaleStayConsistentAcrossGameGuiScales(){
        for(int gameScale=1;gameScale<=8;gameScale++)for(float preference:new float[]{.75f,1,1.25f}){
            var view=UiViewport.fit(1920/gameScale,1080/gameScale,gameScale,preference);
            assertEquals(1.5f*preference,view.pixelsPerPoint(),.0001);
            var layout=DashboardChrome.layout(view.width(),view.height());
            for(int tab=0;tab<DashboardChrome.SECTIONS.length;tab++){
                Rect hit=layout.navigation(tab);double x=hit.x()+hit.width()/2.0,y=hit.y()+hit.height()/2.0;
                assertTrue(hit.contains(view.local(x*view.scale()),view.local(y*view.scale())));
            }
            assertTrue(layout.x()>=0&&layout.y()>=0);
            assertTrue(layout.search().x()>layout.x()+layout.sidebar());
            assertTrue(layout.search().x()+layout.search().width()<layout.settings().x());
        }
    }
    @Test void compactWindowsKeepActionsInsideAndSeparate(){
        for(int[] viewport:new int[][]{{320,240},{640,360},{854,480},{1280,720},{1920,1080},{3840,2160}}){
            var view=UiViewport.fit(viewport[0],viewport[1],1,1);
            var layout=AccountChrome.layout(view.width(),view.height());
            assertTrue(layout.x()>=0&&layout.y()>=0);
            assertTrue(layout.offline().y()+layout.offline().height()<layout.y()+layout.height());
            assertTrue(layout.microsoft().x()+layout.microsoft().width()<layout.offline().x());
            assertTrue(layout.list().x()+layout.list().width()<layout.split());
            var dash=DashboardChrome.layout(view.width(),view.height());
            int last=DashboardChrome.SECTIONS.length-1;
            assertTrue(dash.navigation(last).y()+dash.navigation(last).height()<dash.y()+dash.height()-23);
        }
    }
    @Test void labelInkIsCenteredAndInvalidScaleHasSafeFallback()throws Exception {
        var font=font("medium",18);
        for(String text:List.of("Настройки","Microsoft","Offline","Применить","gyjp","ABC")){
            var glyphs=font.line(text).glyphs();float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
            for(var placed:glyphs){var g=placed.glyph();min=Math.min(min,g.bearingY());max=Math.max(max,g.bearingY()+g.height());}
            float em=26f/3,top=font.centeredTop(text,em,20,25);
            assertEquals(32.5f,top+(min+max)*em/36,.0001f);
        }
        assertTrue(Float.isFinite(font.centeredTop(" ",9,20,25)));
        assertEquals(1.5f,UiViewport.fit(1920,1080,1,Float.NaN).pixelsPerPoint());
        assertEquals(1.5f,UiViewport.fit(1920,1080,1,Float.POSITIVE_INFINITY).pixelsPerPoint());
    }
    @Test void nineSliceGeometryNeverProducesNegativeQuadsOrAtlasBleeding(){
        for(float density:new float[]{.5f,.75f,1.125f,1.5f,1.875f,2.25f,4f})for(int radius=1;radius<=20;radius++)for(boolean outline:new boolean[]{false,true}){
            final int[] count={0};
            ChromeAtlas.draw(10,20,40,24,radius,density,outline,(x,y,w,h,u,v,sw,sh)->{
                assertTrue(w>0&&h>0);assertTrue(u>=0&&v>=0&&u+sw<=1024&&v+sh<=1024);assertTrue(Float.isFinite(x)&&Float.isFinite(y));count[0]++;
            });assertTrue(count[0]>=4&&count[0]<=9);
        }
    }
    @Test void cardBordersKeepBothSidesOnPhysicalPixelBoundaries(){
        for(float density:new float[]{.75f,1.125f,1.5f,1.875f,2.25f})
            for(int x:new int[]{11,213,217,319})for(int height:new int[]{44,78,177}){
                float[] bounds={Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY};
                ChromeAtlas.draw(x,53,201,height,7,density,true,(qx,qy,w,h,u,v,sw,sh)->{
                    bounds[0]=Math.min(bounds[0],qx);
                    bounds[1]=Math.max(bounds[1],qx+w);
                });
                assertEquals(Math.round(x*density)-1,bounds[0]*density,.001f);
                assertEquals(Math.round((x+201)*density)+1,bounds[1]*density,.001f);
            }
    }
}
