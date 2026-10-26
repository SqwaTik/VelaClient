package dev.sqwat.vela;
import dev.sqwat.vela.config.*;
import dev.sqwat.vela.ui.render.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.io.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class CrashAndAccountRegressionTest {
    @Test void hudContentCannotCloseTheOuterRevealClip()throws Exception{
        String source=Files.readString(Path.of("src/main/java/dev/sqwat/vela/ui/HudLayoutService.java"));
        String chip=source.substring(source.indexOf("public void drawChip("),source.indexOf("public String label("));
        assertFalse(chip.contains("disableScissor("),"HUD content must not pop a clip owned by extractRenderState");
        assertFalse(chip.contains("UiClip.enable("),"Nested HUD clipping needs its own balanced scope");
        assertEquals(1,source.split("disableScissor\\(",-1).length-1);
    }
    @Test void accountHighlightStaysOpaqueAtEveryAnimationFrame(){for(boolean active:new boolean[]{false,true})for(int i=0;i<=100;i++)assertEquals(255,AccountChrome.selection(active,i/100f)>>>24);assertNotEquals(AccountChrome.selection(false,0),AccountChrome.selection(false,1));}
    @Test void notificationButtonFillsInnerWidth(){for(int width:new int[]{94,160,208}){Rect r=NotificationChrome.action(12,10,width,false);assertEquals(20,r.x());assertEquals(width-16,r.width());assertEquals(12+width-8,r.x()+r.width());assertTrue(r.y()>30);}}
    @Test void compressedKeyIsShorterAndPreservesSettings(){var config=new ClientConfig();config.accentColor="#AB1234";String legacy="VELA1:"+Base64.getUrlEncoder().withoutPadding().encodeToString(new com.google.gson.Gson().toJson(config).getBytes(StandardCharsets.UTF_8));String compact=ConfigShare.encode(config);assertTrue(compact.length()<legacy.length());assertEquals(config.accentColor,ConfigShare.decode(compact).accentColor);assertEquals(config.accentColor,ConfigShare.decode(legacy).accentColor);System.out.println("Sample key length: legacy="+legacy.length()+", compressed="+compact.length());}
    @Test void compressedPayloadHasBoundedExpansion()throws Exception{var output=new ByteArrayOutputStream();try(var gzip=new GZIPOutputStream(output)){gzip.write(" ".repeat(100000).getBytes(StandardCharsets.UTF_8));}assertThrows(IllegalArgumentException.class,()->ConfigShare.decode("VELA2:"+Base64.getUrlEncoder().encodeToString(output.toByteArray())));}
    @Test void truncatedCompressedKeysAreRejected(){String key=ConfigShare.encode(new ClientConfig());assertThrows(IllegalArgumentException.class,()->ConfigShare.decode(key.substring(0,key.length()-12)));}
}
