package dev.sqwat.vela.mixin.perf;

import dev.sqwat.vela.module.Optimization;
import dev.sqwat.vela.render.perf.MapAtlas;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/**
 * Every map packet forces a full re-upload of the map (16384 pixels converted, then a texture write), even when only its
 * markers changed or a server resends the same picture. The pixels come from the colours alone, so equal colours skip it.
 * Each upload is also copied into {@link MapAtlas}.
 */
@Mixin(targets = "net.minecraft.client.resources.MapTextureManager$MapInstance")
public abstract class MapInstanceUploadMixin {
    @Shadow private MapItemSavedData data;
    @Shadow @Final private DynamicTexture texture;
    @Shadow private boolean requiresUpload;
    @Shadow @Final private Identifier location;
    @Unique private byte[] vela$uploaded;

    @Inject(method = "updateTextureIfNeeded", at = @At("HEAD"))
    private void vela$skipUnchanged(CallbackInfo ci) {
        if (!Optimization.mapAtlasOn()) return;
        if (requiresUpload && vela$uploaded != null && Arrays.equals(vela$uploaded, data.colors)) requiresUpload = false;
        if (!requiresUpload && vela$uploaded != null) MapAtlas.ensure(location, texture.getPixels());
    }

    @Inject(method = "updateTextureIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V", shift = At.Shift.AFTER))
    private void vela$uploaded(CallbackInfo ci) {
        byte[] colors = data.colors;
        if (vela$uploaded == null || vela$uploaded.length != colors.length) vela$uploaded = colors.clone();
        else System.arraycopy(colors, 0, vela$uploaded, 0, colors.length);
        MapAtlas.upload(location, texture.getPixels());
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void vela$release(CallbackInfo ci) {
        MapAtlas.release(location);
    }
}
