package dev.sqwat.vela.opt.feature.particles;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "particle_light",
	group = FeatureGroup.PARTICLES,
	mixins = "particle.ParticleMixin"
)
public class ParticleLightFeature extends Feature {
}
