package dev.sqwat.vela.opt.feature.particles;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "particle_lod",
	visual = true,
	group = FeatureGroup.PARTICLES,
	mixins = {"particle.QuadParticleGroupMixin", "particle.ParticleEngineLodMixin"}
)
public class ParticleLodFeature extends Feature {
}
