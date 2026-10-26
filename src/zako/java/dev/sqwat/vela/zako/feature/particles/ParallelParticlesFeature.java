// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature.particles;

import dev.sqwat.vela.zako.ZakoOptConfig;
import dev.sqwat.vela.zako.bench.BenchView;
import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.feature.Features;

// -Dzakoopt.parallelparticles=true forces it below 4 CPU threads, =auto follows the thread count like the switch does
@FeatureData(
	key = "parallel_particles",
	group = FeatureGroup.PARTICLES,
	mixins = {"particle.QuadParticleGroupMixin", "particle.ParticleEngineParallelMixin"},
	bench = BenchView.HOLOGRAMS
)
public class ParallelParticlesFeature extends Feature {
	public boolean vertices;

	@Override
	public String blocked() {
		return ZakoOptConfig.CORES < 4 ? "zakoopt.blocked.cpu" : null;
	}

	@Override
	public void refresh() {
		String p = System.getProperty(property());
		on = Features.active && available() && !broken && (p == null ? value && blocked() == null : p.equals("true") || p.equals("auto") && blocked() == null);
		vertices = on && flag("parallelvertices", true);
	}
}
