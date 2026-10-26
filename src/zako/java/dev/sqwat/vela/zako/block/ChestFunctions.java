// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.block;

// a chest's lid and pair-light functions for the current tick; they only tie the two halves together, the partial tick is
// applied to them every frame
public interface ChestFunctions {
	Object zakoopt$openFn(long tick);

	Object zakoopt$lightFn(long tick);

	void zakoopt$setOpenFn(Object fn, long tick);

	void zakoopt$setLightFn(Object fn, long tick);
}
