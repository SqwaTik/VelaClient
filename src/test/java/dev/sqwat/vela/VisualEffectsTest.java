package dev.sqwat.vela;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VisualEffectsTest {
    @Test void killEffectExposesModeAndDuration(){
        var effect=new dev.sqwat.vela.module.visual.KillEffect();
        assertEquals(2,effect.settings().size());
        var mode=(dev.sqwat.vela.module.setting.ChoiceSetting)effect.settings().getFirst();
        assertEquals(java.util.List.of("Луч","Растворение"),mode.options());
        var duration=(dev.sqwat.vela.module.setting.NumberSetting)effect.settings().get(1);
        assertEquals("duration",duration.id());
        assertEquals(1_100_000_000L,effect.lifetimeNanos());
        duration.set(0d);assertEquals(300_000_000L,effect.lifetimeNanos());
        duration.set(10d);assertEquals(5_000_000_000L,effect.lifetimeNanos());
    }
    @Test void dashTrailOffersParticlesAndContinuousModes(){
        var trail=new dev.sqwat.vela.module.visual.DashTrail();
        var mode=(dev.sqwat.vela.module.setting.ChoiceSetting)trail.settings().stream().filter(s->s.id().equals("mode")).findFirst().orElseThrow();
        assertEquals(java.util.List.of("Частицы","Лента","Спираль"),mode.options());
        assertEquals("Частицы",mode.value());
    }
    @Test void killEffectHasBoundedLifetime(){
        assertEquals(0,dev.sqwat.vela.module.visual.KillEffect.opacity(-.1));
        assertTrue(dev.sqwat.vela.module.visual.KillEffect.opacity(.15)>.5);
        assertEquals(0,dev.sqwat.vela.module.visual.KillEffect.opacity(1));
        assertEquals(0,dev.sqwat.vela.module.visual.KillEffect.opacity(2));
    }
    @Test void spriteTexturesAreReadableAndComplete()throws Exception{
        for(int i=1;i<=21;i++)try(var in=getClass().getResourceAsStream("/assets/velaclient/textures/effects/dashcubics/dashcubic"+i+".png")){
            assertNotNull(in);var image=javax.imageio.ImageIO.read(in);assertNotNull(image);
            assertEquals(i<=3?48:i<=16?96:144,image.getWidth());assertEquals(i>=13&&i<=16?96:48,image.getHeight());
        }
        for(String file:new String[]{"target.png","glow.png","near_arrow.png"})try(var in=getClass().getResourceAsStream("/assets/velaclient/textures/effects/"+file)){assertNotNull(in);assertNotNull(javax.imageio.ImageIO.read(in));}
    }
    @Test void friendEffectsDefaultOnAndPersistIndependently(){
        var gson=new com.google.gson.Gson();var cfg=gson.fromJson("{}",dev.sqwat.vela.core.ClientConfig.class);
        assertTrue(cfg.friendVisuals&&cfg.friendChinaHat&&cfg.friendDashTrail&&cfg.friendJumpCircle);
        cfg.friendDashTrail=false;var restored=gson.fromJson(gson.toJson(cfg),dev.sqwat.vela.core.ClientConfig.class);
        assertTrue(restored.friendChinaHat&&restored.friendJumpCircle);assertFalse(restored.friendDashTrail);
    }
}
