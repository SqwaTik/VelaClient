package dev.sqwat.vela;

import dev.sqwat.vela.ui.HudGeometry;
import dev.sqwat.vela.ui.render.Rect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudGeometryTest {
    @Test void notificationCenterDoesNotMoveWhenContentWidthChanges() {
        for(int width:new int[]{56,128,180,280})
            assertEquals(64,HudGeometry.notificationOffset(width)+width/2);
    }
    @Test void nearFitsNamesWithRoomForDistanceAndArrow() {
        assertEquals(112,HudGeometry.nearWidth(20));
        assertEquals(176,HudGeometry.nearWidth(100));
        assertEquals(200,HudGeometry.nearWidth(500));
    }
    @Test void controlsReclaimUnusedChevronSpace() {
        assertEquals(151,HudGeometry.toggleX(0,200,true));
        assertEquals(171,HudGeometry.toggleX(0,200,false));
    }
    @Test void contextStaysBesideHudAndInsideViewport() {
        Rect menu=HudGeometry.context(new Rect(280,170,30,20),320,200);
        assertEquals(108,menu.width());
        assertEquals(Math.round(58*HudGeometry.CONTEXT_SCALE),menu.height());
        assertEquals(1f,HudGeometry.CONTEXT_SCALE);
        assertTrue(menu.x()>=2 && menu.y()>=2);
        assertTrue(menu.x()+menu.width()<=318);
        assertTrue(menu.y()+menu.height()<=198);
        assertTrue(menu.x()+menu.width()<280);
    }
    @Test void expandedTargetSettingsStayInsideViewport() {
        Rect menu=HudGeometry.context(new Rect(280,170,30,20),320,200,176,106);
        assertEquals(176,menu.width());
        assertEquals(106,menu.height());
        assertTrue(menu.x()>=2 && menu.y()>=2);
        assertTrue(menu.x()+menu.width()<=318);
        assertTrue(menu.y()+menu.height()<=198);
    }
    @Test void toggleHoverDoesNotIncludeTheRestOfTheCard() {
        assertTrue(HudGeometry.toggleHit(100,20).contains(110,25));
        assertFalse(HudGeometry.toggleHit(100,20).contains(50,25));
        assertFalse(HudGeometry.toggleHit(100,20).contains(110,10));
    }
}
