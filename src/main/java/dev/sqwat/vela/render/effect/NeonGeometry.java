package dev.sqwat.vela.render.effect;

import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.world.phys.Vec3;

/** Bounded, world-space glow bands. No full-screen blur or texture readback. */
public final class NeonGeometry {
    public static final int SEGMENTS=64;
    private static final double[] COS=new double[SEGMENTS+1],SIN=new double[SEGMENTS+1];
    static{for(int i=0;i<=SEGMENTS;i++){double a=i*Math.PI*2/SEGMENTS;COS[i]=Math.cos(a);SIN[i]=Math.sin(a);}}
    private NeonGeometry(){}
    public static Vec3 point(Vec3 center,double radius,int segment){return center.add(COS[segment]*radius,0,SIN[segment]*radius);}
    public static int tint(int rgb,double opacity){return ((int)Math.round(255*Math.clamp(opacity,0,1))<<24)|(rgb&0xFFFFFF);}
    public static int core(int rgb){
        int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
        return ((r+(255-r)*3/4)<<16)|((g+(255-g)*3/4)<<8)|(b+(255-b)*3/4);
    }
    public static void ring(Vec3 center,double radius,double width,int rgb,double opacity){
        if(radius<=0||opacity<=0)return;
        // Broad low-opacity halo, narrower saturated tube, small white-hot core.
        band(center,radius,width*2.2,rgb,opacity*.055);
        band(center.add(0,.0005,0),radius,width*1.5,rgb,opacity*.12);
        band(center.add(0,.001,0),radius,width,rgb,opacity*.28);
        band(center.add(0,.0015,0),radius,width*.48,rgb,opacity*.72);
        band(center.add(0,.002,0),radius,width*.14,core(rgb),opacity);
    }
    private static void band(Vec3 center,double radius,double width,int rgb,double opacity){
        double inner=Math.max(0,radius-width/2),outer=radius+width/2;
        var style=GizmoStyle.fill(tint(rgb,opacity));
        for(int i=0;i<SEGMENTS;i++)Gizmos.rect(point(center,outer,i),point(center,inner,i),
            point(center,inner,i+1),point(center,outer,i+1),style);
    }
}
