package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.render.effect.NeonGeometry;
import dev.sqwat.vela.ui.hud.HudVisibility;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Styled hitboxes for F3+B: while the module is on, the debug hitbox key shows these instead of vanilla's white
 * boxes (EntityHitboxDebugRenderer is skipped). Visual bounds only: never changes collision, reach or dimensions.
 */
public final class Hitboxes extends ClientModule {
    private static final int LIMIT=64;
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Режим","Вид выделения","Контур",List.of("Контур","Заливка","Оба")));
    private final NumberSetting distance=setting(new NumberSetting("distance","Дистанция","Максимальная дальность в блоках",32,8,64,4));
    private final NumberSetting opacity=setting(new NumberSetting("opacity","Непрозрачность заливки","Плотность заливки в процентах",12,5,40,1));
    private final BooleanSetting players=setting(new BooleanSetting("players","Игроки","Выделять остальных игроков",true));
    private final BooleanSetting friends=setting(new BooleanSetting("friends","Друзья","Выделять друзей отдельным цветом",true));
    private final BooleanSetting self=setting(new BooleanSetting("self","Себя","Показывать свой контур от третьего лица",false));
    private final BooleanSetting others=setting(new BooleanSetting("others","Мобы и предметы","Остальные сущности, как в F3+B",true));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Использовать цвет темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0x849BFF));
    private final ColorSetting friendColor=setting(new ColorSetting("friend_color","Цвет друзей",0x65DDB0));
    private record Candidate(net.minecraft.world.entity.Entity player,boolean friend,double distance){}
    private static final Comparator<Candidate> NEAREST=Comparator.comparingDouble(Candidate::distance);
    private final ArrayList<Candidate> visible=new ArrayList<>(LIMIT+1);
    private Object world;
    public Hitboxes(){
        super("hitboxes","Hitboxes","Свои контуры хитбоксов вместо стандартных по F3+B",ModuleCategory.VISUALS);
        opacity.visibleWhen(()->!mode.value().equals("Контур"));color.visibleWhen(()->!theme.value());friendColor.visibleWhen(friends::value);
    }
    public boolean selected(boolean own,boolean friend,boolean firstPerson){return own?self.value()&&!firstPerson:friend?friends.value():players.value();}
    public static AABB renderedBox(AABB box,Vec3 current,Vec3 interpolated){return box.move(interpolated.subtract(current));}
    @Override protected void onDisable(){visible.clear();world=null;}
    public static Hitboxes current(){return dev.sqwat.vela.module.Modules.active(Hitboxes.class);}
    /** F3+B is on: the moment these boxes are shown. */
    private static boolean debugKey(Minecraft mc){return mc.debugEntries.isCurrentlyEnabled(net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES);}
    private void consider(net.minecraft.world.entity.Entity entity,boolean friend,double d){
        var next=new Candidate(entity,friend,d);
        int index=java.util.Collections.binarySearch(visible,next,NEAREST);
        if(index<0)index=-index-1;
        if(index<LIMIT){visible.add(index,next);if(visible.size()>LIMIT)visible.removeLast();}
    }
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();var services=dev.sqwat.vela.VelaClient.services();
        visible.clear();world=mc.level;
        if(mc.level==null||mc.player==null||services==null||!debugKey(mc))return;
        double range=distance.value()*distance.value();int scanned=0;
        for(var player:mc.level.players()){
            if(++scanned>256)break;
            if(player.isRemoved()||!player.isAlive()||player.isSpectator()||player.isInvisibleTo(mc.player))continue;
            boolean friend=services.friends().contains(player.getGameProfile().name());
            if(!selected(player==mc.player,friend,mc.options.getCameraType().isFirstPerson()))continue;
            double d=player.distanceToSqr(mc.player);if(d>range)continue;
            consider(player,friend,d);
        }
        if(others.value())for(var entity:mc.level.entitiesForRendering()){
            if(++scanned>1024)break;
            if(entity instanceof net.minecraft.world.entity.player.Player||entity.isRemoved()||entity.isInvisible())continue;
            double d=entity.distanceToSqr(mc.player);if(d>range)continue;
            consider(entity,false,d);
        }
    }
    public void submit(){
        var mc=Minecraft.getInstance();
        if(!enabled()||mc.level==null||world!=mc.level||mc.player==null||!HudVisibility.allowed(mc)||!debugKey(mc))return;
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for(var candidate:visible){
            var player=candidate.player();
            if(player.level()!=mc.level||player.isRemoved()||!player.isAlive()||player.isInvisibleTo(mc.player))continue;
            if(player instanceof net.minecraft.world.entity.player.Player&&!selected(player==mc.player,candidate.friend(),mc.options.getCameraType().isFirstPerson()))continue;
            int rgb=candidate.friend()?friendColor.value():theme.value()?VelaTheme.PRIMARY:color.value();
            int stroke=NeonGeometry.tint(rgb,.9),fill=NeonGeometry.tint(rgb,opacity.value()/100);
            var style=switch(mode.value()){case "Заливка"->GizmoStyle.fill(fill);case "Оба"->GizmoStyle.strokeAndFill(stroke,1.5f,fill);default->GizmoStyle.stroke(stroke,1.5f);};
            Gizmos.cuboid(renderedBox(player.getBoundingBox(),player.position(),player.getPosition(partial)),style);
        }
    }
}
