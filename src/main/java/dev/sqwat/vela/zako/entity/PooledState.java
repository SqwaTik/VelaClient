// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.entity;

public interface PooledState {
	Object zakoopt$state();

	Object zakoopt$stateOwner();

	void zakoopt$setState(Object state, Object owner);
}
