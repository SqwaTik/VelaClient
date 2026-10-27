package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ChoiceSetting;
import dev.sqwat.vela.module.setting.ColorSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.render.effect.GlassWave;
import dev.sqwat.vela.render.effect.JumpWaveShape;
import dev.sqwat.vela.render.effect.NeonGeometry;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/**
 * Effects for the mace's smash attack only: the hit vanilla counts as a smash (a mace in hand, falling more than
 * 1.5 blocks, not gliding). A plain mace hit does nothing. One effect per smash, chosen as the mode:
 * <ul>
 * <li>Storm: a volley of vanilla lightning bolts around the impact, sparks and layered thunder;</li>
 * <li>Cracks: the ground splits from a scorched crater, glowing from inside;</li>
 * <li>Liquid glass: a refracting glass ripple spreading over the ground;</li>
 * <li>Tsunami: a fast leaning wall of air and spray with wind streaks rushing past, and a gust.</li>
 * </ul>
 * Debris can be added to any mode. Purely visual and local; the bolts are client-side only.
 */
public final class MaceSmash extends ClientModule {
    static final String STORM="Гроза",CRACKS="Трещины",GLASS="Жидкое стекло",TSUNAMI="Цунами";
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Эффект","Что происходит при сокрушительном ударе",STORM,List.of(STORM,CRACKS,GLASS,TSUNAMI)));
    private final NumberSetting bolts=setting(new NumberSetting("bolts","Молний","Сколько молний бьёт вокруг точки удара",4,1,8,1));
    private final BooleanSetting debris=setting(new BooleanSetting("debris","Осколки","Куски блока под ногами и пыль",true));
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","Сколько секунд держится эффект",1.6,.5,4,.1));
    private final NumberSetting size=setting(new NumberSetting("size","Размер","Радиус эффекта в блоках",3,1,8,.5));
    private final BooleanSetting byHeight=setting(new BooleanSetting("by_height","Сила от высоты","Чем выше падение, тем крупнее эффект",true));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Свечение эффекта в цвете темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0xFF8A3D));

    /** One smash: where, when, how big, which effect, and its crack paths (x,z pairs relative to the centre). */
    private record Impact(Vec3 center,long born,double radius,String mode,List<double[]> cracks){}
    /** A bolt of the storm still to strike: where and on which client tick. */
    private record Bolt(Vec3 at,long tick){}
    private static final int MAX_IMPACTS=6;
    private volatile List<Impact> impacts=List.of();
    private final List<Bolt> pending=new ArrayList<>();
    private final GlassWave glass=new GlassWave();
    private Object world;
    private long ticks;

    public MaceSmash(){
        super("mace_smash","Mace Smash","Гроза, трещины, стекло или цунами при ударе булавой с высоты",ModuleCategory.VISUALS);
        color.visibleWhen(()->!theme.value());
        bolts.visibleWhen(()->mode.value().equals(STORM));
    }
    public static MaceSmash current(){return dev.sqwat.vela.module.Modules.active(MaceSmash.class);}

    /** Called from the attack hook, before vanilla resets the fall distance. */
    public static void attacked(Player player,Entity target){
        var module=current();
        if(module==null||!(player.getMainHandItem().getItem() instanceof MaceItem)||!MaceItem.canSmashAttack(player))return;
        module.smash(player,target);
    }
    private void smash(Player player,Entity target){
        var level=Minecraft.getInstance().level;
        if(level==null)return;
        world=level;
        Vec3 ground=groundBelow(target.position());
        double fall=player.fallDistance;
        double scale=byHeight.value()?Math.clamp(.7+(fall-1.5)/10,.7,1.6):1;
        double radius=size.value()*scale;
        var random=RandomSource.create();
        String chosen=mode.value();
        var next=new ArrayList<>(impacts);
        while(next.size()>=MAX_IMPACTS)next.removeFirst();
        next.add(new Impact(ground,System.nanoTime(),radius,chosen,chosen.equals(CRACKS)?crackPaths(random,radius):List.of()));
        impacts=List.copyOf(next);
        switch(chosen){
            case STORM->storm(level,ground,radius,random);
            case TSUNAMI->gust(level,ground,radius,random);
            case GLASS->level.playLocalSound(ground.x,ground.y,ground.z,SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.PLAYERS,1,.7f,false);
            default->level.playLocalSound(ground.x,ground.y,ground.z,SoundEvents.MACE_SMASH_GROUND_HEAVY,SoundSource.PLAYERS,1,.8f,false);
        }
        if(debris.value())debris(level,ground,radius,random);
    }
    /** Top of the first solid block at or under {@code at} (within 6 blocks); the feet height otherwise. */
    private static Vec3 groundBelow(Vec3 at){
        var level=Minecraft.getInstance().level;
        var pos=BlockPos.containing(at.x,at.y+.2,at.z);
        for(int i=0;i<7;i++,pos=pos.below()){
            var shape=level.getBlockState(pos).getCollisionShape(level,pos);
            if(!shape.isEmpty())return new Vec3(at.x,pos.getY()+shape.max(net.minecraft.core.Direction.Axis.Y),at.z);
        }
        return at;
    }

    // ---- Storm ----
    /** The first bolt hits the impact at once; the rest land around it over half a second, each with its own thunder. */
    private void storm(ClientLevel level,Vec3 at,double radius,RandomSource random){
        strike(level,at);
        sparks(level,at,48,random);
        level.playLocalSound(at.x,at.y,at.z,SoundEvents.LIGHTNING_BOLT_IMPACT,SoundSource.WEATHER,2,.8f,false);
        int count=bolts.value().intValue();
        for(int i=1;i<count;i++){
            double angle=random.nextDouble()*Math.PI*2,distance=radius*(.5+random.nextDouble()*.9);
            Vec3 point=groundBelow(at.add(Math.cos(angle)*distance,1.5,Math.sin(angle)*distance));
            pending.add(new Bolt(point,ticks+2+i*2+random.nextInt(3)));
        }
    }
    /** Vanilla's lightning bolt, visual only and only in this client: flash, bolt and thunder. */
    private static void strike(ClientLevel level,Vec3 at){
        var bolt=net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.TRIGGERED);
        if(bolt==null)return;
        bolt.setVisualOnly(true);
        bolt.snapTo(at.x,at.y,at.z);
        int id=Integer.MAX_VALUE-600;
        while(id>Integer.MAX_VALUE-1100&&level.getEntity(id)!=null)id--;
        bolt.setId(id);
        level.addEntity(bolt);
    }
    private static void sparks(ClientLevel level,Vec3 at,int count,RandomSource random){
        for(int i=0;i<count;i++){
            double a=random.nextDouble()*Math.PI*2,speed=.2+random.nextDouble()*.5;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,at.x,at.y+.2+random.nextDouble()*1.5,at.z,Math.cos(a)*speed,random.nextDouble()*.3,Math.sin(a)*speed);
        }
    }

    // ---- Tsunami ----
    private static void gust(ClientLevel level,Vec3 at,double radius,RandomSource random){
        level.playLocalSound(at.x,at.y,at.z,SoundEvents.WIND_CHARGE_BURST.value(),SoundSource.PLAYERS,1.2f,.75f,false);
        level.playLocalSound(at.x,at.y,at.z,SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),SoundSource.PLAYERS,.9f,.6f,false);
        for(int i=0;i<20;i++){
            double a=i*Math.PI*2/20,speed=.45*radius/3;
            level.addParticle(ParticleTypes.SMALL_GUST,at.x+Math.cos(a),at.y+.3+random.nextDouble()*.6,at.z+Math.sin(a),Math.cos(a)*speed,.02,Math.sin(a)*speed);
            if(i%2==0)level.addParticle(ParticleTypes.CLOUD,at.x+Math.cos(a)*.8,at.y+.15,at.z+Math.sin(a)*.8,Math.cos(a)*speed*2,.01,Math.sin(a)*speed*2);
        }
    }

    // ---- Cracks ----
    /** 6-9 jagged cracks with occasional side branches, as polylines. */
    private static List<double[]> crackPaths(RandomSource random,double radius){
        var paths=new ArrayList<double[]>();
        int count=6+random.nextInt(4);
        for(int i=0;i<count;i++){
            double angle=(i+random.nextDouble()*.6)*Math.PI*2/count;
            double length=radius*(.55+random.nextDouble()*.45);
            paths.add(walk(random,0,0,angle,length,7));
            if(random.nextFloat()<.6f){
                double at=.35+random.nextDouble()*.35;
                double bx=Math.cos(angle)*length*at,bz=Math.sin(angle)*length*at;
                paths.add(walk(random,bx,bz,angle+(random.nextBoolean()?1:-1)*(.5+random.nextDouble()*.5),length*(.3+random.nextDouble()*.25),4));
            }
        }
        return paths;
    }
    private static double[] walk(RandomSource random,double x,double z,double angle,double length,int steps){
        double[] path=new double[(steps+1)*2];path[0]=x;path[1]=z;
        for(int s=1;s<=steps;s++){
            angle+=(random.nextDouble()-.5)*.7;
            x+=Math.cos(angle)*length/steps;z+=Math.sin(angle)*length/steps;
            path[s*2]=x;path[s*2+1]=z;
        }
        return path;
    }

    private static void debris(ClientLevel level,Vec3 at,double radius,RandomSource random){
        var below=BlockPos.containing(at.x,at.y-.5,at.z);var state=level.getBlockState(below);
        if(!state.isAir()){
            var block=new BlockParticleOption(ParticleTypes.BLOCK,state);
            for(int i=0;i<36;i++){
                double a=random.nextDouble()*Math.PI*2,r=random.nextDouble()*radius*.4;
                level.addParticle(block,at.x+Math.cos(a)*r,at.y+.1,at.z+Math.sin(a)*r,Math.cos(a)*.25,.35+random.nextDouble()*.45,Math.sin(a)*.25);
            }
        }
        for(int i=0;i<24;i++){
            double a=i*Math.PI*2/24;
            level.addParticle(ParticleTypes.POOF,at.x+Math.cos(a)*.6,at.y+.1,at.z+Math.sin(a)*.6,Math.cos(a)*.18*radius/3,.02,Math.sin(a)*.18*radius/3);
        }
    }

    @Override protected void onDisable(){impacts=List.of();pending.clear();world=null;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.level==null){onDisable();return;}
        if(world!=null&&world!=mc.level)onDisable();
        ticks++;
        if(!pending.isEmpty()){
            var random=RandomSource.create();
            for(var it=pending.iterator();it.hasNext();){
                var bolt=it.next();
                if(bolt.tick>ticks)continue;
                it.remove();strike(mc.level,bolt.at);sparks(mc.level,bolt.at,16,random);
            }
        }
        if(impacts.isEmpty())return;
        long now=System.nanoTime(),life=(long)(duration.value()*1e9);
        if(impacts.stream().anyMatch(i->now-i.born>=life))impacts=impacts.stream().filter(i->now-i.born<life).toList();
    }
    public boolean active(){return !impacts.isEmpty();}
    private int rgb(){return (theme.value()?VelaTheme.PRIMARY:color.value())&0xFFFFFF;}

    /** World gizmos, once per frame (VisualGizmosMixin): cracks, the storm's ground glow and the tsunami wall. */
    public void submit(){
        var mc=Minecraft.getInstance();
        if(impacts.isEmpty()||world!=mc.level||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        int rgb=rgb();long now=System.nanoTime();double life=duration.value()*1e9;
        for(var impact:impacts){
            float t=(float)((now-impact.born)/life);
            if(t<0||t>=1)continue;
            float fade=1-smooth(Math.max(0,(t-.6f)/.4f));
            switch(impact.mode){
                case CRACKS->drawCracks(impact,Math.min(1,t/.12f),fade,rgb);
                case STORM->{
                    // Charged ground: a flickering ring that fades fast after the strike.
                    float charge=Math.max(0,1-t*2.5f)*(.6f+.4f*(float)Math.abs(Math.sin(t*90)));
                    NeonGeometry.ring(impact.center.add(0,.03,0),impact.radius*(.35+.25*t),Math.max(.07,impact.radius*.04),mixWhite(rgb,.35f),charge);
                }
                case TSUNAMI->drawTsunami(impact,t,rgb);
                case GLASS->{
                    // A faint bright rim on the front of each glass ripple, so the wave reads at a glance.
                    for(int ring=0;ring<2;ring++){
                        double rt=Math.clamp((t-ring*.14)/.86,0,1);
                        if(rt<=0||rt>=1)continue;
                        double r=JumpWaveShape.radius(rt,impact.radius*(ring==0?1.4:.9));
                        NeonGeometry.ring(impact.center.add(0,.035+ring*.005,0),r,Math.max(.05,impact.radius*.025),mixWhite(rgb,.5f),JumpWaveShape.opacity(rt)*(ring==0?.55:.35));
                    }
                }
                default->{}
            }
        }
    }
    /** The liquid-glass ripple: refracting rings drawn with the shared glass surface (HUD pass, every frame). */
    public void draw(net.minecraft.client.gui.GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();
        if(impacts.isEmpty()||world!=mc.level||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();double life=duration.value()*1e9;int rgb=rgb();
        g.pose().pushMatrix();g.pose().identity();
        try{for(var impact:impacts){
            if(!impact.mode.equals(GLASS))continue;
            double t=(now-impact.born)/life;
            for(int ring=0;ring<2;ring++){
                double rt=Math.clamp((t-ring*.14)/.86,0,1);
                if(rt<=0||rt>=1)continue;
                double r=JumpWaveShape.radius(rt,impact.radius*(ring==0?1.4:.9));
                glass.draw(g,impact.center.add(0,.04+ring*.01,0),new Vec3(1,0,0),new Vec3(0,0,1),r,NeonGeometry.tint(rgb,JumpWaveShape.opacity(rt)*(ring==0?.95:.7)));
            }
        }}finally{g.pose().popMatrix();}
    }

    /**
     * A wall of air and spray that outruns the eye: it shoots out to twice the radius in the first 40% of the effect,
     * leans forward with a bright crest, shrinks as it goes, and wind streaks race past it.
     */
    private static void drawTsunami(Impact impact,float t,int rgb){
        float travel=1-(float)Math.pow(1-Math.min(1,t/.4f),3);
        double r=impact.radius*(.3+1.9*travel);
        float fade=1-smooth(Math.max(0,(t-.35f)/.65f));
        if(fade<=0)return;
        double height=impact.radius*.55*Math.pow(1-t,.6);
        double lean=height*.35;
        int water=mixWhite(rgb,.65f);
        var body=GizmoStyle.fill(NeonGeometry.tint(water,.34*fade));
        var foam=GizmoStyle.fill(NeonGeometry.tint(0xFFFFFF,.75*fade));
        double y=impact.center.y+.02;
        int segments=NeonGeometry.SEGMENTS;
        for(int i=0;i<segments;i++){
            Vec3 a0=NeonGeometry.point(impact.center,r,i),a1=NeonGeometry.point(impact.center,r,i+1);
            Vec3 b0=NeonGeometry.point(impact.center,r+lean,i),b1=NeonGeometry.point(impact.center,r+lean,i+1);
            Vec3 c0=NeonGeometry.point(impact.center,r+lean*1.15,i),c1=NeonGeometry.point(impact.center,r+lean*1.15,i+1);
            // Body from the ground to just under the crest, leaning outwards.
            Gizmos.rect(at(a0,y),at(a1,y),at(b1,y+height*.85),at(b0,y+height*.85),body);
            // Foamy crest curling over the front.
            Gizmos.rect(at(b0,y+height*.85),at(b1,y+height*.85),at(c1,y+height),at(c0,y+height),foam);
        }
        // Wind streaks: thin lines racing outwards ahead of and through the wall.
        for(int i=0;i<40;i++){
            double angle=i*2.399963; // golden angle spreads them evenly
            double phase=(i*.137+t*2.6)%1.0;
            double from=r*(.55+phase*.7),to=from+.6+impact.radius*.18;
            double h=y+.15+((i*.618)%1.0)*Math.max(.4,height*1.1);
            Vec3 a=new Vec3(impact.center.x+Math.cos(angle)*from,h,impact.center.z+Math.sin(angle)*from);
            Vec3 b=new Vec3(impact.center.x+Math.cos(angle)*to,h,impact.center.z+Math.sin(angle)*to);
            Gizmos.line(a,b,NeonGeometry.tint(0xFFFFFF,.55*fade*(1-phase)),1.6f);
        }
    }
    private static Vec3 at(Vec3 point,double y){return new Vec3(point.x,y,point.z);}
    private static int mixWhite(int rgb,float amount){return VelaTheme.mix(0xFF000000|rgb,0xFFFFFFFF,amount)&0xFFFFFF;}

    /** Dark fissure with a glowing core; each crack grows out from the centre, thinner towards its tip. */
    private static void drawCracks(Impact impact,float grow,float fade,int rgb){
        var dark=GizmoStyle.fill(NeonGeometry.tint(0x140C08,.85*fade));
        var glow=GizmoStyle.fill(NeonGeometry.tint(rgb,.9*fade));
        var hot=GizmoStyle.fill(NeonGeometry.tint(NeonGeometry.core(rgb),fade));
        double y=impact.center.y+.025,cx=impact.center.x,cz=impact.center.z;
        double baseWidth=Math.max(.14,impact.radius*.075);
        // Scorched patch under the impact, so the cracks start from a crater rather than a point.
        var scorch=GizmoStyle.fill(NeonGeometry.tint(0x0E0806,.55*fade));
        double patch=impact.radius*.28*grow;
        for(int i=0;i<NeonGeometry.SEGMENTS;i+=2){
            Vec3 a=NeonGeometry.point(impact.center.add(0,.02,0),patch,i),b=NeonGeometry.point(impact.center.add(0,.02,0),patch,Math.min(NeonGeometry.SEGMENTS,i+2));
            Gizmos.rect(new Vec3(cx,y-.005,cz),a,b,new Vec3(cx,y-.005,cz),scorch);
        }
        for(double[] path:impact.cracks){
            int segments=path.length/2-1;
            double shown=segments*grow;
            for(int s=0;s<segments&&s<shown;s++){
                double part=Math.min(1,shown-s);
                double x0=path[s*2],z0=path[s*2+1],x1=x0+(path[s*2+2]-x0)*part,z1=z0+(path[s*2+3]-z0)*part;
                double w0=baseWidth*(1-(double)s/(segments+1)),w1=baseWidth*(1-(s+part)/(segments+1));
                quad(cx,y,cz,x0,z0,x1,z1,w0,w1,dark);
                quad(cx,y+.002,cz,x0,z0,x1,z1,w0*.4,w1*.4,glow);
                quad(cx,y+.004,cz,x0,z0,x1,z1,w0*.15,w1*.15,hot);
            }
        }
    }
    private static void quad(double cx,double y,double cz,double x0,double z0,double x1,double z1,double w0,double w1,GizmoStyle style){
        double dx=x1-x0,dz=z1-z0,len=Math.sqrt(dx*dx+dz*dz);
        if(len<1e-4)return;
        double nx=-dz/len,nz=dx/len;
        Gizmos.rect(new Vec3(cx+x0+nx*w0/2,y,cz+z0+nz*w0/2),new Vec3(cx+x0-nx*w0/2,y,cz+z0-nz*w0/2),
            new Vec3(cx+x1-nx*w1/2,y,cz+z1-nz*w1/2),new Vec3(cx+x1+nx*w1/2,y,cz+z1+nz*w1/2),style);
    }
    private static float smooth(float t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
}
