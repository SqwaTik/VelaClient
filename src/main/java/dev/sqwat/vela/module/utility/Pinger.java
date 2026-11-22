package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/** Shows current low health/armor state and clears it as soon as the condition ends. */
public final class Pinger extends ClientModule {
    private final BooleanSetting armor=setting(new BooleanSetting("armor","Прочность брони","Уведомлять о низкой прочности экипированной брони",true));
    private final NumberSetting armorPercent=setting(new NumberSetting("armor_percent","Порог брони, %","Остаток прочности отдельной части брони",20,5,40,1));
    private final BooleanSetting health=setting(new BooleanSetting("health","Здоровье","Уведомлять о низком здоровье игрока",false));
    private final NumberSetting healthHp=setting(new NumberSetting("health_hp","Порог здоровья, HP","Порог текущего здоровья, где 2 HP равны одному сердцу",6,1,20,1));
    private final BooleanSetting screenWarning=setting(new BooleanSetting("screen_warning","Красное предупреждение","Подсвечивать края экрана при низком здоровье или прочности брони",false));
    private long warningUntil;
    /** Worst current severity (0 at the threshold, 1 when nothing is left); deepens the red edge. */
    private float severity;
    public float warningOpacity(long now,boolean reducedMotion){
        return enabled()&&screenWarning.value()?dev.sqwat.vela.ui.animation.WarningPulse.opacity(now,warningUntil,reducedMotion)*(.65f+.55f*severity):0;
    }
    public boolean screenWarningActive(){return enabled()&&screenWarning.value()&&System.nanoTime()<warningUntil;}
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};

    public Pinger(){
        super("pinger","Pinger","Уведомляет о выбранном пороге прочности брони или здоровья",ModuleCategory.UTILITIES);
        armorPercent.visibleWhen(armor::value);healthHp.visibleWhen(health::value);
    }

    /** Per armour slot: the item last seen there and whether its warning was already shown. */
    private final ItemStack[] warnedStack=new ItemStack[4];
    private final boolean[] warnedArmor=new boolean[4];
    private boolean warnedHealth;
    private boolean showing;
    /** How far above the threshold the value must come back before the warning can fire again (5 %, one heart). */
    static final int ARMOR_REARM=5;
    static final float HEALTH_REARM=2;

    @Override protected void onDisable(){warningUntil=0;reset();if(showing){dev.sqwat.vela.ui.Notifications.clearHud();showing=false;}}
    private void reset(){java.util.Arrays.fill(warnedStack,null);java.util.Arrays.fill(warnedArmor,false);warnedHealth=false;}
    public static boolean shouldWarn(double current,double threshold){return Double.isFinite(current)&&current>=0&&current<=threshold;}
    /**
     * Whether a shown warning may fire again: only once the value is back clearly above the threshold. Without the
     * margin, regeneration around the threshold (5.5 → 6.5 → 5.8 HP) warned again on every swing.
     */
    public static boolean rearms(double current,double threshold,double margin){return Double.isFinite(current)&&current>threshold+margin;}

    /**
     * One warning per crossing: when health or a piece of armour reaches the set threshold. Further losses below it
     * stay silent (the HUD value still updates); the warning fires again only after the value recovers above the
     * threshold (healing, repair) or another piece is put on. Servers resend the armour stack on every point of
     * damage, so the piece is compared by item, not by instance; comparing instances re-warned on every hit.
     */
    @Override public void onClientTick(){
        var player=Minecraft.getInstance().player;
        if(player==null||player.isSpectator()||!player.isAlive()){onDisable();return;}
        var crossed=new java.util.ArrayList<dev.sqwat.vela.ui.Notifications.HudValue>();
        boolean fresh=false;
        for(int i=0;i<ARMOR.length;i++){
            ItemStack stack=player.getItemBySlot(ARMOR[i]);
            if(!armor.value()||stack.isEmpty()||!stack.isDamageableItem()){warnedStack[i]=null;warnedArmor[i]=false;continue;}
            if(warnedStack[i]==null||!ItemStack.isSameItem(stack,warnedStack[i]))warnedArmor[i]=false;
            warnedStack[i]=stack;
            int percent=Math.max(0,Math.round(100f*(stack.getMaxDamage()-stack.getDamageValue())/stack.getMaxDamage()));
            if(!shouldWarn(percent,armorPercent.value())){if(rearms(percent,armorPercent.value(),ARMOR_REARM))warnedArmor[i]=false;continue;}
            crossed.add(new dev.sqwat.vela.ui.Notifications.HudValue("armor",stack,percent+"%",percent/100f,1-percent/armorPercent.value().floatValue()));
            if(!warnedArmor[i]){warnedArmor[i]=true;fresh=true;}
        }
        float hp=player.getHealth();
        if(health.value()&&shouldWarn(hp,healthHp.value())){
            crossed.add(new dev.sqwat.vela.ui.Notifications.HudValue("heart",ItemStack.EMPTY,formatHp(hp)+" HP",hp/Math.max(1,player.getMaxHealth()),1-hp/healthHp.value().floatValue()));
            if(!warnedHealth){warnedHealth=true;fresh=true;}
        }else if(!health.value()||rearms(hp,healthHp.value(),HEALTH_REARM))warnedHealth=false;
        severity=0;for(var value:crossed)severity=Math.max(severity,value.severity());
        if(fresh){dev.sqwat.vela.ui.Notifications.showHudValues(crossed);showing=true;warningUntil=System.nanoTime()+3_500_000_000L;}
        else if(crossed.isEmpty()&&showing){warningUntil=0;dev.sqwat.vela.ui.Notifications.clearHud();showing=false;}
        else if(showing)dev.sqwat.vela.ui.Notifications.updateHudValues(crossed);
    }

    private static String label(EquipmentSlot slot){return switch(slot){case HEAD->"Шлем";case CHEST->"Нагрудник";case LEGS->"Поножи";case FEET->"Ботинки";default->"Броня";};}
    private static String formatHp(float hp){return String.format(java.util.Locale.ROOT,"%.1f",hp).replaceAll("\\.0$","");}
}
