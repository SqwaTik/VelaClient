package dev.sqwat.vela;

import dev.sqwat.vela.render.perf.GuiSortOrder;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiSortOrderTest {
    @Test void primitiveScissorOrderMatchesVanillaIncludingNullsAndTies(){
        Comparator<ScreenRectangle> original=Comparator.nullsFirst(Comparator.comparing(ScreenRectangle::top)
                .thenComparing(ScreenRectangle::bottom).thenComparing(ScreenRectangle::left).thenComparing(ScreenRectangle::right));
        var random=new Random(41);
        var values=new ArrayList<ScreenRectangle>();values.add(null);
        for(int i=0;i<80;i++)values.add(new ScreenRectangle(random.nextInt(2000)-1000,random.nextInt(20),random.nextInt(50),random.nextInt(20)));
        values.addAll(List.of(new ScreenRectangle(0,0,20,20),new ScreenRectangle(0,0,21,20),new ScreenRectangle(-1,0,21,20)));
        for(var a:values)for(var b:values)assertEquals(Integer.signum(original.compare(a,b)),Integer.signum(GuiSortOrder.SCISSOR.compare(a,b)));
    }
}
