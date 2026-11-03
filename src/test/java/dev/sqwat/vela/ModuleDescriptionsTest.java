package dev.sqwat.vela;
import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.ui.render.PngFontMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class ModuleDescriptionsTest {
    @TempDir Path root;
    @Test void descriptionsFitTwoLinesWithoutEllipsis()throws Exception{
        var font=dev.sqwat.vela.ui.render.PngFontBaker.bake(dev.sqwat.vela.ui.render.PngFontBaker.font("regular"),12).metrics("regular");
        try(var services=new VelaServices(root)){
            for(var module:services.modules().all()){
                String text=module.description();int split=text.length();
                while(split>0&&font.line(text.substring(0,split)).width()*8/font.ppem>110)split--;
                if(split<text.length()){int space=text.lastIndexOf(' ',split);if(space>0)split=space;}
                assertTrue(font.line(text.substring(split).strip()).width()*8/font.ppem<=180,module.id()+": "+text);
                assertFalse(text.contains("…"));
            }
        }
    }
}
