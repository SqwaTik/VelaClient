// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.gl;

import com.mojang.blaze3d.vertex.VertexFormat;

public interface RawBuffer {
	boolean zakoopt$accepts(VertexFormat format);

	void zakoopt$push(long src, int count);
}
