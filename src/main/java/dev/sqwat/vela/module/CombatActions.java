package dev.sqwat.vela.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;

/** Shared ordinary gameplay actions; a single owner may interact in a player tick. */
final class CombatActions {
    private static Object playerIdentity;
    private static int actionTick=-1;
    static boolean ready(Minecraft c) {
        return c.player!=null&&c.level!=null&&c.gameMode!=null&&c.gui.screen()==null&&c.gui.overlay()==null
            &&c.isWindowActive()&&!c.isPaused()&&c.player.isAlive()&&!c.player.isSpectator()
            &&!c.player.isUsingItem()&&!c.gameMode.isDestroying();
    }
    static boolean claim(Minecraft c) {
        if(!ready(c))return false;
        if(playerIdentity!=c.player){playerIdentity=c.player;actionTick=-1;}
        if(actionTick==c.player.tickCount)return false;
        actionTick=c.player.tickCount;return true;
    }
    static boolean eligible(Minecraft c,LivingEntity entity,boolean players,boolean mobs,boolean invisible) {
        var services=dev.sqwat.vela.VelaClient.services();
        return entity!=c.player&&entity.isAlive()&&!(entity instanceof ArmorStand)
            &&(!entity.isInvisible()||invisible)&&!(entity instanceof Player p&&(p.isSpectator()||p.isCreative()))
            &&(entity instanceof Player?players:mobs)
            &&(services==null||!services.friends().contains(entity.getName().getString()));
    }
    static LivingEntity nearest(Minecraft c,double range) {
        return c.level.getEntitiesOfClass(LivingEntity.class,c.player.getBoundingBox().inflate(range),
            e->eligible(c,e,true,false,false)&&c.player.distanceToSqr(e)<=range*range&&c.player.hasLineOfSight(e))
            .stream().min(Comparator.comparingDouble(c.player::distanceToSqr)).orElse(null);
    }
    static int hotbar(Minecraft c,Item item) {
        for(int i=0;i<9;i++)if(c.player.getInventory().getItem(i).is(item))return i;
        return -1;
    }
    static void face(Minecraft c,Vec3 point,boolean smooth) {
        var d=point.subtract(c.player.getEyePosition());
        float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));
        c.player.setYRot(c.player.getYRot()+net.minecraft.util.Mth.wrapDegrees(yaw-c.player.getYRot())*(smooth?.35f:1));
        c.player.setXRot(c.player.getXRot()+(pitch-c.player.getXRot())*(smooth?.35f:1));
    }
    static boolean useHotbar(Minecraft c,int slot) {
        if(slot<0||slot>=9||c.player.getCooldowns().isOnCooldown(c.player.getInventory().getItem(slot))||!claim(c))return false;
        int selected=c.player.getInventory().getSelectedSlot();
        try{c.player.getInventory().setSelectedSlot(slot);c.gameMode.useItem(c.player,InteractionHand.MAIN_HAND);}
        finally{c.player.getInventory().setSelectedSlot(selected);((dev.sqwat.vela.mixin.CarriedItemSync)c.gameMode).vela$syncSlot();}
        return true;
    }
}
