package dev.sqwat.vela.render;

import net.minecraft.world.phys.Vec3;

/** ThrowableProjectile in 26.2 applies gravity, then drag, then movement. */
public final class ThrowPhysics {
    private ThrowPhysics(){}
    public static Vec3 launch(float yaw,float pitch,float pitchOffset,double speed,Vec3 movement,boolean grounded){
        double y=Math.toRadians(yaw),p=Math.toRadians(pitch);
        return new Vec3(-Math.sin(y)*Math.cos(p),-Math.sin(Math.toRadians(pitch+pitchOffset)),Math.cos(y)*Math.cos(p))
            .normalize().scale(speed).add(movement.x,grounded?0:movement.y,movement.z);
    }
    public static Vec3 velocity(Vec3 current,double gravity,boolean water){
        return current.add(0,-gravity,0).scale(water?.8:.99);
    }
}
