package dev.sqwat.vela.ui.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.DynamicTexture;

/** PNG available during the very first resource reload, with linear filtering. */
public final class StartupLogoTexture extends DynamicTexture {
    public StartupLogoTexture(NativeImage pixels){
        super(()->"Vela logo",pixels);
        sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }
}
