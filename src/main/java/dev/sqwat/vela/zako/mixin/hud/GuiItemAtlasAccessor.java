// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.hud;

import net.minecraft.client.gui.render.DynamicAtlasAllocator;
import net.minecraft.client.gui.render.GuiItemAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiItemAtlas.class)
public interface GuiItemAtlasAccessor {
	@Accessor("allocator")
	DynamicAtlasAllocator<Object> zakoopt$allocator();
}
