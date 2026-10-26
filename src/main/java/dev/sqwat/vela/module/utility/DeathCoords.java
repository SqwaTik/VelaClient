package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/** Local chat line with the death position; nothing is sent to the server. Click copies the coordinates. */
public final class DeathCoords extends ClientModule {
    private Object player;
    private boolean dead;
    public DeathCoords(){super("death_coords","Death Coords","Координаты смерти в чате",ModuleCategory.UTILITIES);}
    @Override protected void onDisable(){player=null;dead=false;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){player=null;dead=false;return;}
        if(player!=mc.player){player=mc.player;dead=mc.player.isDeadOrDying();return;}
        boolean now=mc.player.isDeadOrDying();
        if(now&&!dead)announce(mc);
        dead=now;
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
