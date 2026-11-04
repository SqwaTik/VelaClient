package dev.sqwat.vela.mixin.world;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sqwat.vela.compat.BlockOutlineRenderState;
import dev.sqwat.vela.compat.gizmos.Gizmos;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 1.21.5: the outline is drawn twice (solid and translucent pass) straight from the hit result; the overlay replaces both. */
@Mixin(LevelRenderer.class)
abstract class BlockOverlayMixin {
    @Inject(method="renderBlockOutline",at=@At("HEAD"),cancellable=true)
    private void vela$blockOverlay(Camera camera,MultiBufferSource.BufferSource buffers,PoseStack pose,boolean translucent,CallbackInfo ci){
        var overlay=dev.sqwat.vela.module.utility.BlockOverlay.active();
        if(overlay==null)return;
        ci.cancel();
        var mc=Minecraft.getInstance();
        if(translucent||mc.level==null||!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()==HitResult.Type.MISS)return;
        var pos=hit.getBlockPos();var block=mc.level.getBlockState(pos);
        if(block.isAir()||!mc.level.getWorldBorder().isWithinBounds(pos))return;
        Gizmos.begin();
        try{overlay.submit(new BlockOutlineRenderState(pos,block.getShape(mc.level,pos,CollisionContext.of(camera.getEntity()))));}
        finally{var at=camera.getPosition();Gizmos.draw(pose,buffers,at.x,at.y,at.z);}
    }
}
