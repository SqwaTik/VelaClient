package dev.sqwat.vela.render.view;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * This frame's walking bob, so world-space overlays can stay still on screen while the camera bobs. Vanilla puts the
 * bob into the projection; {@link #steady} moves a world point so that, after the bob, it lands where it would have
 * landed without one.
 */
public final class ViewBob {
    private static final Matrix4f CANCEL=new Matrix4f();
    private static boolean active;
    private ViewBob(){}

    /** Called at the start of each level frame, before the bob (if any) is captured. */
    public static void reset(){active=false;CANCEL.identity();}
    /** {@code bob} is the bob transform in view space, {@code view} the camera's world-to-view rotation. */
    public static void capture(Matrix4f bob,Matrix4f view){
        // world offset -> view -> undo bob -> back to world
        CANCEL.set(view).invertAffine().mul(new Matrix4f(bob).invertAffine()).mul(view);
        active=true;
    }
    /** {@code point} moved so the bob does not shift it on screen; {@code camera} is the camera position. */
    public static Vec3 steady(Vec3 point,Vec3 camera){
        if(!active)return point;
        var v=new Vector3f((float)(point.x-camera.x),(float)(point.y-camera.y),(float)(point.z-camera.z));
        CANCEL.transformPosition(v);
        return new Vec3(camera.x+v.x,camera.y+v.y,camera.z+v.z);
    }
}
