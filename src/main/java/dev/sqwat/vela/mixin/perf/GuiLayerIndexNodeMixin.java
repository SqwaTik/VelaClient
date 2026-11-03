package dev.sqwat.vela.mixin.perf;

import dev.sqwat.vela.render.perf.IndexedAreaList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.ArrayList;

/** Layer element lists that are queried for overlap are indexed; see {@link IndexedAreaList}. */
@Mixin(targets="net.minecraft.client.renderer.state.gui.GuiRenderState$Node")
public abstract class GuiLayerIndexNodeMixin {
    @Redirect(method={"addItem","addText","addPicturesInPictureState","addGuiElement"},at=@At(value="NEW",target="java/util/ArrayList"),require=0)
    private ArrayList<?> vela$indexedList(){return new IndexedAreaList<>();}
}
