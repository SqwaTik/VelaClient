package dev.sqwat.vela.render.effect;

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
        // A helix around the direction of travel: sideways and vertical, never along the path (that tangled it).
        double radius=.12*width,mid=height*.5,lift=Math.min(mid*.8,radius*1.4);
        double dx=b.position().x-a.position().x,dz=b.position().z-a.position().z,len=Math.hypot(dx,dz);
        double sx=len>1e-4?-dz/len:1,sz=len>1e-4?dx/len:0;
        Vec3 startOffset=new Vec3(sx*Math.cos(phase)*radius,Math.sin(phase)*lift,sz*Math.cos(phase)*radius);
        Vec3 endOffset=new Vec3(sx*Math.cos(finish)*radius,Math.sin(finish)*lift,sz*Math.cos(finish)*radius);
        Vec3 startCenter=a.position().add(0,mid,0),endCenter=end.add(0,mid,0);
        return new Segment(startCenter.add(startOffset),endCenter.add(endOffset),startCenter.subtract(startOffset),endCenter.subtract(endOffset));
    }
}
