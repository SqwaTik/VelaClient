package dev.sqwat.vela.opt.mixin.entity;

import dev.sqwat.vela.opt.entity.Colliders;
import dev.sqwat.vela.opt.feature.Features;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Entity collision shapes on the client from the level's few collidable entities: see {@link Colliders}. */
@Mixin(ClientLevel.class)
public abstract class ClientEntityCollisionsMixin implements Colliders.Holder {
    @Unique private final List<Entity> vela$colliders=Colliders.newList();

    @Override public List<Entity> vela$colliders(){return vela$colliders;}

    public List<VoxelShape> getEntityCollisions(final @Nullable Entity source,final AABB testArea){
        if(testArea.getSize()<1.0E-7)return List.of();
        Predicate<Entity> canCollide=source==null?EntitySelector.CAN_BE_COLLIDED_WITH:EntitySelector.NO_SPECTATORS.and(source::canCollideWith);
        AABB box=testArea.inflate(1.0E-7);
        if(!Features.active){
            // Vanilla: every entity of the overlapping sections.
            List<Entity> hits=((Level)(Object)this).getEntities(source,box,canCollide);
            if(hits.isEmpty())return List.of();
            List<VoxelShape> shapes=new ArrayList<>(hits.size());
            for(Entity e:hits)shapes.add(Shapes.create(e.getBoundingBox()));
            return shapes;
        }
        if(vela$colliders.isEmpty())return List.of();
        List<VoxelShape> shapes=null;
        for(int i=0;i<vela$colliders.size();i++){
            Entity e=vela$colliders.get(i);
            if(e==source||e.isRemoved()||!e.getBoundingBox().intersects(box)||!canCollide.test(e))continue;
            if(shapes==null)shapes=new ArrayList<>(2);
            shapes.add(Shapes.create(e.getBoundingBox()));
        }
        return shapes==null?List.of():shapes;
    }
}
