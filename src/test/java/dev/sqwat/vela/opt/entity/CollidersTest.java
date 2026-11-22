package dev.sqwat.vela.opt.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.vehicle.boat.Boat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollidersTest {
    @Test void onlyTypesThatCanBeBumpedIntoAreKept(){
        // Boats inherit the override from AbstractBoat; the others declare their own.
        assertTrue(Colliders.collidable(Boat.class));
        assertTrue(Colliders.collidable(Shulker.class));
        assertTrue(Colliders.collidable(HappyGhast.class));
        assertFalse(Colliders.collidable(Pig.class));
        assertFalse(Colliders.collidable(Entity.class));
    }
}
