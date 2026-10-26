package dev.sqwat.vela;
import dev.sqwat.vela.module.Far;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.render.FrameSamples;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FarAndPerformanceTest {
    @Test void farIsOptInWithBoundedDistance(){var f=new Far();assertFalse(f.enabled());var s=(NumberSetting)f.settings().getFirst();s.set(100d);assertEquals(64d,s.value());s.set(0d);assertEquals(4d,s.value());}
    @Test void framePercentilesAndBoundedStorage(){var f=new FrameSamples(100);for(int i=1;i<=100;i++)f.add(i);var s=f.summary();assertEquals(100,s.frames());assertEquals(50.5,s.averageMs());assertEquals(95,s.p95Ms());assertEquals(99,s.p99Ms());for(int i=0;i<100;i++)f.add(2);assertEquals(2,f.summary().p99Ms());}
    @Test void invalidFrameValuesAreIgnored(){var f=new FrameSamples(3);f.add(Double.NaN);f.add(Double.POSITIVE_INFINITY);f.add(-1);f.add(0);assertEquals(0,f.summary().frames());}
    @Test void rareStallsAreNotHiddenByPercentiles(){var f=new FrameSamples(1000);for(int i=0;i<999;i++)f.add(1);f.add(80);assertEquals(1,f.summary().p99Ms());assertEquals(80,f.summary().maxMs());assertEquals(1,f.summary().over16Ms());}
}
