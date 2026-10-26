package dev.sqwat.vela;
import dev.sqwat.vela.module.Shaders;
import dev.sqwat.vela.module.setting.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ShaderFogTest {
    @Test void skyRasterWorkIsBoundedAcrossWindowSizes(){
        for(int[] size:new int[][]{{1,1},{1280,720},{1920,1080},{2560,1440},{3840,2160},{7680,4320},{1080,1920}}){
            int divisor=dev.sqwat.vela.render.SkyShaderRenderer.resolutionDivisor(size[0],size[1]);
            assertTrue(divisor>=2);
            assertTrue(Math.max(1,size[0]/divisor)<=960);
            assertTrue(Math.max(1,size[1]/divisor)<=540);
        }
    }
    @Test void skyQualityIsNotExposedAndEffectsRemainOptIn(){
        var shader=new Shaders();assertFalse(shader.enabled());assertFalse(shader.skyActive());
        assertTrue(shader.settings().stream().noneMatch(s->s.id().equals("sky_quality")));
    }
    @Test void wetDefaultsAndQualityAreBounded(){
        var shader=new Shaders();assertFalse(shader.wetActive());
        assertEquals(.7f,shader.reflection(),.00001f);assertEquals(.6f,shader.wetness(),.00001f);
        assertEquals(24,shader.reflectionSteps());assertEquals(2,shader.reflectionResolutionDivisor());
        var wet=(BooleanSetting)shader.settings().stream().filter(s->s.id().equals("wet")).findFirst().orElseThrow();
        var quality=(ChoiceSetting)shader.settings().stream().filter(s->s.id().equals("wet_quality")).findFirst().orElseThrow();
        wet.set(true);assertTrue(shader.wetActive());
        quality.set("Низкое");assertEquals(14,shader.reflectionSteps());assertEquals(3,shader.reflectionResolutionDivisor());
        quality.set("Высокое");assertEquals(40,shader.reflectionSteps());assertEquals(1,shader.reflectionResolutionDivisor());
        ((NumberSetting)shader.settings().stream().filter(s->s.id().equals("wet_reflection")).findFirst().orElseThrow()).set(0d);
        assertTrue(shader.wetActive());
        ((NumberSetting)shader.settings().stream().filter(s->s.id().equals("wet_amount")).findFirst().orElseThrow()).set(0d);
        assertFalse(shader.wetActive());
    }
    @Test void fogDefaultsMatchReferenceAndRemainOptIn(){
        var shader=new Shaders();assertFalse(shader.enabled());assertTrue(shader.fogActive());
        assertEquals(.0088f,shader.fogDensity(),.000001f);
        assertEquals(35,shader.fogHeight());assertEquals(.0315f,shader.fogFalloff(),.000001f);
        assertEquals(.5f,shader.fogHaze());
        ((NumberSetting)shader.settings().stream().filter(s->s.id().equals("fog_density")).findFirst().orElseThrow()).set(0d);
        assertFalse(shader.fogActive());assertEquals(0,shader.fogDensity());
    }
    @Test void customFogColorDoesNotChangeTheme(){
        var shader=new Shaders();int theme=dev.sqwat.vela.ui.render.VelaTheme.PRIMARY;
        ((BooleanSetting)shader.settings().stream().filter(s->s.id().equals("fog_theme")).findFirst().orElseThrow()).set(false);
        ((ColorSetting)shader.settings().stream().filter(s->s.id().equals("fog_color")).findFirst().orElseThrow()).set(0x123456);
        assertEquals(0x123456,shader.fogColor());assertEquals(theme,dev.sqwat.vela.ui.render.VelaTheme.PRIMARY);
    }
}
