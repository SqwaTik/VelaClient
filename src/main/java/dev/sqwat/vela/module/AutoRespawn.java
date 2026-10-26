package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

/** Presses the normal respawn action after an optional delay, exactly as the death screen button does. */
public final class AutoRespawn extends ClientModule {
    private final NumberSetting delay=setting(new NumberSetting("delay","Задержка","Пауза перед возрождением, мс",0,0,3000,100));
    private long deathAt=-1;
    public AutoRespawn(){super("auto_respawn","Auto Respawn","Автоматическое возрождение после смерти",ModuleCategory.UTILITIES);}
    @Override protected void onDisable(){deathAt=-1;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||!mc.player.isDeadOrDying()||!(mc.gui.screen() instanceof DeathScreen)){deathAt=-1;return;}
        long now=System.nanoTime();
        if(deathAt<0)deathAt=now;
        if(now-deathAt<delay.value().longValue()*1_000_000L)return;
        mc.player.respawn();
        mc.gui.setScreen(null);
        deathAt=-1;
    }
}
