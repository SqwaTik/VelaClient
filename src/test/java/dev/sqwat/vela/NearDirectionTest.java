package dev.sqwat.vela;
import dev.sqwat.vela.ui.hud.NearDirection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NearDirectionTest {
    private void points(double dx,double dz,float yaw,double x,double y){
        float angle=NearDirection.radians(dx,dz,yaw);
        assertEquals(x,Math.cos(angle),.00001);assertEquals(y,Math.sin(angle),.00001);
    }
    @Test void directionsRelativeToPlayer(){
        points(0,10,0,0,-1);points(0,-10,0,0,1);
        points(10,0,0,-1,0);points(-10,0,0,1,0);
        points(-10,0,90,0,-1);points(10,0,-90,0,-1);
    }
    @Test void atMostFiveEntries(){assertEquals(5,NearDirection.LIMIT);}
}
