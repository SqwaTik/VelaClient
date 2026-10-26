// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.entity;

public interface CachedRenderer {
	Object zakoopt$renderer();

	Object zakoopt$rendererMap();

	void zakoopt$setRenderer(Object renderer, Object map);
}
