package dev.sqwat.vela;

import dev.sqwat.vela.zako.feature.Features;
import dev.sqwat.vela.zako.feature.MicroOptsFeature;
import dev.sqwat.vela.zako.feature.ParallelParticlesFeature;
import dev.sqwat.vela.zako.feature.ImmediateRingFeature;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZakoFailureGateTest {
    @Test void brokenFeaturesCannotBeForcedBackOn() {
        boolean previous=Features.active;
        Features.active=true;
        try {
            var ring=new ImmediateRingFeature();ring.force=true;ring.broken=true;ring.refresh();
            assertFalse(ring.on);
            var particles=new ParallelParticlesFeature();particles.broken=true;particles.refresh();
            assertFalse(particles.on);assertFalse(particles.vertices);
            var micro=new MicroOptsFeature();micro.broken=true;micro.refresh();
            assertFalse(micro.stableResults);assertFalse(micro.fewerChecks);
            assertFalse(micro.texBufferOnce);assertFalse(micro.sharedItemBounds);
            assertFalse(micro.fastCubes);assertFalse(micro.spawnerMobMesh);
        } finally {Features.active=previous;}
    }
    @Test void disabledMicroSwitchDisablesItsParts() {
        boolean previous=Features.active;Features.active=true;
        try {
            var micro=new MicroOptsFeature();micro.value=false;micro.refresh();
            assertFalse(micro.on);assertFalse(micro.fastCubes);assertFalse(micro.spawnerMobMesh);
        } finally {Features.active=previous;}
    }
}
