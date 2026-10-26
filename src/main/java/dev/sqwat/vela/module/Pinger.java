package dev.sqwat.vela.module;

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
    public float warningOpacity(long now,boolean reducedMotion){
        return enabled()&&screenWarning.value()?dev.sqwat.vela.ui.animation.WarningPulse.opacity(now,warningUntil,reducedMotion):0;
    }
    public boolean screenWarningActive(){return enabled()&&screenWarning.value()&&System.nanoTime()<warningUntil;}
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};

    public Pinger(){
        super("pinger","Pinger","Уведомляет о выбранном пороге прочности брони или здоровья",ModuleCategory.UTILITIES);
        armorPercent.visibleWhen(armor::value);healthHp.visibleWhen(health::value);
    }

    @Override protected void onDisable(){warningUntil=0;dev.sqwat.vela.ui.Notifications.clearHud();}
    public static boolean shouldWarn(double current,double threshold){return Double.isFinite(current)&&current>=0&&current<=threshold;}

    @Override public void onClientTick(){
        var player=Minecraft.getInstance().player;
        if(player==null||player.isSpectator()||!player.isAlive()){onDisable();return;}
        var crossed=new java.util.ArrayList<dev.sqwat.vela.ui.Notifications.HudValue>();
        if(armor.value()){
            for(var slot:ARMOR){
                ItemStack stack=player.getItemBySlot(slot);
                if(stack.isEmpty()||!stack.isDamageableItem())continue;
                int percent=Math.max(0,Math.round(100f*(stack.getMaxDamage()-stack.getDamageValue())/stack.getMaxDamage()));
                if(shouldWarn(percent,armorPercent.value())){
                    crossed.add(new dev.sqwat.vela.ui.Notifications.HudValue("armor",stack,percent+"%",percent/100f));
                }
            }
        }
        if(health.value()){
            float hp=player.getHealth();
            if(shouldWarn(hp,healthHp.value())){
                crossed.add(new dev.sqwat.vela.ui.Notifications.HudValue("heart",ItemStack.EMPTY,formatHp(hp)+" HP",hp/Math.max(1,player.getMaxHealth())));
            }
        }
        if(!crossed.isEmpty()){dev.sqwat.vela.ui.Notifications.showHudValues(crossed);warningUntil=System.nanoTime()+3_500_000_000L;}
        else{warningUntil=0;dev.sqwat.vela.ui.Notifications.clearHud();}
    }

    private static String label(EquipmentSlot slot){return switch(slot){case HEAD->"Шлем";case CHEST->"Нагрудник";case LEGS->"Поножи";case FEET->"Ботинки";default->"Броня";};}
    private static String formatHp(float hp){return String.format(java.util.Locale.ROOT,"%.1f",hp).replaceAll("\\.0$","");}
}
