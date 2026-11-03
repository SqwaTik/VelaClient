package dev.sqwat.vela;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UiAssetsTest {
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
