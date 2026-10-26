package dev.sqwat.vela.render.effect;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Bounded ambient field. Positions are world anchored, never attached to the camera. */
public final class WorldParticleField {
    public static final int LIMIT=128;
    private static final long LIFETIME=8_000_000_000L;
    private final ArrayList<Particle> particles=new ArrayList<>(LIMIT);
    private final List<Particle> view=Collections.unmodifiableList(particles);
    private Vec3 anchor;
    public static final class Particle {
        private final Vec3 origin;
        private final long born;
        public final int shape;
        public final float phase;
        public boolean visible;
        private Particle(Vec3 origin,long born,int shape,float phase){this.origin=origin;this.born=born;this.shape=shape;this.phase=phase;}
        public Vec3 position(long now){
            double age=Math.max(0,(now-born)/1e9);
            return origin.add(Math.sin(age*.7+phase)*.35, -age*.16,Math.cos(age*.6+phase)*.35);
        }
        public double opacity(long now){
            double age=(now-born)/(double)LIFETIME;
            if(age<0||age>=1)return 0;
            return Math.min(1,Math.min(age/.1,(1-age)/.2));
        }
    }
    public void tick(Vec3 center,int requested,double radius,long now,Random random){
        if(anchor!=null&&anchor.distanceToSqr(center)>radius*radius*4)clear();
        anchor=center;
        particles.removeIf(p->now-p.born>=LIFETIME||p.origin.distanceToSqr(center)>radius*radius*4);
        int target=Math.max(0,Math.min(LIMIT,requested));
        while(particles.size()>target)particles.removeLast();
        for(int i=0,n=Math.min(4,target-particles.size());i<n;i++){
            double angle=random.nextDouble()*Math.PI*2,r=Math.sqrt(random.nextDouble())*radius;
            particles.add(new Particle(center.add(Math.cos(angle)*r,random.nextDouble()*6-1,Math.sin(angle)*r),now,random.nextInt(ParticleAtlas.COUNT),random.nextFloat()*6.283185f));
        }
    }
    public List<Particle> particles(){return view;}
    public void clear(){particles.clear();anchor=null;}
}
