package dev.sqwat.vela.mixin.perf;

import dev.sqwat.vela.render.perf.DistantChunks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opaque far leaves hide the faces between them, as vanilla's own opaque leaves do. */
@Mixin(LeavesBlock.class)
public abstract class DistantLeavesMixin {
    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
    private void vela$cullFarLeaves(BlockState state, BlockState neighborState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (neighborState.getBlock() instanceof LeavesBlock && DistantChunks.simplifying()) cir.setReturnValue(true);
    }
}
