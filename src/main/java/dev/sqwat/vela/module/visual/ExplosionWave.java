package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.render.effect.GlassWave;
import dev.sqwat.vela.render.effect.JumpWaveShape;
import dev.sqwat.vela.render.effect.NeonGeometry;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;

/** Client-side wave for confirmed explosions; no world scans or additional scene capture. */
public final class ExplosionWave extends ClientModule {
    public static final int LIMIT=4;
    private record Wave(Vec3 center,long born,long life,double radius){}
    private final ArrayDeque<Wave> waves=new ArrayDeque<>();
    private final GlassWave renderer=new GlassWave();
    private final NumberSetting radius=setting(new NumberSetting("radius","Радиус","Размер волны в блоках",6,2,15,.5));
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","Время расширения волны в секундах",1.2,.4,3,.1));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Окрашивать волну цветом темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0x8877FF));
    private Object world;
    public ExplosionWave(){super("explosion_wave","Explosion Wave","Стеклянная ударная волна от взрывов",ModuleCategory.VISUALS);color.visibleWhen(()->!theme.value());}
    public static void exploded(Vec3 center){
        var mc=Minecraft.getInstance();var services=dev.sqwat.vela.VelaClient.services();
        if(services==null||mc.level==null||mc.player==null||center.distanceToSqr(mc.player.position())>64*64)return;
        if(!(services.modules().byId("explosion_wave") instanceof ExplosionWave module)||!module.enabled())return;
        if(module.world!=mc.level){module.onDisable();module.world=mc.level;}
        module.spawn(center,System.nanoTime());
    }
    public void spawn(Vec3 center,long now){
        if(!enabled()||!Double.isFinite(center.x)||!Double.isFinite(center.y)||!Double.isFinite(center.z))return;
        expire(now);while(waves.size()>=LIMIT)waves.removeFirst();
        waves.addLast(new Wave(center,now,(long)(duration.value()*1e9),radius.value()));
    }
    public int count(){return waves.size();}
    public void expire(long now){waves.removeIf(wave->now-wave.born>=wave.life);}
    @Override protected void onDisable(){waves.clear();world=null;}
    @Override public void onClientTick(){
        var level=Minecraft.getInstance().level;if(world!=level){onDisable();world=level;}expire(System.nanoTime());
    }
    public void draw(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();
        if(!enabled()||waves.isEmpty()||mc.level==null||world!=mc.level||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();var rotation=mc.gameRenderer.mainCamera().rotation();
        var right=new org.joml.Vector3f(1,0,0).rotate(rotation);var up=new org.joml.Vector3f(0,1,0).rotate(rotation);
        var x=new Vec3(right.x,right.y,right.z);var y=new Vec3(up.x,up.y,up.z);
        int tint=theme.value()?VelaTheme.PRIMARY:color.value();
        g.pose().pushMatrix();g.pose().identity();
        try{for(var wave:waves){
            double progress=(double)(now-wave.born)/wave.life;if(progress<0||progress>=1)continue;
            renderer.draw(g,wave.center,x,y,JumpWaveShape.radius(progress,wave.radius),
                NeonGeometry.tint(tint,JumpWaveShape.opacity(progress)*.95));
        }}finally{g.pose().popMatrix();}
    }
}
