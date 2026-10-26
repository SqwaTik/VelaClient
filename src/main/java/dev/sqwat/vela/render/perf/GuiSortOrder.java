package dev.sqwat.vela.render.perf;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import java.util.Comparator;

/** Vanilla ordering, with primitive keys to avoid boxing for every comparison. */
public final class GuiSortOrder {
    public static final Comparator<ScreenRectangle> SCISSOR=Comparator.nullsFirst(
            Comparator.comparingInt(ScreenRectangle::top).thenComparingInt(ScreenRectangle::bottom)
                    .thenComparingInt(ScreenRectangle::left).thenComparingInt(ScreenRectangle::right));
    public static final Comparator<GuiElementRenderState> ELEMENT=Comparator.comparing(GuiElementRenderState::scissorArea,SCISSOR)
            .thenComparing(GuiElementRenderState::pipeline,Comparator.comparingInt(RenderPipeline::getSortKey))
            .thenComparing(GuiElementRenderState::textureSetup,Comparator.nullsFirst(Comparator.comparingInt(TextureSetup::getSortKey)));
    private GuiSortOrder(){}
}
