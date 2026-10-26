package dev.sqwat.vela.render;

import net.minecraft.world.phys.Vec3;

/** Tick-built geometry; only the advancing head needs interpolation during rendering. */
public final class TrailGeometry {
    private TrailGeometry(){}
    public record Segment(Vec3 firstStart,Vec3 firstEnd,Vec3 secondStart,Vec3 secondEnd){}
    public static Segment segment(TrailHistory.Point a,TrailHistory.Point b,double height,double width,boolean spiral,double head){
        Vec3 end=head>=1?b.position():a.position().lerp(b.position(),head);
        if(!spiral){
            double top=Math.min(height,height*width/3);
            return new Segment(a.position().add(0,.12,0),end.add(0,.12,0),a.position().add(0,top,0),end.add(0,top,0));
        }
        double phase=a.born()/1e9*8,finish=phase+(b.born()-a.born())/1e9*8*head;
        double radius=.08*width,mid=height*.5;
        Vec3 startOffset=new Vec3(Math.cos(phase)*radius,Math.sin(phase)*mid*.8,Math.sin(phase)*radius);
        Vec3 endOffset=new Vec3(Math.cos(finish)*radius,Math.sin(finish)*mid*.8,Math.sin(finish)*radius);
        Vec3 startCenter=a.position().add(0,mid,0),endCenter=end.add(0,mid,0);
        return new Segment(startCenter.add(startOffset),endCenter.add(endOffset),startCenter.subtract(startOffset),endCenter.subtract(endOffset));
    }
}
