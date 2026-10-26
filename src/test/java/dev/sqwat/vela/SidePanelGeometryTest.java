package dev.sqwat.vela;

import dev.sqwat.vela.ui.render.Rect;
import dev.sqwat.vela.ui.render.SidePanelGeometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SidePanelGeometryTest {
    @Test void wideViewportKeepsDrawerOutsideWindow(){
        Rect window=new Rect(100,40,547,330);
        Rect panel=SidePanelGeometry.place(1000,600,window,244,1);
        assertEquals(655,panel.x());assertEquals(180,panel.width());assertEquals(244,panel.height());
    }
    @Test void narrowViewportKeepsEveryAnimationFrameInsideScreen(){
        for(int width:new int[]{320,640,854,1280})for(int height:new int[]{240,360,720})for(int step=0;step<=20;step++){
            Rect panel=SidePanelGeometry.place(width,height,new Rect(8,20,547,330),244,step/20f);
            assertTrue(panel.x()>=8);assertTrue(panel.y()>=8);
            assertTrue(panel.x()+panel.width()<=width-8);assertTrue(panel.y()+panel.height()<=height-8);
        }
    }
}
