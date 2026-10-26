package dev.sqwat.vela.ui.render;

import org.joml.Matrix3x2fc;
import org.joml.Vector2f;

/** Outward-rounded bounds in Minecraft GUI coordinates. */
public record ClipBounds(int left,int top,int right,int bottom) {
    public static ClipBounds transform(Matrix3x2fc pose,int left,int top,int right,int bottom){
        Vector2f a=pose.transformPosition(left,top,new Vector2f());
        Vector2f b=pose.transformPosition(right,top,new Vector2f());
        Vector2f c=pose.transformPosition(left,bottom,new Vector2f());
        Vector2f d=pose.transformPosition(right,bottom,new Vector2f());
        return new ClipBounds(
            (int)Math.floor(Math.min(Math.min(a.x,b.x),Math.min(c.x,d.x))),
            (int)Math.floor(Math.min(Math.min(a.y,b.y),Math.min(c.y,d.y))),
            (int)Math.ceil(Math.max(Math.max(a.x,b.x),Math.max(c.x,d.x))),
            (int)Math.ceil(Math.max(Math.max(a.y,b.y),Math.max(c.y,d.y))));
    }
}
