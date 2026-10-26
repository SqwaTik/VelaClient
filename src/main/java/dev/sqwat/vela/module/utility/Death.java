package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/**
 * What happens after dying: a local chat line with the death position (click copies it; nothing is sent to the
 * server) and pressing the normal respawn action after an optional delay, exactly as the death screen button does.
 */
public final class Death extends ClientModule {
    private final BooleanSetting coords=setting(new BooleanSetting("coords","Координаты в чат","Место смерти локальным сообщением",true));
    private final BooleanSetting respawn=setting(new BooleanSetting("respawn","Авто-возрождение","Возрождаться без нажатия кнопки",false));
    private final NumberSetting delay=setting(new NumberSetting("delay","Задержка","Пауза перед возрождением, мс",0,0,3000,100));
    private Object player;
    private boolean dead;
    private long deathAt=-1;
    public Death(){super("death","Death","Координаты смерти в чате и авто-возрождение",ModuleCategory.UTILITIES);delay.visibleWhen(respawn::value);}
    @Override protected void onDisable(){player=null;dead=false;deathAt=-1;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){onDisable();return;}
        boolean now=mc.player.isDeadOrDying();
        if(player!=mc.player){player=mc.player;dead=now;}
        else{if(now&&!dead&&coords.value())announce(mc);dead=now;}
        if(!respawn.value()||!now||!(mc.gui.screen() instanceof DeathScreen)){deathAt=-1;return;}
        long time=System.nanoTime();
        if(deathAt<0)deathAt=time;
        if(time-deathAt<delay.value().longValue()*1_000_000L)return;
        mc.player.respawn();
        mc.gui.setScreen(null);
        deathAt=-1;
    }
    private static void announce(Minecraft mc){
        String coords=mc.player.getBlockX()+" "+mc.player.getBlockY()+" "+mc.player.getBlockZ();
        String dimension=mc.level.dimension().identifier().getPath();
        var line=Component.literal("Место смерти: ").withStyle(s->s.withColor(0xB9C2D6))
            .append(Component.literal(coords).withStyle(s->s.withColor(0xFFFFFF).withUnderlined(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(coords))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Скопировать координаты")))))
            .append(Component.literal(" ("+dimension+")").withStyle(s->s.withColor(0x8996B0)));
        mc.gui.hud.getChat().addClientSystemMessage(line);
    }
}
