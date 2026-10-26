package dev.sqwat.vela.render.effect;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.chunk.LevelChunk;

/** Read-only collision view; never loads a chunk or starts cache I/O. */
public final class TrajectoryTerrain implements BlockGetter {
    private final ClientLevel level;
    private final de.johni0702.minecraft.bobby.FakeChunkManager cache;
    public TrajectoryTerrain(ClientLevel level){
        this.level=level;
        cache=dev.sqwat.vela.module.utility.Far.active()?((de.johni0702.minecraft.bobby.ext.ClientChunkCacheExt)level.getChunkSource()).bobby_getFakeChunkManager():null;
    }
    private LevelChunk cached(BlockPos pos){return cache==null?null:cache.getChunk(pos.getX()>>4,pos.getZ()>>4);}
    public boolean known(BlockPos pos){return level.hasChunkAt(pos)||cached(pos)!=null;}
    @Override public BlockState getBlockState(BlockPos pos){
        if(level.hasChunkAt(pos))return level.getBlockState(pos);
        var chunk=cached(pos);return chunk==null?Blocks.AIR.defaultBlockState():chunk.getBlockState(pos);
    }
    @Override public FluidState getFluidState(BlockPos pos){return getBlockState(pos).getFluidState();}
    @Override public BlockEntity getBlockEntity(BlockPos pos){return level.hasChunkAt(pos)?level.getBlockEntity(pos):null;}
    @Override public int getHeight(){return level.getHeight();}
    @Override public int getMinY(){return level.getMinY();}
}
