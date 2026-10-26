// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.gl;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import lombok.experimental.UtilityClass;

// buffer attached to each texel-buffer texture; render thread only
@UtilityClass
public class TexBufferCache {
	private final Int2LongOpenHashMap ATTACHED = new Int2LongOpenHashMap();
	public int boundTexture;

	public boolean attached(int format, int buffer) {
		return boundTexture > 0 && buffer > 0 && ATTACHED.getOrDefault(boundTexture, -1L) == key(format, buffer);
	}

	public void attach(int format, int buffer) {
		if (boundTexture > 0 && buffer > 0) ATTACHED.put(boundTexture, key(format, buffer));
	}

	public void forget() {
		ATTACHED.clear();
		boundTexture = 0;
	}

	private long key(int format, int buffer) {
		return (long) format << 32 | (buffer & 0xFFFFFFFFL);
	}
}
