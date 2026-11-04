package dev.sqwat.vela.compat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

/** 1.21.11's Camera has no view-projection, frustum or panorama getters (26.x moved them there): LegacyCameraMixin records them each frame. */
public final class LegacyCamera {
    private static final Matrix4f VIEW_PROJECTION=new Matrix4f();
    private static Frustum cull;
    private static float handFov=70f;
    private LegacyCamera(){}
    public static void capture(Matrix4f viewRotation,Matrix4f projection){VIEW_PROJECTION.set(projection).mul(viewRotation);}
    public static void captureFrustum(Frustum frustum){cull=frustum;}
    public static void captureHandFov(float fov){handFov=fov;}
    public static Matrix4f viewRotationProjection(Camera camera,Matrix4f dest){return dest.set(VIEW_PROJECTION);}
    public static boolean panoramic(){return Minecraft.getInstance().gameRenderer.isPanoramicMode();}
    public static Frustum captured(){return Minecraft.getInstance().levelRenderer.getCapturedFrustum();}
    public static Frustum cull(Camera camera){
        if(cull==null){cull=new Frustum(new Matrix4f(),new Matrix4f(VIEW_PROJECTION));cull.prepare(camera.position().x,camera.position().y,camera.position().z);}
        return cull;
    }
    /** The first-person hand's projection: GameRenderer's "3d hud" perspective (near 0.05, far 100). */
    public static Matrix4f handProjection(Matrix4f dest){
        var w=Minecraft.getInstance().getWindow();
        return dest.setPerspective((float)Math.toRadians(handFov),(float)w.getWidth()/Math.max(1,w.getHeight()),0.05f,100f);
    }
}
