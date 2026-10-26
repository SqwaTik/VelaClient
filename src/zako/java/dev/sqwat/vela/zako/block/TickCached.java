// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.block;

public interface TickCached {
	Object zakoopt$cached();

	long zakoopt$cachedTick();

	void zakoopt$setCached(Object value, long tick);
}
