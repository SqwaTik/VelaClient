package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Health deltas from received entity state, not guessed damage from clicks. */
public final class HpCounter extends ClientModule {
    public static final int LIMIT=48;
    public static final long LIFETIME=1_200_000_000L;
    private record Number(Vec3 position,String text,boolean heal,long born){}
    private final ArrayDeque<Number> numbers=new ArrayDeque<>();
    private final Map<UUID,Float> health=new HashMap<>();
    private Object world;
    private int offset;
    private final BooleanSetting damage=setting(new BooleanSetting("damage","Урон","Показывать потерянное здоровье",true));
    private final BooleanSetting healing=setting(new BooleanSetting("healing","Лечение","Показывать восстановленное здоровье",true));
    private final BooleanSetting mobs=setting(new BooleanSetting("mobs","Мобы","Числа и над мобами, не только над игроками",true));
    private final dev.sqwat.vela.module.setting.NumberSetting scale=setting(new dev.sqwat.vela.module.setting.NumberSetting("scale","Размер","Размер чисел, %",220,80,400,10));
    public HpCounter(){super("hp_counter","HP Counter","Всплывающие числа урона и лечения игроков и мобов",ModuleCategory.VISUALS);}
    private static HpCounter current(net.minecraft.world.entity.LivingEntity player){
        var services=dev.sqwat.vela.VelaClient.services();
        if(services==null)return null;
        if(!(services.modules().byId("hp_counter") instanceof HpCounter module)||!module.enabled())return null;
        var mc=Minecraft.getInstance();
        if(mc==null||!mc.isSameThread()||mc.level==null||mc.player==null||player.level()!=mc.level||player.isRemoved())return null;
        if(!(player instanceof net.minecraft.world.entity.player.Player)&&!module.mobs.value())return null;
        // Health is set while an entity is created, before it has an ID or is in the level: players must be in the
        // level's player list, other entities must have ticked at least once.
        if(player instanceof net.minecraft.world.entity.player.Player){
            boolean registered=false;
            var players=mc.level.players();
            for(int i=0;i<players.size();i++)if(players.get(i)==player){registered=true;break;}
            if(!registered)return null;
        }else if(player.tickCount<=0)return null;
        if(module.world!=mc.level){module.onDisable();module.world=mc.level;}return module;
    }
    public static void baseline(net.minecraft.world.entity.LivingEntity player){
        var module=current(player);if(module==null)return;
        if(module.health.size()>=256&&!module.health.containsKey(player.getUUID()))return;
        module.health.putIfAbsent(player.getUUID(),totalHealth(player));
    }
    public static void changed(net.minecraft.world.entity.LivingEntity player){
        var module=current(player);if(module!=null)module.observePlayer(player,System.nanoTime());
    }
    public void observe(UUID id,float value,Vec3 position,long now,boolean visible){
        if(!enabled()||!Float.isFinite(value))return;
        if(health.size()>=256&&!health.containsKey(id))return;
        Float previous=health.put(id,value);
        if(previous!=null&&visible)spawn(position,value-previous,now);
    }
    private static float totalHealth(net.minecraft.world.entity.LivingEntity player){return player.getHealth()+player.getAbsorptionAmount();}
    private void observePlayer(net.minecraft.world.entity.LivingEntity player,long now){
        var previous=health.get(player.getUUID());
        float value=totalHealth(player);
        if(previous==null){observe(player.getUUID(),value,Vec3.ZERO,now,false);return;}
        if(Math.abs(previous-value)<.01f)return;
        var mc=Minecraft.getInstance();boolean own=player==mc.player;
        boolean visible=!player.isInvisible()&&player.distanceToSqr(mc.player)<=64*64&&(own||mc.player.hasLineOfSight(player));
        Vec3 position=own?player.getEyePosition().add(player.getViewVector(1).scale(2)).add(0,.3,0)
            :player.position().add(0,player.getBbHeight()+.2,0);
        observe(player.getUUID(),value,position,now,visible);
    }
    public void spawn(Vec3 position,float change,long now){
        if(!enabled()||!Float.isFinite(change)||Math.abs(change)<.01f||(change>0?!healing.value():!damage.value()))return;
        expire(now);while(numbers.size()>=LIMIT)numbers.removeFirst();
        float amount=Math.abs(change);
        String value=Math.abs(amount-Math.round(amount))<.05f?Integer.toString(Math.round(amount)):String.format(Locale.ROOT,"%.1f",amount);
        double side=((offset++%3)-1)*.18;
        numbers.addLast(new Number(position.add(side,0,0),(change>0?"+":"–")+value,change>0,now));
    }
    public int count(){return numbers.size();}
    public void expire(long now){while(!numbers.isEmpty()&&now-numbers.getFirst().born>=LIFETIME)numbers.removeFirst();}
    @Override protected void onDisable(){numbers.clear();health.clear();world=null;offset=0;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null){onDisable();return;}
        if(world!=mc.level){onDisable();world=mc.level;}
        long now=System.nanoTime();expire(now);Set<UUID> seen=new HashSet<>();int checked=0;
        for(var player:mc.level.players()){
            if(checked++>=128)break;
            if(player.isRemoved()||player.distanceToSqr(mc.player)>64*64)continue;
            seen.add(player.getUUID());observePlayer(player,now);
        }
        if(mobs.value())for(var mob:mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,mc.player.getBoundingBox().inflate(32),
                e->!(e instanceof net.minecraft.world.entity.player.Player)&&!e.isRemoved()&&!(e instanceof net.minecraft.world.entity.decoration.ArmorStand))){
            if(checked++>=192)break;
            seen.add(mob.getUUID());observePlayer(mob,now);
        }
        health.keySet().retainAll(seen);
    }
    public void draw(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();
        if(!enabled()||numbers.isEmpty()||mc.level==null||world!=mc.level||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();var camera=mc.gameRenderer.mainCamera();var origin=camera.position();
        var matrix=camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f());var point=new org.joml.Vector4f();
        float density=1.5f/Math.max(1,mc.getWindow().getGuiScale());
        g.pose().pushMatrix();g.pose().identity();
        try{for(var number:numbers){
            float progress=(float)(now-number.born)/LIFETIME;if(progress<0||progress>=1)continue;
            var at=number.position.add(0,progress*.65,0);
            point.set((float)(at.x-origin.x),(float)(at.y-origin.y),(float)(at.z-origin.z),1).mul(matrix);
            if(point.w<=.05f)continue;
            float x=(point.x/point.w+1)*g.guiWidth()/2f,y=(1-point.y/point.w)*g.guiHeight()/2f;
            if(!Float.isFinite(x)||!Float.isFinite(y)||x<0||y<0||x>g.guiWidth()||y>g.guiHeight())continue;
            float alpha=Math.min(1,progress/.12f)*Math.min(1,(1-progress)/.35f);
            int color=((int)(255*alpha)<<24)|(number.heal?0x64E8A5:0xFF6689);
            // Pops in slightly larger, then settles; keeps readable size further away than before.
            float pop=1+.35f*(float)Math.pow(1-Math.min(1,progress/.15f),2);
            float size=16*density*scale.value().floatValue()/100f*pop*(float)Math.clamp(8/Math.max(1,origin.distanceTo(at)),.85,1);
            float shadow=Math.max(1,size/12f);
            VelaDraw.floatingText(g,number.text,x+shadow,y+shadow,((int)(190*alpha)<<24),size);
            VelaDraw.floatingText(g,number.text,x,y,color,size);
        }}finally{g.pose().popMatrix();}
    }
}
