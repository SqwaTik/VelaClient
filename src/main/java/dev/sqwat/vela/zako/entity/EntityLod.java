// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.entity;

import dev.sqwat.vela.zako.feature.Features;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EntityLod {

	public boolean far(double distanceSq) {
		if (!Features.ENTITY_LOD.on) {
			return false;
		}
		double d = Features.ENTITY_LOD.distance;
		return distanceSq > d * d;
	}

	public boolean farPlayer(double distanceSq) {
		double d = Features.PLAYER_LOD.distance;
		return Features.ENTITY_LOD.on && Features.PLAYER_LOD.on && distanceSq > d * d;
	}
}
