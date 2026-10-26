package dev.sqwat.vela.render.effect;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.random.RandomGenerator;

/** Bounded world-local cosmetic particles; movement is independent of render FPS. */
public final class HitParticles {
    public static final int LIMIT=128;
    public static final long LIFETIME=800_000_000L;
    public record Particle(Vec3 origin,Vec3 velocity,long born,float rotation,long lifetime,double gravity,double twist){
        public Particle(Vec3 origin,Vec3 velocity,long born,float rotation){this(origin,velocity,born,rotation,LIFETIME,1.4,0);}
        public Vec3 position(long now){double t=Math.max(0,now-born)/1e9,a=t*twist,c=Math.cos(a),s=Math.sin(a);return origin.add((velocity.x*c-velocity.z*s)*t,velocity.y*t-gravity*t*t,(velocity.x*s+velocity.z*c)*t);}
        public double opacity(long now){double age=(double)(now-born)/lifetime;return age<0||age>=1?0:(1-age)*(1-age);}
    }
    private final ArrayDeque<Particle> particles=new ArrayDeque<>();
    public Iterable<Particle> particles(){return particles;}
    public int size(){return particles.size();}
    public void clear(){particles.clear();}
    public void expire(long now){particles.removeIf(p->now-p.born()>=p.lifetime());}
    public void burst(Vec3 origin,int count,long now,RandomGenerator random){
        burst(origin,count,now,random,LIFETIME,1,1.4,0);
    }
    public void burst(Vec3 origin,int count,long now,RandomGenerator random,long lifetime,double speedScale,double gravity,double twist){
        expire(now);
        for(int i=0;i<Math.clamp(count,0,LIMIT);i++){
            if(particles.size()==LIMIT)particles.removeFirst();
            double angle=random.nextDouble()*Math.PI*2,speed=.6+random.nextDouble()*1.4;
            particles.addLast(new Particle(origin,new Vec3(Math.cos(angle)*speed,.4+random.nextDouble()*1.5,Math.sin(angle)*speed).scale(speedScale),now,(float)angle,lifetime,gravity,twist));
        }
    }
    public static boolean newDamage(int previous,int current){return current>0&&current>previous;}
}
