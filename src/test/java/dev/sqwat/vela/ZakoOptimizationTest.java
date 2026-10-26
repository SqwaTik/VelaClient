package dev.sqwat.vela;
import dev.sqwat.vela.module.ZakoOptimization;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ZakoOptimizationTest {
    @Test @SuppressWarnings("unchecked") void unchangedFramesDoNotReapplySettings() throws Exception {
        var module=new ZakoOptimization();
        var field=ZakoOptimization.class.getDeclaredField("bindings");field.setAccessible(true);
        int[] writes={0};
        ((java.util.List<Runnable>)field.get(module)).add(()->writes[0]++);
        module.values();
        for(int i=0;i<1000;i++)module.values();
        assertEquals(1,writes[0]);
        module.entityDistance.set(48.0);module.values();
        assertEquals(2,writes[0]);
        module.entityDistance.reset();module.values();
        assertEquals(3,writes[0]);
    }
    @Test void switchingOwnersRestoresSharedFeatureSettings(){
        var first=new ZakoOptimization();var second=new ZakoOptimization();
        first.entityDistance.set(48.0);second.entityDistance.set(24.0);
        first.values();second.values();first.values();
        assertEquals(48,dev.sqwat.vela.zako.feature.Features.ENTITY_LOD.distance);
    }
    @Test void registryReadsExistingSettingsWithoutDuplicateControls(){
        var module=new ZakoOptimization();
        module.entityLod.set(false);module.entityDistance.set(63.0);
        module.values();
        assertFalse(dev.sqwat.vela.zako.feature.Features.ENTITY_LOD.value);
        assertEquals(63,dev.sqwat.vela.zako.feature.Features.ENTITY_LOD.distance);
        assertEquals(module.settings().size(),module.settings().stream().map(s->s.id()).distinct().count());
    }
    @Test void newFeatureDefaultsDoNotLeakFromAnotherProfile(){
        var module=new ZakoOptimization();
        var scale=(dev.sqwat.vela.module.setting.NumberSetting)module.settings().stream().filter(s->s.id().equals("render_scale_percent")).findFirst().orElseThrow();
        scale.set(89.0);module.values();
        var fresh=new ZakoOptimization();
        assertEquals(75.0,fresh.settings().stream().filter(s->s.id().equals("render_scale_percent")).findFirst().orElseThrow().value());
    }
    @Test void everyOptimizationHasTranslatedLabel(){
        var module=new ZakoOptimization();
        for(var setting:module.settings())assertNotEquals(setting.id(),setting.name(),setting.id());
    }
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path root;
    @Test void settingsPersistThroughExistingProfiles(){
        try(var services=new dev.sqwat.vela.core.VelaServices(root)){
            var m=services.zakoOptimization();assertNull(services.modules().byId("zako_optimization"));
            m.entityDistance.set(48.0);m.playerLod.set(false);m.singleCopy.set(true);
        }
        try(var services=new dev.sqwat.vela.core.VelaServices(root)){
            var m=services.zakoOptimization();assertTrue(m.available());assertEquals(48.0,m.entityDistance.value());
            assertFalse(m.playerLod.value());assertTrue(m.singleCopy.value());
        }
    }
    @Test void incompatibleExternalOptimizerPreservesLayers(){var m=new ZakoOptimization();m.require(()->false);assertFalse(m.reduceLayers(false,100000));}
    @Test void playerAndEntityControlsAreIndependent(){
        var m=new ZakoOptimization();
        assertFalse(m.reduceLayers(false,35*35));assertTrue(m.reduceLayers(false,36*36));
        m.playerLod.set(false);assertFalse(m.reduceLayers(true,100000));assertTrue(m.reduceLayers(false,100000));
        m.entityLod.set(false);assertFalse(m.reduceLayers(false,100000));
        m.require(()->false);assertFalse(m.reduceLayers(true,100000));
    }
    @Test void distancesClampAndRemainSquared(){var m=new ZakoOptimization();m.entityDistance.set(-1.0);assertEquals(8.0,m.entityDistance.value());m.spawnerDistance.set(999.0);assertEquals(64.0,m.spawnerDistance.value());assertFalse(ZakoOptimization.beyond(256,16));assertTrue(ZakoOptimization.beyond(257,16));}
}
