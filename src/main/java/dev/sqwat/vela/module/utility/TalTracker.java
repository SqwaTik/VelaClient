package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Local chat line when a player's Totem of Undying pops (entity event 35), with a running count per player and whether
 * the popped totem was enchanted. Counts reset when that player dies or leaves, and on world change.
 * Uses only the event the server already broadcasts to everyone nearby; nothing is sent.
 */
public final class TalTracker extends ClientModule {
    private final BooleanSetting self=setting(new BooleanSetting("self","Свои тотемы","Сообщать и о своих тотемах",false));
    private final BooleanSetting deaths=setting(new BooleanSetting("deaths","Итог при смерти","Сколько тотемов игрок потерял до смерти",true));
    private final Map<UUID,Integer> pops=new HashMap<>();
    private final Map<UUID,String> names=new HashMap<>();
    private Object level;
    public TalTracker(){super("tal_tracker","Tal Tracker","Сообщает в чат, когда у игрока лопается тотем",ModuleCategory.UTILITIES);}
    @Override protected void onDisable(){pops.clear();names.clear();level=null;}

    public static void popped(Entity entity){
        var s=dev.sqwat.vela.VelaClient.services();
        if(s!=null&&s.modules().byId("tal_tracker") instanceof TalTracker module&&module.enabled())module.record(entity);
    }
    private void record(Entity entity){
        var mc=Minecraft.getInstance();
        if(!(entity instanceof Player player)||mc.level==null)return;
        if(level!=mc.level){pops.clear();names.clear();level=mc.level;}
        if(player==mc.player&&!self.value())return;
        int count=pops.merge(player.getUUID(),1,Integer::sum);
        String name=player.getGameProfile().name();names.put(player.getUUID(),name);
        // The totem is still in hand when the pop event arrives; the equipment update follows separately.
        var held=player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)?player.getMainHandItem():player.getOffhandItem();
        boolean enchanted=held.is(Items.TOTEM_OF_UNDYING)&&held.isEnchanted();
        boolean friend=dev.sqwat.vela.VelaClient.services().friends().contains(name);
        var line=Component.literal(name).withStyle(st->st.withColor(friend?0x5BE38A:0xFFFFFF))
            .append(Component.literal(dev.sqwat.vela.ui.Lang.tr(" потерял тотем ")).withStyle(st->st.withColor(0xB9C2D6)))
            .append(Component.literal("#"+count).withStyle(st->st.withColor(0xFF5A6E)));
        if(enchanted)line.append(Component.literal(dev.sqwat.vela.ui.Lang.tr(" (зачарованный)")).withStyle(st->st.withColor(0xC08CFF)));
        mc.gui.hud.getChat().addClientSystemMessage(line);
    }
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.level==null){onDisable();return;}
        if(level!=mc.level){pops.clear();names.clear();level=mc.level;return;}
        if(pops.isEmpty())return;
        var iterator=pops.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();
            var player=mc.level.getPlayerByUUID(entry.getKey());
            if(player!=null&&!player.isDeadOrDying())continue;
            if(player!=null&&deaths.value()){
                String name=names.getOrDefault(entry.getKey(),player.getGameProfile().name());
                mc.gui.hud.getChat().addClientSystemMessage(Component.literal(name).withStyle(st->st.withColor(0xFFFFFF))
                    .append(Component.literal(dev.sqwat.vela.ui.Lang.tr(" погиб, потеряв тотемов: ")).withStyle(st->st.withColor(0xB9C2D6)))
                    .append(Component.literal(Integer.toString(entry.getValue())).withStyle(st->st.withColor(0xFF5A6E))));
            }
            names.remove(entry.getKey());iterator.remove();
        }
    }
}
