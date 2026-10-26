// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.gl;

import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;

// Sodium 0.7 and newer count a section's geometry in vertices
@UtilityClass
public class SodiumSections {
	public long count(long data, int facing) {
		return SectionRenderDataUnsafe.getVertexCount(data, facing);
	}
}
