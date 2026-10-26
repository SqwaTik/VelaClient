package dev.sqwat.vela.render;

import net.minecraft.world.phys.Vec3;
import org.joml.*;
import java.lang.Math;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaveMeshTest {
    @Test void sharedVerticesMatchOriginalProjectionIncludingNearPlane(){
        var mesh=new WaveMesh();
        var matrix=new Matrix4f().perspective(1.2f,16f/9,.05f,256);
        for(double z:new double[]{-8,-1,-.05,.2,10})for(double x:new double[]{0,1,20}){
            var center=new Vec3(x,.5,z);var axisX=new Vec3(1,0,0);var axisY=new Vec3(0,.6,.8);
            float[] expected=new float[2560];int used=0;
            var point=new Vector4f();
            for(int band=0;band<2;band++)for(int segment=0;segment<64;segment++){
                int first=used;
                for(int corner=0;corner<4;corner++){
                    int edge=(corner==1||corner==2)?band:band+1,angle=corner>=2?segment+1:segment;
                    double a=angle*Math.PI*2/64,cos=Math.cos(a),sin=Math.sin(a);
                    double radial=edge==0?.25:edge==1?.65:1,u=cos*2*radial,v=sin*2*radial;
                    point.set((float)(center.x+axisX.x*u+axisY.x*v),(float)(center.y+axisX.y*u+axisY.y*v),(float)(center.z+axisX.z*u+axisY.z*v),1).mul(matrix);
                    float px=(point.x/point.w+1)*1920/2f,py=(1-point.y/point.w)*1080/2f;
                    if(point.w<.05||Math.abs(px)>1920*3||Math.abs(py)>1080*3){used=first;break;}
                    expected[used++]=px;expected[used++]=py;expected[used++]=(float)(.5+cos*radial*.5);expected[used++]=(float)(.5+sin*radial*.5);expected[used++]=1/point.w;
                }
            }
            int actual=mesh.build(matrix,center,axisX,axisY,Vec3.ZERO,2,1920,1080);
            assertEquals(used,actual);
            assertArrayEquals(Arrays.copyOf(expected,used),Arrays.copyOf(mesh.vertices,actual));
        }
    }
}
