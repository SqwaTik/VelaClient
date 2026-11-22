package dev.sqwat.vela.compat.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Vector2fc;

/**
 * The 2D pose (1.21.6's Matrix3x2fStack) over 1.21.5's PoseStack: reads the stack's top on each use and writes every change
 * back to its x/y part, so vanilla's z (item and tooltip layers) stays put. Push and pop go straight to the PoseStack.
 */
public final class Pose2D extends Matrix3x2fStack {
    private static Pose2D last;
    private final GuiGraphics graphics;
    private final PoseStack stack;

    private Pose2D(GuiGraphics graphics){super(1);this.graphics=graphics;this.stack=graphics.pose();}

    /** {@code g.pose()} as written for 1.21.6+. */
    public static Pose2D of(GuiGraphics g){
        Pose2D pose=last;
        if(pose==null||pose.graphics!=g)last=pose=new Pose2D(g);
        return pose.sync();
    }
    /** The z of the current GUI layer (tooltips and items translate it). */
    public static float z(GuiGraphics g){return g.pose().last().pose().m32();}

    private Pose2D sync(){
        Matrix4f m=stack.last().pose();
        super.set(m.m00(),m.m01(),m.m10(),m.m11(),m.m30(),m.m31());
        return this;
    }
    private Pose2D write(){
        Matrix4f m=stack.last().pose();
        m.m00(m00).m01(m01).m10(m10).m11(m11).m30(m20).m31(m21);
        return this;
    }

    @Override public Matrix3x2fStack pushMatrix(){stack.pushPose();return sync();}
    @Override public Matrix3x2fStack popMatrix(){stack.popPose();return sync();}
    @Override public Matrix3x2fStack clear(){return sync();}
    @Override public Matrix3x2f translate(float x,float y){sync();super.translate(x,y);return write();}
    @Override public Matrix3x2f translate(Vector2fc offset){return translate(offset.x(),offset.y());}
    @Override public Matrix3x2f translateLocal(float x,float y){sync();super.translateLocal(x,y);return write();}
    @Override public Matrix3x2f scale(float x,float y){sync();super.scale(x,y);return write();}
    @Override public Matrix3x2f scale(float xy){return scale(xy,xy);}
    @Override public Matrix3x2f rotate(float angle){sync();super.rotate(angle);return write();}
    @Override public Matrix3x2f rotateAbout(float angle,float x,float y){sync();super.rotateAbout(angle,x,y);return write();}
    @Override public Matrix3x2f mul(Matrix3x2fc right){sync();super.mul(right);return write();}
    @Override public Matrix3x2f identity(){super.identity();return write();}
    @Override public Matrix3x2f set(Matrix3x2fc m){super.set(m);return write();}
}
