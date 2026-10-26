// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.entity;

import net.minecraft.world.entity.Entity;

public interface StateEntity {
	Entity zakoopt$entity();

	void zakoopt$entity(Entity entity);
}
