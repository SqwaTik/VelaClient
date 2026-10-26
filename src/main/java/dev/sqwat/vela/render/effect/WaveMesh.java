package dev.sqwat.vela.render.effect;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** Render-thread scratch space. Project shared ring vertices once, then assemble quads. */
final class WaveMesh {
    static final int SEGMENTS=64;
    private static final double[] COS=new double[SEGMENTS+1],SIN=new double[SEGMENTS+1];
    static {for(int i=0;i<=SEGMENTS;i++){double a=i*Math.PI*2/SEGMENTS;COS[i]=Math.cos(a);SIN[i]=Math.sin(a);}}
    final float[] vertices=new float[SEGMENTS*2*20];
    private final float[] points=new float[3*(SEGMENTS+1)*5];
    private final boolean[] valid=new boolean[3*(SEGMENTS+1)];
    private final Vector4f point=new Vector4f();

    int build(Matrix4fc matrix,Vec3 center,Vec3 axisX,Vec3 axisY,Vec3 origin,double radius,int width,int height){
        for(int edge=0;edge<3;edge++)for(int angle=0;angle<=SEGMENTS;angle++){
            int index=edge*(SEGMENTS+1)+angle,offset=index*5;
            double radial=edge==0?.25:edge==1?.65:1;
            double u=COS[angle]*radius*radial,v=SIN[angle]*radius*radial;
            point.set((float)(center.x+axisX.x*u+axisY.x*v-origin.x),
                (float)(center.y+axisX.y*u+axisY.y*v-origin.y),
                (float)(center.z+axisX.z*u+axisY.z*v-origin.z),1).mul(matrix);
            float x=(point.x/point.w+1)*width/2f,y=(1-point.y/point.w)*height/2f;
            valid[index]=point.w>=.05f&&Float.isFinite(x)&&Float.isFinite(y)&&Math.abs(x)<=width*3&&Math.abs(y)<=height*3;
            points[offset]=x;points[offset+1]=y;
            points[offset+2]=(float)(.5+COS[angle]*radial*.5);
            points[offset+3]=(float)(.5+SIN[angle]*radial*.5);
            points[offset+4]=1/point.w;
        }
        int used=0;
        for(int band=0;band<2;band++)for(int segment=0;segment<SEGMENTS;segment++){
            int first=used;
            for(int corner=0;corner<4;corner++){
                int edge=(corner==1||corner==2)?band:band+1,angle=corner>=2?segment+1:segment;
                int index=edge*(SEGMENTS+1)+angle;
                if(!valid[index]){used=first;break;}
                System.arraycopy(points,index*5,vertices,used,5);used+=5;
            }
        }
        return used;
    }
}
