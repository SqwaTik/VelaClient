// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.block;

public interface CachedLight {
	int zakoopt$light();

	long zakoopt$lightTick();

	void zakoopt$setLight(int light, long tick);

	int zakoopt$pairLight();

	long zakoopt$pairLightTick();

	void zakoopt$setPairLight(int light, long tick);
}
