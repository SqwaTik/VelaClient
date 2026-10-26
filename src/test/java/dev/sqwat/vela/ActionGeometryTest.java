package dev.sqwat.vela;
import dev.sqwat.vela.ui.render.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ActionGeometryTest {
    @Test void hoverOnlyExpandsRight(){
        Rect base=new Rect(30,40,59,22);
        for(int i=0;i<=100;i++){
            float p=i/100f;Rect hit=ActionGeometry.hit(base,p);
            assertEquals(30,hit.x());assertEquals(40,hit.y());
            assertTrue(hit.width()>=59&&hit.width()<=67);
            assertTrue(ActionGeometry.textShift(p)>=0);
        }
        assertEquals(67,ActionGeometry.hit(base,1).width());
        assertEquals(6,ActionGeometry.textShift(1));
    }
}
