package dev.sqwat.vela;
import dev.sqwat.vela.ui.ArrowPlacement;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
class ArrowPlacementTest {
    @Test void clusteredArrowsReserveLabelsWithoutOverlap(){
        var layout=new ArrowPlacement();
        var boxes=new ArrayList<float[]>();
        for(int i=0;i<24;i++){
            if(!layout.place(-1.57f,92,50,1,960,540))continue;
            float[] b={layout.x-46,layout.y-12,layout.x+46,layout.y+38};
            for(var p:boxes)assertFalse(b[0]<p[2]&&b[2]>p[0]&&b[1]<p[3]&&b[3]>p[1]);
            assertTrue(b[0]>=3&&b[1]>=3&&b[2]<=957&&b[3]<=537);
            boxes.add(b);
        }
        assertTrue(boxes.size()>1);
    }
}
