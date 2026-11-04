package dev.sqwat.vela.mixin.perf;

import dev.sqwat.vela.render.perf.DistantChunks;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Before vanilla collects dirty sections: sections compiled at the wrong distance level are marked dirty. */
@Mixin(LevelExtractor.class)
public abstract class DistantRecompileMixin {
    @Shadow @Final private LevelRenderer levelRenderer;
    @Shadow private ClientLevel level;
    @Shadow public abstract void setSectionDirty(int sectionX, int sectionY, int sectionZ);

    @Inject(method = "extract", at = @At("HEAD"))
    private void vela$distantLevels(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
        if (level == null) return;
        var pos = camera.position();
        DistantChunks.beforeSectionUpdates(SectionPos.blockToSectionCoord(pos.x), SectionPos.blockToSectionCoord(pos.z), levelRenderer.visibleSections(),
            node -> setSectionDirty(SectionPos.x(node), SectionPos.y(node), SectionPos.z(node)));
    }
}
