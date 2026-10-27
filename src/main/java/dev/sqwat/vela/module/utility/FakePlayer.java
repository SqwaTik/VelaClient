package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.visual.Particles;
import dev.sqwat.vela.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerSkin;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

/** A local visual dummy. Never sends a spawn or inventory request to the server. */
public final class FakePlayer extends ClientModule {
    private final BooleanSetting equipment=setting(new BooleanSetting("equipment","Копировать экипировку","Броня и предметы в руках при создании",true));
    private final BooleanSetting skin=setting(new BooleanSetting("skin","Копировать скин","Скин вашего игрока",true));
    private final BooleanSetting knockback=setting(new BooleanSetting("knockback","Отдача","Отбрасывать манекен при ударе",true));
    private ClientLevel owner;
    private LocalDummy dummy;
    private boolean localAttack;
    public FakePlayer(){super("fake_player","FakePlayer","Локальный манекен для тренировки ударов",ModuleCategory.UTILITIES);}
    public boolean hit(Entity entity){
        var player=Minecraft.getInstance().player;
        if(!enabled()||dummy==null||entity!=dummy||player==null)return false;
        dummy.syncEquipmentAttributes();
        localAttack=true;
        try{player.attack(dummy);}finally{localAttack=false;}
        return true;
    }
    public static FakePlayer current(){
        return dev.sqwat.vela.module.Modules.active(FakePlayer.class);
    }
    public Boolean receiveLocalHit(Entity entity,net.minecraft.world.damagesource.DamageSource source,float amount){
        return localAttack&&entity==dummy?dummy.receive(source,amount):null;
    }
    public boolean attacking(Entity target){return localAttack&&target==dummy;}
    public boolean playAttackSound(net.minecraft.world.entity.player.Player player,net.minecraft.sounds.SoundEvent sound){
        if(!localAttack||player!=Minecraft.getInstance().player||owner==null)return false;
        owner.playLocalSound(player.getX(),player.getY(),player.getZ(),sound,net.minecraft.sounds.SoundSource.PLAYERS,1,1,false);
        return true;
    }
    private final class LocalDummy extends AbstractClientPlayer {
        private final PlayerSkin copiedSkin;
        private final java.util.Map<EquipmentSlot,net.minecraft.world.item.ItemStack> appliedEquipment=new java.util.EnumMap<>(EquipmentSlot.class);
        LocalDummy(ClientLevel level,GameProfile profile,PlayerSkin copiedSkin){
            super(level,profile);this.copiedSkin=copiedSkin;
            for(var slot:EquipmentSlot.values())appliedEquipment.put(slot,net.minecraft.world.item.ItemStack.EMPTY);
        }
        void syncEquipmentAttributes(){
            // Client entities normally receive attributes from the server. This local entity
            // has no server counterpart, so apply vanilla equipment modifiers locally.
            var changed=collectEquipmentChanges(appliedEquipment);
            if(changed!=null)changed.forEach((slot,stack)->appliedEquipment.put(slot,stack.copy()));
        }
        @Override public PlayerSkin getSkin(){return copiedSkin!=null?copiedSkin:super.getSkin();}
        // Match RemotePlayer: calculate the walk cycle after the complete movement tick.
        @Override public void calculateEntityAnimation(boolean flying){}
        @Override public void tick(){super.tick();super.calculateEntityAnimation(false);}
        @Override public boolean isPushable(){return knockback.value()&&super.isPushable();}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean isCreative(){return false;}
        @Override public boolean canSimulateMovement(){return true;}
        @Override public boolean isEffectiveAi(){return true;}
        @Override public void knockback(double strength,double x,double z,net.minecraft.world.damagesource.DamageSource source,float damage,boolean force){
            if(knockback.value())super.knockback(strength,x,z,source,damage,force);
        }
        boolean receive(net.minecraft.world.damagesource.DamageSource source,float damage){
            if(!isAlive())return false;
            boolean protectedHit=invulnerableTime>10&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN);
            float accepted=dev.sqwat.vela.render.effect.LocalHitWindow.damage(damage,lastHurt,protectedHit);
            if(accepted<=0)return false;
            lastHurt=damage;
            if(!protectedHit)invulnerableTime=20;
            float absorbed=getDamageAfterMagicAbsorb(source,getDamageAfterArmorAbsorb(source,accepted));
            float healthDamage=Math.max(0,absorbed-getAbsorptionAmount());
            setAbsorptionAmount(Math.max(0,getAbsorptionAmount()-(absorbed-healthDamage)));
            setHealth(Math.max(0,getHealth()-healthDamage));
            if(isDeadOrDying()&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)){
                for(var hand:net.minecraft.world.InteractionHand.values()){
                    var stack=getItemInHand(hand);
                    var protection=stack.get(net.minecraft.core.component.DataComponents.DEATH_PROTECTION);
                    if(protection==null)continue;
                    var consumed=stack.copy();stack.shrink(1);setHealth(1);
                    protection.applyEffects(consumed,this);
                    Particles.emit(this,Particles.Event.TOTEM);
                    Minecraft.getInstance().particleEngine.createTrackingEmitter(this,net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,30);
                    owner.playLocalSound(getX(),getY(),getZ(),net.minecraft.sounds.SoundEvents.TOTEM_USE,net.minecraft.sounds.SoundSource.PLAYERS,1,1,false);
                    break;
                }
            }
            // Keep the training entity alive after a lethal hit, preserving hit feedback.
            if(isDeadOrDying())setHealth(getMaxHealth());
            Particles.emit(this,Particles.Event.HIT);
            if(protectedHit)return true;
            // Exactly what a client does when the server reports another player was hurt: leg flail, red flash and
            // the hurt sound (ClientboundDamageEventPacket -> handleDamageEvent), the server's knockback, and the
            // dark-heart damage indicators the server sends for hits over 2 HP.
            if(!source.is(net.minecraft.tags.DamageTypeTags.NO_KNOCKBACK))dealDefaultKnockback(source,damage,false);
            handleDamageEvent(source);
            if(healthDamage>2)damageIndicators((int)(healthDamage*.5f));
            return true;
        }
        /** ServerLevel.sendParticles(DAMAGE_INDICATOR, x, y(0.5), z, count, .1, 0, .1, .2) done locally. */
        private void damageIndicators(int count){
            for(int i=0;i<count;i++){
                double dx=random.nextGaussian()*.1,dz=random.nextGaussian()*.1;
                owner.addParticle(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,getX()+dx,getY(.5),getZ()+dz,random.nextGaussian()*.2,random.nextGaussian()*.2,random.nextGaussian()*.2);
            }
        }
        @Override public void hurtArmor(net.minecraft.world.damagesource.DamageSource source,float damage){
            if(damage<=0)return;
            int wear=Math.max(1,(int)(damage/4));
            for(var slot:EquipmentSlot.values()){
                if(!slot.isArmor())continue;
                var stack=getItemBySlot(slot);
                var equippable=stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
                if(equippable==null||!equippable.damageOnHurt()||!stack.isDamageableItem()||!stack.canBeHurtBy(source))continue;
                int unbreaking=LocalCombatRules.level(stack,net.minecraft.world.item.enchantment.Enchantments.UNBREAKING),loss=0;
                for(int i=0;i<Math.min(wear,stack.getMaxDamage());i++)if(unbreaking==0||random.nextFloat()<.6f||random.nextInt(unbreaking+1)==0)loss++;
                int next=stack.getDamageValue()+loss;
                if(next>=stack.getMaxDamage()){
                    setItemSlot(slot,net.minecraft.world.item.ItemStack.EMPTY);
                    owner.playLocalSound(getX(),getY(),getZ(),net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(),net.minecraft.sounds.SoundSource.PLAYERS,1,1,false);
                }else stack.setDamageValue(next);
            }
        }
        @Override protected float getDamageAfterMagicAbsorb(net.minecraft.world.damagesource.DamageSource source,float damage){
            float reduced=super.getDamageAfterMagicAbsorb(source,damage);
            if(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_EFFECTS)||source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ENCHANTMENTS))return reduced;
            int protection=0;
            for(var slot:EquipmentSlot.values())if(slot.isArmor())protection+=LocalCombatRules.protection(getItemBySlot(slot),source);
            return net.minecraft.world.damagesource.CombatRules.getDamageAfterMagicAbsorb(reduced,protection);
        }
    }
    @Override protected void onDisable(){clear();}
    private void clear(){
        if(owner!=null&&dummy!=null&&owner.getEntity(dummy.getId())==dummy)
            owner.removeEntity(dummy.getId(),Entity.RemovalReason.DISCARDED);
        owner=null;dummy=null;localAttack=false;
    }
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){clear();return;}
        if(owner!=mc.level)clear();
        if(dummy!=null&&!dummy.isRemoved())return;
        int id=Integer.MAX_VALUE;
        while(id>Integer.MAX_VALUE-512&&mc.level.getEntity(id)!=null)id--;
        if(id==Integer.MAX_VALUE-512)return;
        var localSkin=mc.player.getSkin();
        boolean copySkin=skin.value();
        dummy=new LocalDummy(mc.level,new GameProfile(UUID.randomUUID(),mc.player.getGameProfile().name()),copySkin?localSkin:null);
        dummy.setId(id);
        dummy.snapTo(mc.player.position(),mc.player.getYRot(),mc.player.getXRot());
        dummy.setYHeadRot(mc.player.getYHeadRot());
        dummy.setYBodyRot(mc.player.getYRot());
        dummy.setOldPosAndRot();
        dummy.setOnGround(mc.player.onGround());
        dummy.getAttributes().assignBaseValues(mc.player.getAttributes());
        if(equipment.value())for(var slot:EquipmentSlot.values())dummy.setItemSlot(slot,mc.player.getItemBySlot(slot).copy());
        dummy.syncEquipmentAttributes();
        for(var effect:mc.player.getActiveEffects())dummy.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
        dummy.setHealth(Math.min(dummy.getMaxHealth(),mc.player.getHealth()));
        dummy.setAbsorptionAmount(mc.player.getAbsorptionAmount());
        owner=mc.level;owner.addEntity(dummy);
    }
}
