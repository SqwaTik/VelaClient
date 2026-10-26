package dev.sqwat.vela;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UiAssetsTest {
    @Test void pngFontsCoverEnglishRussianAndFitAtlas() throws Exception {
        for(String weight:List.of("regular","medium","semibold")){
            String base="/assets/velaclient/";
            var image=ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(base+"textures/gui/font_"+weight+".png")));
            String metrics=new String(Objects.requireNonNull(getClass().getResourceAsStream(base+"ui/font_"+weight+".csv")).readAllBytes(),StandardCharsets.UTF_8);
            Set<Integer> chars=new HashSet<>();
            for(String line:metrics.lines().skip(1).toList()){
                String[] v=line.split(",");chars.add(Integer.parseInt(v[0]));
                assertTrue(Integer.parseInt(v[1])+Integer.parseInt(v[3])<=image.getWidth());
                assertTrue(Integer.parseInt(v[2])+Integer.parseInt(v[4])<=image.getHeight());
                assertTrue(Float.parseFloat(v[7])>=0,"Negative advance for "+v[0]);
            }
            "Vela Visuals HUD Utilities Markers Friends Configs Поиск Настройки Ёё йЙ Щщ Ыы 0123456789 …".codePoints().forEach(cp->assertTrue(chars.contains(cp),weight+" missing "+cp));
            assertEquals(2048,image.getWidth());
        }
    }
    @Test void iconsAndChromeHaveAntialiasedTransparency() throws Exception {
        for(String name:List.of("rounded","icon_visuals","icon_hud","icon_utilities","icon_markers","icon_friends","icon_configs","icon_settings","color_wheel","logo")){
            var image=ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/assets/velaclient/textures/gui/"+name+".png")));
            boolean edge=false,transparent=false;
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
                int a=image.getRGB(x,y)>>>24;edge|=a>0&&a<255;transparent|=a==0;
            }
            assertTrue(edge,name+" missing antialiasing");assertTrue(transparent,name+" missing transparency");
        }
    }
}
