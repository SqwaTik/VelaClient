package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.render.*;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Cross/circle crosshair with the attack cooldown shown as spread. */
public final class Crosshair extends ClientModule {
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Вид","Форма прицела","Крестик",List.of("Крестик","Круг")));
    private final NumberSetting size=setting(new NumberSetting("size","Размер","Длина лучей или радиус круга",4,2,10,.5));
    private final NumberSetting thickness=setting(new NumberSetting("thickness","Толщина","Толщина линий",1,1,4,.5));
    private final NumberSetting gap=setting(new NumberSetting("gap","Отступ","Расстояние лучей от центра",2,0,5,.5));
    private final NumberSetting spread=setting(new NumberSetting("spread","Отступ атаки","Расширение при перезарядке удара",10,0,20,.5));
    private final BooleanSetting dot=setting(new BooleanSetting("dot","Точка","Точка в центре прицела",false));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Использовать основной цвет интерфейса",true));
    private final dev.sqwat.vela.module.setting.ColorSetting color=setting(new dev.sqwat.vela.module.setting.ColorSetting("color","Цвет",0xF3F5FA));
    private final BooleanSetting target=setting(new BooleanSetting("target","Реакция на цель","Изменение цвета при наведении на сущность в пределах обычной дальности взаимодействия",true));
    private final BooleanSetting dynamic=setting(new BooleanSetting("dynamic","Динамика","Инерция круга и анимация при ударе; выключите для неподвижного прицела",true));
    private final dev.sqwat.vela.ui.animation.CrosshairMotion motion=new dev.sqwat.vela.ui.animation.CrosshairMotion();
    private final dev.sqwat.vela.ui.animation.FrameClock clock=new dev.sqwat.vela.ui.animation.FrameClock();
    private Object world,screen;
    public Crosshair(){super("crosshair","Crosshair","Настраиваемый прицел с индикацией перезарядки",ModuleCategory.VISUALS);gap.visibleWhen(()->mode.value().equals("Крестик"));color.visibleWhen(()->!theme.value());spread.visibleWhen(()->dynamic.value()&&mode.value().equals("Крестик"));}
    @Override protected void onDisable(){motion.reset();clock.reset();world=screen=null;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.level!=world||mc.gui.screen()!=screen||!mc.options.getCameraType().isFirstPerson()){motion.reset();clock.reset();}
        world=mc.level;screen=mc.gui.screen();
    }
    public static Crosshair current(){return dev.sqwat.vela.module.Modules.active(Crosshair.class);}
    public static float attackGap(double base,double amount,float cooldown){return (float)(base+amount*(1-Math.clamp(cooldown,0,1)));}
    public static int reactionColor(int base){
        int danger=VelaTheme.DANGER;
        int distance=Math.abs((base>>16&255)-(danger>>16&255))+Math.abs((base>>8&255)-(danger>>8&255))+Math.abs((base&255)-(danger&255));
        return distance<180?VelaTheme.TEXT:danger;
    }
    public void draw(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        boolean circle=mode.value().equals("Круг");
        boolean targeted=mc.gui.screen()==null&&target.value()&&mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit&&hit.getEntity().isAlive();
        motion.update(mc.player.getViewYRot(partial),mc.player.getViewXRot(partial),targeted,dynamic.value()&&circle&&mc.gui.screen()==null,clock.step());
        int base=theme.value()?VelaTheme.PRIMARY:0xFF000000|color.value();
        int color=VelaTheme.mix(base,reactionColor(base),motion.reaction());
        float x=g.guiWidth()/2f,y=g.guiHeight()/2f;
        if(circle){
            x+=motion.x();y+=motion.y();
            float radius=size.value().floatValue()+1;
            g.guiRenderState.addGuiElement(new CrosshairRingState(g.pose(),x,y,radius,thickness.value().floatValue()+.6f,
                dev.sqwat.vela.ui.animation.CrosshairMotion.arc(mc.player.getAttackAnim(partial),dynamic.value()),color,VelaDraw.pixelDensity(g),g.scissorStack.peek()));
            if(dot.value())new GameUiCanvas(g).round(x-.5f,y-.5f,1,1,.5f,color);
        }else{
            float opening=attackGap(gap.value(),dynamic.value()?spread.value():0,mc.player.getAttackStrengthScale(partial));
            CrosshairChrome.draw(new GameUiCanvas(g),x,y,size.value().floatValue(),thickness.value().floatValue(),opening,dot.value(),color);
        }
    }
}
