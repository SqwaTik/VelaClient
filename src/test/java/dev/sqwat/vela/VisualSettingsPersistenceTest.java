package dev.sqwat.vela;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ChoiceSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class VisualSettingsPersistenceTest {
    @TempDir Path root;
    @Test void combinedShaderSettingsPersist(){
        try(var services=new VelaServices(root)){
            var shader=(dev.sqwat.vela.module.visual.Shaders)services.modules().byId("shaders");
            for(String id:java.util.List.of("sky","hands","wet"))
                ((BooleanSetting)shader.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow()).set(true);
            ((ChoiceSetting)shader.settings().stream().filter(s->s.id().equals("hand_mode")).findFirst().orElseThrow()).set("Пламя");
            ((ChoiceSetting)shader.settings().stream().filter(s->s.id().equals("sky_mode")).findFirst().orElseThrow()).set("Чёрная дыра");
            ((ChoiceSetting)shader.settings().stream().filter(s->s.id().equals("wet_quality")).findFirst().orElseThrow()).set("Низкое");
            shader.skyCode(dev.sqwat.vela.render.shader.CustomSkySource.DEFAULT+"\n// saved draft");
        }
        try(var services=new VelaServices(root)){
            var shader=(dev.sqwat.vela.module.visual.Shaders)services.modules().byId("shaders");
            assertTrue(shader.skyActive());assertTrue(shader.handsActive());assertTrue(shader.wetActive());
            assertEquals("Пламя",shader.handMode());assertEquals("Чёрная дыра",shader.skyMode());
            assertEquals(12,shader.reflectionSteps());assertTrue(shader.skyCode().endsWith("// saved draft"));
            assertFalse(shader.enabled());
        }
    }
    @Test void shaderFogSettingsPersist(){
        try(var services=new VelaServices(root)){
            var shader=services.modules().byId("shaders");shader.setEnabled(true);
            ((dev.sqwat.vela.module.setting.NumberSetting)shader.settings().stream().filter(s->s.id().equals("fog_height")).findFirst().orElseThrow()).set(72d);
            ((BooleanSetting)shader.settings().stream().filter(s->s.id().equals("wet")).findFirst().orElseThrow()).set(true);
            ((ChoiceSetting)shader.settings().stream().filter(s->s.id().equals("hand_mode")).findFirst().orElseThrow()).set("Стекло");
            ((dev.sqwat.vela.module.visual.Shaders)shader).skyCode("void mainImage(out vec4 c, in vec2 p){c=vec4(1.0);}");
        }
        try(var services=new VelaServices(root)){
            var shader=(dev.sqwat.vela.module.visual.Shaders)services.modules().byId("shaders");
            assertTrue(shader.enabled());assertEquals(72,shader.fogHeight());
            assertTrue(shader.wetActive());assertEquals("Стекло",shader.handMode());
            assertEquals("void mainImage(out vec4 c, in vec2 p){c=vec4(1.0);}",shader.skyCode());
        }
    }
    @Test void skyColorAndAnimationElementsPersist(){
        try(var services=new VelaServices(root)){
            var ambience=services.modules().byId("ambience");
            ((BooleanSetting)ambience.settings().stream().filter(s->s.id().equals("sky")).findFirst().orElseThrow()).set(true);
            ((BooleanSetting)ambience.settings().stream().filter(s->s.id().equals("sky_client_color")).findFirst().orElseThrow()).set(false);
            ((dev.sqwat.vela.module.setting.ColorSetting)ambience.settings().stream().filter(s->s.id().equals("sky_color")).findFirst().orElseThrow()).set(0x123456);
            var animations=services.modules().byId("animations");animations.setEnabled(true);
            ((BooleanSetting)animations.settings().stream().filter(s->s.id().equals("tab")).findFirst().orElseThrow()).set(false);
        }
        try(var services=new VelaServices(root)){
            var ambience=services.modules().byId("ambience");
            assertEquals(true,ambience.settings().stream().filter(s->s.id().equals("sky")).findFirst().orElseThrow().value());
            assertEquals(false,ambience.settings().stream().filter(s->s.id().equals("sky_client_color")).findFirst().orElseThrow().value());
            assertEquals(0x123456,ambience.settings().stream().filter(s->s.id().equals("sky_color")).findFirst().orElseThrow().value());
            var animations=services.modules().byId("animations");assertTrue(animations.enabled());
            assertEquals(false,animations.settings().stream().filter(s->s.id().equals("tab")).findFirst().orElseThrow().value());
        }
    }
    @Test void crosshairDynamicsAndHitColorPersist(){
        try(var services=new VelaServices(root)){
            var cross=services.modules().byId("crosshair");
            ((BooleanSetting)cross.settings().stream().filter(s->s.id().equals("dynamic")).findFirst().orElseThrow()).set(false);
            ((ChoiceSetting)cross.settings().stream().filter(s->s.id().equals("mode")).findFirst().orElseThrow()).set("Круг");
            services.modules().byId("hit_color").setEnabled(true);
        }
        try(var services=new VelaServices(root)){
            assertEquals(false,services.modules().byId("crosshair").settings().stream().filter(s->s.id().equals("dynamic")).findFirst().orElseThrow().value());
            assertEquals("Круг",services.modules().byId("crosshair").settings().stream().filter(s->s.id().equals("mode")).findFirst().orElseThrow().value());
            assertTrue(services.modules().byId("hit_color").enabled());
        }
    }
    @Test void newVisualCameraOptionAndAlwaysVisibleBindsPersist(){
        try(var services=new VelaServices(root)){
            var particles=services.modules().byId("particles");assertNotNull(particles);assertFalse(particles.enabled());particles.setEnabled(true);
            assertTrue(particles.settings().stream().noneMatch(s->s.id().equals("mode")));
            ((ChoiceSetting)particles.settings().stream().filter(s->s.id().equals("shape")).findFirst().orElseThrow()).set("Звезда");
            for(String id:java.util.List.of("hits","jumps","landings","deaths","totems")){
                var flag=(BooleanSetting)particles.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();
                assertTrue(flag.value());flag.set(false);
            }
            ((BooleanSetting)services.modules().byId("motion_blur").settings().stream().filter(s->s.id().equals("third_person")).findFirst().orElseThrow()).set(false);
            services.configs().current().alwaysShowBinds=true;
            services.configs().current().friendVisualsExpanded=true;
        }
        try(var services=new VelaServices(root)){
            var particles=services.modules().byId("particles");assertTrue(particles.enabled());
            assertEquals("Звезда",particles.settings().stream().filter(s->s.id().equals("shape")).findFirst().orElseThrow().value());
            for(String id:java.util.List.of("hits","jumps","landings","deaths","totems"))assertEquals(false,particles.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow().value());
            assertEquals(false,services.modules().byId("motion_blur").settings().stream().filter(s->s.id().equals("third_person")).findFirst().orElseThrow().value());
            assertTrue(services.configs().current().alwaysShowBinds);assertTrue(services.configs().current().friendVisualsExpanded);
        }
    }
}
