package dev.sqwat.vela.render.effect;

import dev.sqwat.vela.ui.render.GlassSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.Arrays;

/** Shared perspective-correct annulus for jump and hit shockwaves. */
public final class GlassWave {
    private final WaveMesh mesh=new WaveMesh();
    private final Matrix4f matrix=new Matrix4f();
    public void draw(GuiGraphicsExtractor g,Vec3 center,Vec3 axisX,Vec3 axisY,double radius,int tint){
        if(radius<.01||(tint>>>24)==0)return;
        var camera=Minecraft.getInstance().gameRenderer.mainCamera();var origin=camera.position();
        camera.getViewRotationProjectionMatrix(matrix);
        int used=mesh.build(matrix,center,axisX,axisY,origin,radius,g.guiWidth(),g.guiHeight());
        // Render extraction owns an immutable copy until submission completes.
        if(used>0)GlassSurface.wave(g,Arrays.copyOf(mesh.vertices,used),tint);
    }
}
