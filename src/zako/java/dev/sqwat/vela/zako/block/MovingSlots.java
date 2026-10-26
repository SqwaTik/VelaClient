// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.block;

// a moving piston's two moving-block states (the moved block and, for a retracting piston, its base), kept between frames
public interface MovingSlots {
	Object zakoopt$moving(int slot);

	void zakoopt$setMoving(int slot, Object state);
}
