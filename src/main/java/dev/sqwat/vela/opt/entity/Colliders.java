package dev.sqwat.vela.opt.entity;

import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * The client-side entities other entities can bump into. Vanilla answers "what entity shapes are in this box" by walking
 * every entity of the overlapping sections and asking each {@code canBeCollidedWith}; only a handful of types (boats,
 * shulkers, the happy ghast, and whatever mods add) ever say yes. Every interpolating entity asks this each tick, so a
 * crowd of 6,000 pigs made it a fifth of the client thread. The level keeps the entities whose class can say yes and
 * asks only those: the same answer, without the crowd walk.
 */
public final class Colliders {
    private Colliders(){}

    /** Whether instances of this class may ever be collided with: they override {@code Entity.canBeCollidedWith}. */
    private static final ClassValue<Boolean> COLLIDABLE=new ClassValue<>(){
        @Override protected Boolean computeValue(Class<?> type){
            for(Class<?> c=type;c!=null&&c!=Entity.class;c=c.getSuperclass()){
                try{c.getDeclaredMethod("canBeCollidedWith",Entity.class);return true;}
                catch(NoSuchMethodException ignored){}
                catch(LinkageError|SecurityException error){return true;}
            }
            return false;
        }
    };

    public static boolean collidable(Class<?> type){
        return COLLIDABLE.get(type);
    }

    /** The level's collidable entities, kept on the client thread by the level's tracking callbacks. */
    public interface Holder {
        List<Entity> vela$colliders();
    }

    public static void tracked(Entity entity){
        if(collidable(entity.getClass())&&entity.level() instanceof Holder holder){
            var list=holder.vela$colliders();
            if(!list.contains(entity))list.add(entity);
        }
    }

    public static void untracked(Entity entity){
        if(collidable(entity.getClass())&&entity.level() instanceof Holder holder)holder.vela$colliders().remove(entity);
    }

    public static List<Entity> newList(){
        return new ArrayList<>();
    }
}
