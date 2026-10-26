package dev.sqwat.vela;

import dev.sqwat.vela.ui.HudVisibility;
import dev.sqwat.vela.ui.render.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OverlayRegressionTest {
    @Test void loadingNeverShowsHud(){
        for(boolean screen:new boolean[]{false,true})
            for(boolean chat:new boolean[]{false,true})
                for(boolean editor:new boolean[]{false,true})
                    assertFalse(HudVisibility.allowed(true,screen,chat,editor));
    }
    @Test void hudRemainsVisibleAcrossWorldScreens(){
        assertTrue(HudVisibility.allowed(false,false,false,false));
        assertTrue(HudVisibility.allowed(false,true,false,false));
        assertTrue(HudVisibility.allowed(false,true,true,false));
        assertTrue(HudVisibility.allowed(false,true,false,true));
    }
    @Test void dragButtonFollowsInputAndKeepsPhysicalSize(){
        for(int scale:new int[]{1,2,4}){
            var view=UiViewport.fit(1920/scale,1080/scale,scale,1);
            var r=ChatDragGeometry.place(view,4,1080/scale-12,1920/scale-4);
            assertEquals(33,r.width()*view.pixelsPerPoint(),.01);
            assertTrue((r.y()+r.height())*view.scale()<1080/scale-12);
            var moved=ChatDragGeometry.place(view,4,1080/scale-42,1920/scale-4);
            assertEquals(30/view.scale(),r.y()-moved.y(),1);
        }
    }
}
