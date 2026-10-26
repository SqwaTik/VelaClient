package dev.sqwat.vela;
import dev.sqwat.vela.render.camera.CameraBlurMotion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CameraBlurMotionTest {
    @Test void identicalAngularSpeedHasIdenticalExposure(){
        assertEquals(CameraBlurMotion.rotation(0,1,.01,90,.5f),CameraBlurMotion.rotation(0,2,.02,90,.5f),.000001f);
    }
    @Test void wrapStationaryAndDiscontinuities(){
        assertTrue(CameraBlurMotion.rotation(179,-179,.01,90,1)>0);
        assertEquals(0,CameraBlurMotion.rotation(0,0,.01,90,1));
        assertEquals(0,CameraBlurMotion.rotation(0,30,.5,90,1));
        assertEquals(127,CameraBlurMotion.encode(0));
        assertEquals(254,CameraBlurMotion.encode(10));
    }
    @Test void unchangedAnglesNeverCreateBlur(){
        assertEquals(0,CameraBlurMotion.rotation(42,42,.001,90,.7f));
        assertEquals(0,CameraBlurMotion.rotation(42,42,.04,90,.7f));
    }
}
