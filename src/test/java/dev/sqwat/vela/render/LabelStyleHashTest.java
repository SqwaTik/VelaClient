package dev.sqwat.vela.render;
import dev.sqwat.vela.render.entity.LabelTextKey;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LabelStyleHashTest {
    @Test void reusedStyleHashPreservesEveryCharacterAndStyleTransition(){
        var plain=Style.EMPTY;var red=Style.EMPTY.withColor(0xFF3344);
        FormattedCharSequence text=sink->sink.accept(0,plain,'A')&&sink.accept(1,plain,'B')&&sink.accept(2,red,'C')&&sink.accept(3,red,'D');
        var key=new LabelTextKey();assertTrue(key.read(text));int hash=1;
        int[] chars={'A'|(1<<21),'B','C','D'};Style[] styles={plain,plain,red,red};
        for(int i=0;i<4;i++)hash=31*(31*hash+chars[i])+styles[i].hashCode();
        assertEquals(hash,key.hashCode());var saved=key.snapshot();assertTrue(key.read(text));assertEquals(saved,key);
        assertTrue(key.read(FormattedCharSequence.forward("ABCD",plain)));assertNotEquals(saved,key);
    }
}
