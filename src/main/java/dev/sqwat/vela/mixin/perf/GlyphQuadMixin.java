package dev.sqwat.vela.mixin.perf;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.sqwat.vela.render.perf.FastCubes;
import dev.sqwat.vela.render.perf.QuadSink;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Glyph quads written whole (see {@link FastCubes#textQuad}): name tags over a crowd are thousands of them a frame. */
@Mixin(BakedSheetGlyph.class)
public abstract class GlyphQuadMixin {
    @Shadow @Final private float u0;
    @Shadow @Final private float u1;
    @Shadow @Final private float v0;
    @Shadow @Final private float v1;
    @Shadow @Final private float left;
    @Shadow @Final private float right;
    @Shadow @Final private float up;
    @Shadow @Final private float down;
    @Shadow private static float extraThickness(boolean bold) { throw new AssertionError(); }
    @Shadow private float shearTop() { throw new AssertionError(); }
    @Shadow private float shearBottom() { throw new AssertionError(); }

    @Inject(method = "render(ZFFFLorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IZI)V", at = @At("HEAD"), cancellable = true)
    private void vela$fastGlyph(boolean italic, float x, float y, float z, Matrix4fc pose, VertexConsumer builder, int color, boolean bold, int light, CallbackInfo ci) {
        if (!(builder instanceof QuadSink sink)) return;
        // Vanilla's corners, in vanilla's order.
        float x0 = x + left, x1 = x + right, y0 = y + up, y1 = y + down;
        float shearY0 = italic ? shearTop() : 0.0F, shearY1 = italic ? shearBottom() : 0.0F;
        float e = extraThickness(bold);
        if (FastCubes.textQuad(sink, pose, x0 + shearY0 - e, y0 - e, x0 + shearY1 - e, y1 + e, x1 + shearY1 + e, y1 + e, x1 + shearY0 + e, y0 - e,
                z, color, u0, v0, u1, v1, light)) ci.cancel();
    }
}
