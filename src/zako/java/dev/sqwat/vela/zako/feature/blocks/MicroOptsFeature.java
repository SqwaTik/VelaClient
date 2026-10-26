// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature.blocks;

import dev.sqwat.vela.zako.bench.BenchView;
import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;

// one switch for the player; each part keeps its own -Dzakoopt.micro_opts.<part> switch for finding a culprit
@FeatureData(
	key = "micro_opts",
	group = FeatureGroup.BLOCKS,
	bench = BenchView.OVERVIEW
)
public class MicroOptsFeature extends Feature {
	// results that do not change: the light of a chest pair, the submit order, text that stays the same
	public boolean stableResults;
	// checks repeated for nothing: block entity distance once a tick, a moving piston's shape, the upside-down name, the team
	// lookup for visible entities, the parrot variant, the biome's ambient particles, chat lines deferred to one batch
	public boolean fewerChecks;
	// the same texture buffer is not re-attached for every draw (clouds and the like)
	public boolean texBufferOnce;
	// one culling box shared by identical items
	public boolean sharedItemBounds;
	// 1.21.4: model cubes built the fast way
	public boolean fastCubes;
	// 1.21.4: a spawner's mob drawn once, then replayed
	public boolean spawnerMobMesh;

	@Override
	public void refresh() {
		super.refresh();
		stableResults = on && flag("micro_opts.stable_results", true);
		fewerChecks = on && flag("micro_opts.fewer_checks", true);
		texBufferOnce = on && flag("micro_opts.tex_buffer_once", true);
		sharedItemBounds = on && flag("micro_opts.shared_item_bounds", true);
		fastCubes = on && flag("micro_opts.fast_cubes", true);
		spawnerMobMesh = on && flag("micro_opts.spawner_mob_mesh", true);
	}
}
