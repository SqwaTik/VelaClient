package dev.sqwat.vela.opt.gl;

import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;

// Sodium 0.6 counts a section's geometry in indices, 6 per quad of 4 vertices
@UtilityClass
public class SodiumSections {
	public long count(long data, int facing) {
		return SectionRenderDataUnsafe.getElementCount(data, facing) / 6 * 4;
	}
}
