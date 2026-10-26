package dev.sqwat.vela.module;

import net.minecraft.client.Minecraft;

/** Holds the vanilla sprint input; vanilla owns sprint eligibility and resets. */
public final class AutoSprint extends ClientModule {
    private boolean held;
    public AutoSprint(){super("autosprint","AutoSprint","Автоматическое нажатие клавиши спринта",ModuleCategory.UTILITIES);}
    @Override public void onClientTick(){
        var c=Minecraft.getInstance();
        if(c.player==null||c.level==null||c.gui.screen()!=null||c.gui.overlay()!=null||!c.isWindowActive()||c.isPaused()||!c.player.isAlive()){release();return;}
        c.options.keySprint.setDown(true);held=true;
    }
    @Override protected void onDisable(){release();}
    private void release(){
        if(!held)return;
        var c=Minecraft.getInstance();
        c.options.keySprint.setDown(false);
        held=false;
    }
}
