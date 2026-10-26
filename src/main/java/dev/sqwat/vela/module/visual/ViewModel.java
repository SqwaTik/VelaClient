package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.sqwat.vela.module.setting.*;
import net.minecraft.world.InteractionHand;
import java.util.List;

/** Cosmetic transforms within the vanilla hand pose; no gameplay state changes. */
public final class ViewModel extends ClientModule {
    private final ChoiceSetting hand=setting(new ChoiceSetting("hand","Рука","К каким рукам применять настройки","Обе",List.of("Обе","Основная","Вторая")));
    private final NumberSetting x=setting(new NumberSetting("offset_x","По горизонтали","Смещение рук по горизонтали",0,-1,1,.01));
    private final NumberSetting y=setting(new NumberSetting("offset_y","По вертикали","Смещение рук вверх или вниз",0,-1,1,.01));
    private final NumberSetting z=setting(new NumberSetting("offset_z","Глубина","Приблизить или отдалить руки",0,-1,1,.01));
    private final NumberSetting scale=setting(new NumberSetting("scale","Размер","Масштаб рук и предметов",1,.25,2,.01));
    private final NumberSetting pitch=setting(new NumberSetting("pitch","Наклон","Поворот вверх или вниз",0,-90,90,1));
    private final NumberSetting yaw=setting(new NumberSetting("yaw","Разворот","Поворот влево или вправо",0,-90,90,1));
    private final NumberSetting roll=setting(new NumberSetting("roll","Крен","Вращение вокруг направления взгляда",0,-90,90,1));
    private final List<NumberSetting> main=List.of(x,y,z,pitch,yaw,roll,scale);
    private final List<NumberSetting> off;
    private boolean preview;
    private InteractionHand editingHand=InteractionHand.MAIN_HAND;
    private final double[] wheelRemainder=new double[3];
    public ViewModel(){
        super("view_model","ViewModel","Положение, размер и поворот рук от первого лица",ModuleCategory.VISUALS);
        off=main.stream().map(s->setting(new NumberSetting("off_"+s.id(),s.name(),s.description(),s.defaultValue(),s.min(),s.max(),s.step()))).toList();
        for(var option:settings())option.visibleWhen(()->false);
        setting(new ActionSetting("editor","Открыть редактор"));
    }
    public void preview(boolean value){preview=value;java.util.Arrays.fill(wheelRemainder,0);}
    public boolean previewing(){return preview;}
    public ChoiceSetting hand(){return hand;}
    public List<NumberSetting> transforms(){return transforms(hand.value().equals("Вторая")?InteractionHand.OFF_HAND:hand.value().equals("Основная")?InteractionHand.MAIN_HAND:editingHand);}
    public List<NumberSetting> transforms(InteractionHand target){return target==InteractionHand.MAIN_HAND?main:off;}
    public void editHand(InteractionHand target){if(editingHand!=target){editingHand=target;java.util.Arrays.fill(wheelRemainder,0);}}
    public void selectHand(String value){hand.set(value);java.util.Arrays.fill(wheelRemainder,0);}
    public void setTransform(int index,double value){
        transforms().get(index).set(value);
    }
    /** Constrain the preview anchor to the camera frustum, including depth edits. */
    public void keepVisible(InteractionHand hand,boolean right,float projectionX,float projectionY){
        if(!Float.isFinite(projectionX)||!Float.isFinite(projectionY)||Math.abs(projectionX)<.01||Math.abs(projectionY)<.01)return;
        var v=transforms(hand);
        v.get(2).set(Math.min(.40,v.get(2).value()));
        double depth=.72-v.get(2).value();
        double horizontal=.85*depth/Math.abs(projectionX),vertical=.85*depth/Math.abs(projectionY);
        double originX=right?.56:-.56;
        v.get(0).set(Math.clamp(v.get(0).value(),-horizontal-originX,horizontal-originX));
        v.get(1).set(Math.clamp(v.get(1).value(),-vertical+.30,vertical+.30));
    }
    public void resetTransforms(){
        transforms().forEach(NumberSetting::reset);
        if(hand.value().equals("Обе")){main.forEach(NumberSetting::reset);off.forEach(NumberSetting::reset);}
    }
    public void migrateLegacy(){
        if(!hand.value().equals("Основная"))for(int i=0;i<main.size();i++)off.get(i).set(main.get(i).value());
        if(hand.value().equals("Вторая"))main.forEach(NumberSetting::reset);
    }
    public void wheel(double amount,boolean rotate,boolean depth){
        int component=rotate?5:depth?2:6;
        NumberSetting target=transforms().get(component);
        int index=rotate?1:depth?2:0;
        double desired=Math.clamp(target.value()+wheelRemainder[index]+amount*(rotate?3:.025),target.min(),target.max());
        setTransform(component,desired);wheelRemainder[index]=desired-target.value();
    }
    public void apply(PoseStack pose,InteractionHand renderedHand){
        apply(pose,renderedHand,renderedHand==InteractionHand.MAIN_HAND);
    }
    public void apply(PoseStack pose,InteractionHand renderedHand,boolean rightArm){
        if(!enabled()&&!preview)return;
        var values=renderedHand==InteractionHand.MAIN_HAND?main:off;
        var x=values.get(0);var y=values.get(1);var z=values.get(2);
        var pitch=values.get(3);var yaw=values.get(4);var roll=values.get(5);var scale=values.get(6);
        pose.translate(x.value(),y.value(),z.value());
        if(scale.value()==1&&pitch.value()==0&&yaw.value()==0&&roll.value()==0)return;
        // Match the vanilla neutral hand origin. Scaling the camera origin cancels
        // apparent size in perspective and changes only the screen position.
        double pivotX=rightArm?.56:-.56,pivotY=-.52,pivotZ=-.72;
        pose.translate(pivotX,pivotY,pivotZ);
        pose.mulPose(Axis.XP.rotationDegrees(pitch.value().floatValue()));
        pose.mulPose(Axis.YP.rotationDegrees(yaw.value().floatValue()));
        pose.mulPose(Axis.ZP.rotationDegrees(roll.value().floatValue()));
        float s=scale.value().floatValue();pose.scale(s,s,s);
        pose.translate(-pivotX,-pivotY,-pivotZ);
    }
}
