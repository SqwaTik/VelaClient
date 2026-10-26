package dev.sqwat.vela.mixin.entity;

import dev.sqwat.vela.render.entity.AutomaticVisibility;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(BlockEntityRenderDispatcher.class)
abstract class StorageVisibilityMixin {
    @ModifyExpressionValue(method="tryExtractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"))
    private BlockEntityRenderer<?,?> vela$skipInvisibleStorage(BlockEntityRenderer<?,?> renderer,BlockEntity entity,float delta,ModelFeatureRenderer.CrumblingOverlay breaking,boolean global){
        return renderer!=null&&breaking==null&&AutomaticVisibility.outsideView(entity,global)?null:renderer;
    }
}
