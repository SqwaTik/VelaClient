package dev.sqwat.vela;

import dev.sqwat.vela.ui.render.ClipBounds;
import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClipBoundsTest {
    @Test void rightEdgeSurvivesEveryGameScale(){
        for(int gameScale=1;gameScale<=8;gameScale++)
            for(float preference:new float[]{.75f,1f,1.25f})
                for(int x=0;x<550;x++){
                    float scale=1.5f*preference/gameScale;
                    var bounds=ClipBounds.transform(new Matrix3x2f().scale(scale),x,53,x+417,287);
                    assertTrue(bounds.left()<=x*scale);
                    assertTrue(bounds.right()>=(x+417)*scale);
                    assertTrue(bounds.top()<=53*scale);
                    assertTrue(bounds.bottom()>=287*scale);
                }
    }
    @Test void reproducesVanillaRightEdgeLoss(){
        float scale=.375f;int left=213,right=630;
        int vanillaRight=(int)Math.floor(left*scale)+(int)Math.floor((right-left)*scale);
        assertTrue(vanillaRight<right*scale);
        assertEquals(237,ClipBounds.transform(new Matrix3x2f().scale(scale),left,0,right,100).right());
    }
}
