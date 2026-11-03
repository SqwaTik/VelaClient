package dev.sqwat.vela.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Anti-ESP plugins strip the equipment of far players, so from high above a player shows up without armour.
 * The armour last seen up close is kept and put back on while the server sends empty slots from afar.
 */
public final class ArmorMemory {
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final double NEAR_SQ=24*24;
    private static final long KEEP_MS=10*60_000L;
    private static final class Known{final ItemStack[] armor;long time;Known(ItemStack[] armor,long time){this.armor=armor;this.time=time;}}
    private static final Map<UUID,Known> KNOWN=new HashMap<>();

    private ArmorMemory(){}

    private static void remember(Known known,int slot,ItemStack worn){
        if(!ItemStack.isSameItemSameComponents(worn,known.armor[slot]))known.armor[slot]=worn.copy();
    }

    public static void tick(Minecraft mc){
        var level=mc.level;
        var self=mc.player;
        if(level==null||self==null){KNOWN.clear();return;}
        long now=System.currentTimeMillis();
        for(Player player:level.players()){
            if(player==self||player.isRemoved())continue;
            Known known=KNOWN.get(player.getUUID());
            if(player.distanceToSqr(self)<=NEAR_SQ){
                if(known==null){
                    ItemStack[] armor=new ItemStack[SLOTS.length];
                    for(int i=0;i<SLOTS.length;i++)armor[i]=player.getItemBySlot(SLOTS[i]).copy();
                    KNOWN.put(player.getUUID(),new Known(armor,now));
                    continue;
                }
                // Up close the server is trusted, including armour that was taken off.
                for(int i=0;i<SLOTS.length;i++)remember(known,i,player.getItemBySlot(SLOTS[i]));
                known.time=now;
                continue;
            }
            if(known==null)continue;
            if(now-known.time>KEEP_MS){KNOWN.remove(player.getUUID());continue;}
            for(int i=0;i<SLOTS.length;i++){
                ItemStack worn=player.getItemBySlot(SLOTS[i]);
                if(worn.isEmpty()){
                    if(!known.armor[i].isEmpty())player.setItemSlot(SLOTS[i],known.armor[i].copy());
                }else remember(known,i,worn);
            }
        }
    }
}
