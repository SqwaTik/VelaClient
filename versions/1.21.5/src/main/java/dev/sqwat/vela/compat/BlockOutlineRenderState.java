package dev.sqwat.vela.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.21.11's extracted block outline: what BlockOverlay draws on 1.21.5, where the outline is never extracted. */
public record BlockOutlineRenderState(BlockPos pos,VoxelShape shape){}
