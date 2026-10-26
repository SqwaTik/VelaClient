package dev.sqwat.vela.mixin;
import dev.sqwat.vela.render.OccludedRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(EntityRenderState.class)
abstract class OccludedRenderStateMixin implements OccludedRenderState {
    @Unique private boolean vela$occluded;
    @Override public boolean vela$occluded(){return vela$occluded;}
    @Override public void vela$occluded(boolean value){vela$occluded=value;}
}
