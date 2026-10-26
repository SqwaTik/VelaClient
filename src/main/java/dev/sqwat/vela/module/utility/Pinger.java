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

    /** Per armour slot: the stack last warned about and the percent at that warning (MAX: not warned). */
    private final ItemStack[] warnedStack=new ItemStack[4];
    private final int[] warnedPercent={Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE};
    private float warnedHp=Float.MAX_VALUE;
    private boolean showing;
    static final int ARMOR_STEP=5;
    static final float HEALTH_STEP=2;

    @Override protected void onDisable(){warningUntil=0;reset();if(showing){dev.sqwat.vela.ui.Notifications.clearHud();showing=false;}}
    private void reset(){java.util.Arrays.fill(warnedStack,null);java.util.Arrays.fill(warnedPercent,Integer.MAX_VALUE);warnedHp=Float.MAX_VALUE;}
    public static boolean shouldWarn(double current,double threshold){return Double.isFinite(current)&&current>=0&&current<=threshold;}
    /** A new warning is due when nothing was warned yet ({@code lastWarned} = NaN) or the value fell another {@code step}. */
    public static boolean due(double current,double lastWarned,double step){return Double.isNaN(lastWarned)||current<=lastWarned-step;}

    /**
     * Shows a warning only when something changed: a piece newly below the threshold, a further drop of 5%
     * (2 HP for health), or a different piece equipped. Otherwise the notification and the red edge expire on their
     * own; repairing or removing the item resets it. Previously it was re-shown every tick and never disappeared.
     */
    @Override public void onClientTick(){
        var player=Minecraft.getInstance().player;
        if(player==null||player.isSpectator()||!player.isAlive()){onDisable();return;}
        var crossed=new java.util.ArrayList<dev.sqwat.vela.ui.Notifications.HudValue>();
        boolean fresh=false;
        for(int i=0;i<ARMOR.length;i++){
            ItemStack stack=player.getItemBySlot(ARMOR[i]);
            if(!armor.value()||stack.isEmpty()||!stack.isDamageableItem()){warnedStack[i]=null;warnedPercent[i]=Integer.MAX_VALUE;continue;}
            if(stack!=warnedStack[i]){warnedStack[i]=stack;warnedPercent[i]=Integer.MAX_VALUE;}
            int percent=Math.max(0,Math.round(100f*(stack.getMaxDamage()-stack.getDamageValue())/stack.getMaxDamage()));
            if(!shouldWarn(percent,armorPercent.value())){warnedPercent[i]=Integer.MAX_VALUE;continue;}
            crossed.add(new dev.sqwat.vela.ui.Notifications.HudValue("armor",stack,percent+"%",percent/100f,1-percent/armorPercent.value().floatValue()));
            if(due(percent,warnedPercent[i]==Integer.MAX_VALUE?Double.NaN:warnedPercent[i],ARMOR_STEP)){warnedPercent[i]=percent;fresh=true;}
        }
        float hp=player.getHealth();
        if(health.value()&&shouldWarn(hp,healthHp.value())){
            crossed.add(new dev.sqwat.vela.ui.Notifications.HudValue("heart",ItemStack.EMPTY,formatHp(hp)+" HP",hp/Math.max(1,player.getMaxHealth()),1-hp/healthHp.value().floatValue()));
            if(due(hp,warnedHp==Float.MAX_VALUE?Double.NaN:warnedHp,HEALTH_STEP)){warnedHp=hp;fresh=true;}
        }else warnedHp=Float.MAX_VALUE;
        severity=0;for(var value:crossed)severity=Math.max(severity,value.severity());
        if(fresh){dev.sqwat.vela.ui.Notifications.showHudValues(crossed);showing=true;warningUntil=System.nanoTime()+3_500_000_000L;}
        else if(crossed.isEmpty()&&showing){warningUntil=0;dev.sqwat.vela.ui.Notifications.clearHud();showing=false;}
        else if(showing)dev.sqwat.vela.ui.Notifications.updateHudValues(crossed);
    }

    private static String label(EquipmentSlot slot){return switch(slot){case HEAD->"Шлем";case CHEST->"Нагрудник";case LEGS->"Поножи";case FEET->"Ботинки";default->"Броня";};}
    private static String formatHp(float hp){return String.format(java.util.Locale.ROOT,"%.1f",hp).replaceAll("\\.0$","");}
}
