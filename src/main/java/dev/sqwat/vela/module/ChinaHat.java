package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.render.NeonGeometry;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import java.util.List;
import java.util.ArrayList;

/** Local cosmetic headwear; never changes equipment or sends packets. */
public final class ChinaHat extends ClientModule {
    private final NumberSetting radius=setting(new NumberSetting("radius","Радиус","Ширина полей шляпы",.55,.3,.9,.05));
    private final NumberSetting height=setting(new NumberSetting("height","Высота","Высота верхушки над полями",.28,.1,.5,.02));
    private final NumberSetting opacity=setting(new NumberSetting("opacity","Непрозрачность","Плотность цветной поверхности в процентах",45,10,85,5));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Использовать цвет темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0x8877FF));
    private List<AbstractClientPlayer> players=List.of();
    private Object world;
    public ChinaHat(){super("china_hat","ChinaHat","Светящаяся шляпа над игроком и друзьями",ModuleCategory.VISUALS);color.visibleWhen(()->!theme.value());}
    @Override protected void onDisable(){players=List.of();world=null;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){onDisable();return;}
        var next=new ArrayList<AbstractClientPlayer>(8);next.add(mc.player);
        var services=dev.sqwat.vela.VelaClient.services();
        if(services!=null&&services.configs().current().friendChinaHat)for(var player:mc.level.players()){
            if(next.size()>=8)break;
            if(player!=mc.player&&player.isAlive()&&!player.isInvisible()&&player.distanceToSqr(mc.player)<=32*32
                &&services.friends().contains(player.getName().getString()))next.add(player);
        }
        players=List.copyOf(next);world=mc.level;
    }
    public void submit(){
        var mc=Minecraft.getInstance();
        if(!enabled()||world!=mc.level||!dev.sqwat.vela.ui.HudVisibility.allowed(mc))return;
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int rgb=(theme.value()?VelaTheme.PRIMARY:color.value())&0xFFFFFF;
        var style=GizmoStyle.fill(NeonGeometry.tint(rgb,opacity.value()/100));
        for(var player:players){
            if(player.isRemoved()||!player.isAlive()||player.isInvisible()||player.isSleeping()||player.isFallFlying())continue;
            if(player==mc.player&&mc.options.getCameraType().isFirstPerson())continue;
            var center=player.getPosition(partial).add(0,player.getBbHeight()+.04,0);
            var apex=center.add(0,height.value(),0);
            for(int i=0;i<NeonGeometry.SEGMENTS;i+=2){
                var a=NeonGeometry.point(center,radius.value(),i);var b=NeonGeometry.point(center,radius.value(),i+2);
                Gizmos.rect(a,apex,apex,b,style);
            }
            NeonGeometry.ring(center,radius.value(),.035,rgb,.9);
        }
    }
}
