package dev.sqwat.vela;

import dev.sqwat.vela.zako.ZakoOptConfig;
import dev.sqwat.vela.zako.feature.Features;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZakoCompatibilityTest {
    @Test void retainedImplementationsObserveTheSameRuntimeSwitch(){
        boolean original=Features.PARTICLE_PHYSICS.on;
        try {
            Features.PARTICLE_PHYSICS.on=true;assertTrue(ZakoOptConfig.particlePhysics());
            Features.PARTICLE_PHYSICS.on=false;assertFalse(ZakoOptConfig.particlePhysics());
        } finally { Features.PARTICLE_PHYSICS.on=original; }
    }
    @Test void retainedImplementationsReadTheValidatedDistance(){
        int original=Features.SPAWNER_CULL.distance;
        try {Features.SPAWNER_CULL.distance=48;assertEquals(48,ZakoOptConfig.spawnerDistance());}
        finally {Features.SPAWNER_CULL.distance=original;}
    }
}
