package dev.sqwat.vela.compat.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.Nullable;

/** 1.21.6's GUI element: here drawn as soon as it is submitted (see {@link GuiSubmit}), inside GuiGraphics' own scissor. */
public interface GuiElementRenderState {
    void buildVertices(VertexConsumer out);
    RenderPipeline pipeline();
    TextureSetup textureSetup();
    @Nullable ScreenRectangle scissorArea();
    @Nullable default ScreenRectangle bounds(){return null;}
}
