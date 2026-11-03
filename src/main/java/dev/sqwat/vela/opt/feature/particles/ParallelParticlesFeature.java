package dev.sqwat.vela.opt.feature.particles;

import dev.sqwat.vela.opt.OptConfig;
import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Features;

// -Dvelaopt.parallelparticles=true forces it below 4 CPU threads, =auto follows the thread count like the switch does
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
		return OptConfig.CORES < 4 ? "velaopt.blocked.cpu" : null;
	}

	@Override
	public void refresh() {
		String p = System.getProperty(property());
		on = Features.active && (p == null ? value && blocked() == null : p.equals("true") || p.equals("auto") && blocked() == null);
		vertices = on && flag("parallelvertices", true);
	}
}
