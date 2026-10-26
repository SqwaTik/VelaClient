package dev.sqwat.vela.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;

/** Local transform used by the 26.2 vanilla skull item definitions. */
public final class LegacyHeadTransform {
    private static final Matrix4fc LOCAL=new Matrix4f().translation(.5f,0,.5f).rotate(new Quaternionf(1,0,0,0));
    private LegacyHeadTransform() {}
    public static Matrix4fc compose(Matrix4fc parent) { return new Matrix4f(parent).mul(LOCAL); }
}
