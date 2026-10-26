package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.ChoiceSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Plays a local UI sound when the player attacks a living entity. Vanilla sounds only. */
public final class HitSound extends ClientModule {
    private final ChoiceSetting sound=setting(new ChoiceSetting("sound","Звук","Звук попадания","Колокольчик",java.util.List.of("Колокольчик","Пианино","Опыт","Аметист","Стрела","Щелчок")));
    private final NumberSetting volume=setting(new NumberSetting("volume","Громкость","Громкость звука",70,5,100,5));
    private final NumberSetting pitch=setting(new NumberSetting("pitch","Тон","Высота звука",100,50,200,5));
    public HitSound(){super("hit_sound","Hit Sound","Звук при ударе по цели",ModuleCategory.UTILITIES);}
    public static void attacked(Entity target){
        var s=dev.sqwat.vela.VelaClient.services();
        if(s==null||!(s.modules().byId("hit_sound") instanceof HitSound module)||!module.enabled()||!(target instanceof LivingEntity living)||!living.isAlive())return;
        SoundEvent event=switch(module.sound.value()){
            case "Пианино"->SoundEvents.NOTE_BLOCK_PLING.value();
            case "Опыт"->SoundEvents.EXPERIENCE_ORB_PICKUP;
            case "Аметист"->SoundEvents.AMETHYST_BLOCK_CHIME;
            case "Стрела"->SoundEvents.ARROW_HIT_PLAYER;
            case "Щелчок"->SoundEvents.UI_BUTTON_CLICK.value();
            default->SoundEvents.NOTE_BLOCK_BELL.value();
        };
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event,module.pitch.value().floatValue()/100,module.volume.value().floatValue()/100));
    }
}
