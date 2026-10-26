package dev.sqwat.vela.mixin;

import dev.sqwat.vela.render.AppendOnlyRenderIndex;
import dev.sqwat.vela.zako.ZakoOptConfig;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;

@Mixin(targets="net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group")
public abstract class DrawGroupLookupMixin {
    @Unique private AppendOnlyRenderIndex<PreparedRenderType> vela$drawIndex;

    // Vanilla reaches this call only when reordering and consolidation are both permitted.
    @Redirect(method="getOrAddDraw",at=@At(value="INVOKE",target="Ljava/util/List;indexOf(Ljava/lang/Object;)I"))
    private int vela$indexedDrawLookup(List<PreparedRenderType> draws,Object value){
        if(!ZakoOptConfig.lookupCaches())return draws.indexOf(value);
        if(vela$drawIndex==null)vela$drawIndex=new AppendOnlyRenderIndex<>();
        return vela$drawIndex.indexOf(draws,(PreparedRenderType)value);
    }
}
