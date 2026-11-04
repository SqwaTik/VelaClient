package dev.sqwat.vela.mixin.entity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.sqwat.vela.render.entity.AutomaticVisibility;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** 1.21.5: block entities are drawn straight from BlockEntityRenderDispatcher.render. */
@Mixin(BlockEntityRenderDispatcher.class)
abstract class StorageVisibilityMixin {
    @ModifyExpressionValue(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"))
    private BlockEntityRenderer<?> vela$skipInvisibleStorage(BlockEntityRenderer<?> renderer,BlockEntity entity){
        return renderer!=null&&AutomaticVisibility.outsideView(entity,false)?null:renderer;
    }
}
