package dev.sqwat.vela.render.shader;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** 1.21.11: the camera values the shader passes read, taken from LevelRenderer.renderLevel (26.x keeps them in CameraRenderState). */
public final class ShaderCamera {
    private static final ShaderCamera current=new ShaderCamera();
    public Vec3 pos;
    public final Matrix4f viewRotationMatrix=new Matrix4f();
    public FogType fogType=FogType.NONE;
    public float depthFar;
    private ShaderCamera(){}
    public static ShaderCamera capture(Camera camera,Matrix4fc view){
        current.pos=camera.position();current.viewRotationMatrix.set(view);current.fogType=camera.getFluidInCamera();
        current.depthFar=Minecraft.getInstance().gameRenderer.getDepthFar();
        return current;
    }
}
