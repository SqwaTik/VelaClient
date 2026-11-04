package dev.sqwat.vela.mixin.perf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.sqwat.vela.render.perf.CubeLayout;
import dev.sqwat.vela.render.perf.FastCubes;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.Cube.class)
public abstract class ModelCubeCompileMixin {
    @Shadow @Final public ModelPart.Polygon[] polygons;
    @Unique private CubeLayout vela$layout;

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void vela$fastCompile(PoseStack.Pose pose, VertexConsumer builder, int light, int overlay, int color, CallbackInfo ci) {
        CubeLayout layout = vela$layout;
        if (layout == null) vela$layout = layout = CubeLayout.of(polygons);
        if (FastCubes.compile(polygons, layout, pose, builder, light, overlay, color)) ci.cancel();
    }
}
