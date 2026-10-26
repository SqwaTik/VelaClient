package dev.sqwat.vela;
import dev.sqwat.vela.render.perf.SignTextWidths;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SignTextWidthsTest {
    @Test void identityFontChangesAndReloadInvalidate(){
        SignTextWidths.clear();Object font=new Object();
        var a=FormattedCharSequence.forward("Test",Style.EMPTY);var b=FormattedCharSequence.forward("Test",Style.EMPTY);
        assertEquals(-1,SignTextWidths.get(font,a));SignTextWidths.put(a,22);
        assertEquals(22,SignTextWidths.get(font,a));assertEquals(-1,SignTextWidths.get(font,b));
        assertEquals(-1,SignTextWidths.get(new Object(),a));SignTextWidths.put(a,30);SignTextWidths.clear();
        assertEquals(-1,SignTextWidths.get(font,a));
    }
    @Test void cacheEvictsOldLines(){
        SignTextWidths.clear();var font=new Object();var first=FormattedCharSequence.forward("first",Style.EMPTY);
        SignTextWidths.get(font,first);SignTextWidths.put(first,7);
        for(int i=0;i<2048;i++)SignTextWidths.put(FormattedCharSequence.forward("line"+i,Style.EMPTY),i);
        assertEquals(-1,SignTextWidths.get(font,first));SignTextWidths.clear();
    }
}
