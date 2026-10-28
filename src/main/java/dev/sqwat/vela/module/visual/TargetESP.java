package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.render.effect.NeonGeometry;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

/** Shares Target HUD selection; performs no entity search or gameplay action. */
public final class TargetESP extends ClientModule {
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Режим","Анимированная подсветка текущей цели","Призраки",List.of("Ромб","Кружок","Crystal","Призраки")));
    private LivingEntity target;
    private long acquired,expires;
    public TargetESP(){super("target_esp","TargetESP","Анимированные эффекты вокруг текущей цели",ModuleCategory.VISUALS);}
    @Override protected void onDisable(){target=null;acquired=expires=0;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();var services=dev.sqwat.vela.VelaClient.services();
        if(mc.level==null||mc.player==null||services==null){onDisable();return;}
        long now=System.nanoTime();
        var selected=services.hudLayout().visualTarget(mc);
        if(selected!=null){if(selected!=target){target=selected;acquired=now;}expires=now+300_000_000L;}
        if(target!=null&&(target.level()!=mc.level||!target.isAlive()||target.isRemoved()||target.isInvisible()||now>=expires))onDisable();
    }
    public static double opacity(long now,long acquired,long expires){
        return Math.min(Math.clamp((now-acquired)/150_000_000d,0,1),Math.clamp((expires-now)/300_000_000d,0,1));
    }
    public void submit(){
        if(!mode.value().equals("Crystal")&&!mode.value().equals("Кружок"))return;
        var mc=Minecraft.getInstance();
        if(!enabled()||target==null||target.level()!=mc.level||target.isRemoved()||!target.isAlive()||target.isInvisible()
            ||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();double alpha=opacity(now,acquired,expires);if(alpha<=0)return;
        var services=dev.sqwat.vela.VelaClient.services();
        boolean reduced=services!=null&&(!services.configs().current().animations||services.configs().current().reducedMotion);
        double time=reduced?0:(now-acquired)/1e9;
        var feet=target.getPosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        var center=feet.add(0,target.getBbHeight()*.55,0);
        int rgb=VelaTheme.PRIMARY&0xFFFFFF;
        double radius=Math.max(.48,target.getBbWidth()*.95);
        switch(mode.value()){
            case "Crystal" -> {
                for(int crystal=0;crystal<16;crystal++){
                    double phase=time*1.5+crystal*Math.PI/8;
                    var middle=center.add(Math.cos(phase)*radius,Math.sin(phase*2)*target.getBbHeight()*.35,Math.sin(phase)*radius);
                    var top=middle.add(0,.15,0);var bottom=middle.add(0,-.15,0);
                    for(int i=0;i<4;i++){
                        double a=phase+i*Math.PI/2,b=a+Math.PI/2;
                        var p=middle.add(Math.cos(a)*.075,0,Math.sin(a)*.075);var q=middle.add(Math.cos(b)*.075,0,Math.sin(b)*.075);
                        var fill=net.minecraft.gizmos.GizmoStyle.fill(NeonGeometry.tint(i%2==0?rgb:NeonGeometry.core(rgb),alpha*.8));
                        net.minecraft.gizmos.Gizmos.rect(top,p,q,top,fill).setAlwaysOnTop();
                        net.minecraft.gizmos.Gizmos.rect(bottom,q,p,bottom,fill).setAlwaysOnTop();
                    }
                }
            }
            case "Кружок" -> {
                var ring=feet.add(0,target.getBbHeight()*(.5+.5*Math.sin(time*2)),0);
                double curtain=.25*Math.cos(time*2);
                for(int i=0;i<32;i++){
                    double a=i*Math.PI/16,b=(i+1)*Math.PI/16;
                    var p=ring.add(Math.cos(a)*radius,0,Math.sin(a)*radius);
                    var q=ring.add(Math.cos(b)*radius,0,Math.sin(b)*radius);
                    line(p,q,rgb,alpha);
                    for(int band=0;band<3;band++)net.minecraft.gizmos.Gizmos.rect(p.add(0,curtain*band/3,0),q.add(0,curtain*band/3,0),q.add(0,curtain*(band+1)/3,0),p.add(0,curtain*(band+1)/3,0),net.minecraft.gizmos.GizmoStyle.fill(NeonGeometry.tint(rgb,alpha*.2*(1-band/3d)))).setAlwaysOnTop();
                }
            }
        }
    }
    private static final net.minecraft.resources.Identifier TARGET=net.minecraft.resources.Identifier.fromNamespaceAndPath("velaclient","textures/effects/target.png");
    private static final net.minecraft.resources.Identifier GLOW=net.minecraft.resources.Identifier.fromNamespaceAndPath("velaclient","textures/effects/glow.png");
    public void draw(net.minecraft.client.gui.GuiGraphicsExtractor g){
        if(mode.value().equals("Кружок"))return;
        var mc=Minecraft.getInstance();
        if(!enabled()||target==null||target.level()!=mc.level||!target.isAlive()||target.isRemoved()||target.isInvisible()||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();double alpha=opacity(now,acquired,expires);if(alpha<=0)return;
        var cfg=dev.sqwat.vela.VelaClient.services().configs().current();
        double time=!cfg.animations||cfg.reducedMotion?0:(now-acquired)/1e9;
        var feet=target.getPosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        var center=feet.add(0,target.getBbHeight()*.55,0);
        var sprites=new dev.sqwat.vela.render.effect.WorldSprite(true);
        if(mode.value().equals("Ромб"))sprites.draw(g,TARGET,1766,1766,feet.add(0,target.getBbHeight()/2,0),
            (float)(1.25+.12*Math.sin(time*2)),(float)(Math.sin(time*.8)*Math.PI),NeonGeometry.tint(VelaTheme.PRIMARY,alpha));
        else if(mode.value().equals("Crystal")){
            double radius=Math.max(.48,target.getBbWidth()*.95);
            for(int crystal=0;crystal<16;crystal++){
                double phase=time*1.5+crystal*Math.PI/8;
                var pos=center.add(Math.cos(phase)*radius,Math.sin(phase*2)*target.getBbHeight()*.35,Math.sin(phase)*radius);
                sprites.draw(g,GLOW,180,180,pos,.34f,0,NeonGeometry.tint(VelaTheme.PRIMARY,alpha*.18));
            }
        }else if(mode.value().equals("Призраки")){
            boolean helix=false;double radius=Math.max(.48,target.getBbWidth()*.95);
            for(int orbit=0;orbit<3;orbit++)for(int tail=9;tail>=0;tail--){
                double phase=time*2.8+orbit*Math.PI*2/3-tail*.10;
                double lift=helix?Math.sin(phase*1.4)*target.getBbHeight()*.40:Math.sin(phase+orbit)*.28;
                var pos=center.add(Math.cos(phase)*radius,lift,Math.sin(phase)*radius);
                double taper=1-tail/11d;
                sprites.draw(g,GLOW,180,180,pos,(float)(.38*taper),0,NeonGeometry.tint(VelaTheme.PRIMARY,alpha*taper*.3));
                sprites.draw(g,GLOW,180,180,pos,(float)(.20*taper),0,NeonGeometry.tint(NeonGeometry.core(VelaTheme.PRIMARY),alpha*taper*.85));
            }
        }
        sprites.flush(g);
    }
    private static void line(net.minecraft.world.phys.Vec3 a,net.minecraft.world.phys.Vec3 b,int rgb,double alpha){
        net.minecraft.gizmos.Gizmos.line(a,b,NeonGeometry.tint(rgb,alpha*.16),6).setAlwaysOnTop();
        net.minecraft.gizmos.Gizmos.line(a,b,NeonGeometry.tint(rgb,alpha),2).setAlwaysOnTop();
    }
}
