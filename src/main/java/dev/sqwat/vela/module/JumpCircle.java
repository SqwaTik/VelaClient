package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class JumpCircle extends ClientModule {
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Режим","Вид эффекта прыжка","Кольцо",List.of("Кольцо","Ударная волна")));
    private final NumberSetting radius=setting(new NumberSetting("radius","Радиус","Размер круга в конце анимации",1.2,.5,3,.1));
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","Длительность круга в секундах",1,.3,3,.1));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Использовать цвет темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0x8877FF));
    private final dev.sqwat.vela.render.GlassWave glassWave=new dev.sqwat.vela.render.GlassWave();
    private record Circle(Vec3 center,long born){}
    private volatile List<Circle> circles=List.of();
    private Object world;private boolean grounded;private Vec3 lastPosition;
    private record FriendJump(boolean grounded,Vec3 position){}
    private final Map<UUID,FriendJump> friendJumps=new HashMap<>();
    public JumpCircle(){super("jump_circle","JumpCircle","Круг или стеклянная ударная волна при прыжке",ModuleCategory.VISUALS);color.visibleWhen(()->!theme.value());}
    @Override protected void onDisable(){circles=List.of();world=null;lastPosition=null;friendJumps.clear();}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){onDisable();return;}
        if(world!=mc.level){onDisable();world=mc.level;grounded=mc.player.onGround();}
        long now=System.nanoTime();long life=(long)(duration.value()*1e9);
        var next=new ArrayList<Circle>(circles.size()+1);
        for(var circle:circles)if(now-circle.born<life)next.add(circle);
        if(grounded&&!mc.player.onGround()&&mc.player.getDeltaMovement().y>0&&lastPosition!=null){
            while(next.size()>=dev.sqwat.vela.render.JumpWaveShape.MAX_WAVES)next.removeFirst();
            next.add(new Circle(lastPosition.add(0,.025,0),now));
        }
        var services=dev.sqwat.vela.VelaClient.services();Set<UUID> present=new HashSet<>();
        if(services!=null&&services.configs().current().friendJumpCircle){
            for(var player:mc.level.players()){
                if(present.size()>=8)break;
                if(player==mc.player||!player.isAlive()||player.isInvisible()||player.distanceToSqr(mc.player)>32*32||!services.friends().contains(player.getName().getString()))continue;
                present.add(player.getUUID());var old=friendJumps.put(player.getUUID(),new FriendJump(player.onGround(),player.position()));
                if(old!=null&&old.grounded&&!player.onGround()&&player.getY()>old.position.y+.01&&player.position().distanceToSqr(old.position)<4){
                    while(next.size()>=dev.sqwat.vela.render.JumpWaveShape.MAX_WAVES)next.removeFirst();
                    next.add(new Circle(old.position.add(0,.025,0),now));
                }
            }
        }
        friendJumps.keySet().retainAll(present);
        circles=List.copyOf(next);grounded=mc.player.onGround();lastPosition=mc.player.position();
    }
    public void submit(){
        var mc=Minecraft.getInstance();
        if(!enabled()||!mode.value().equals("Кольцо")||world!=mc.level||!dev.sqwat.vela.ui.HudVisibility.allowed(mc))return;
        int rgb=(theme.value()?VelaTheme.PRIMARY:color.value())&0xFFFFFF;long now=System.nanoTime();
        for(var circle:circles){
            float t=(float)((now-circle.born)/(duration.value()*1e9));
            if(t<0||t>=1)continue;
            double r=dev.sqwat.vela.render.JumpWaveShape.radius(t,radius.value());
            dev.sqwat.vela.render.NeonGeometry.ring(circle.center,r,Math.min(.09,r*.16),rgb,
                dev.sqwat.vela.render.JumpWaveShape.opacity(t));
        }
    }
    public void draw(net.minecraft.client.gui.GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();
        if(!enabled()||!mode.value().equals("Ударная волна")||world!=mc.level||circles.isEmpty()
            ||!dev.sqwat.vela.ui.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();int rgb=(theme.value()?VelaTheme.PRIMARY:color.value())&0xFFFFFF;
        g.pose().pushMatrix();g.pose().identity();
        try{for(var circle:circles){
            double t=(now-circle.born)/(duration.value()*1e9);if(t<0||t>=1)continue;
            glassWave.draw(g,circle.center,new Vec3(1,0,0),new Vec3(0,0,1),
                dev.sqwat.vela.render.JumpWaveShape.radius(t,radius.value()),
                dev.sqwat.vela.render.NeonGeometry.tint(rgb,dev.sqwat.vela.render.JumpWaveShape.opacity(t)*245/255));
        }}finally{g.pose().popMatrix();}
    }
}
