package dev.sqwat.vela.opt.mixin.entity;

import dev.sqwat.vela.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.sqwat.vela.opt.entity.PooledState;

// since submits are deferred every entity gets a new render state each frame; one per entity is enough, extraction writes
// every field anew (1.21.8 shared one state across all entities of a renderer). Kept per renderer, as a player's can change
@Mixin(EntityRenderer.class)
public abstract class EntityRenderStatePoolMixin {
	@WrapOperation(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState()Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
	private EntityRenderState velaopt$pooled(EntityRenderer<?, ?> self, Operation<EntityRenderState> original, @Local(argsOnly = true) Entity entity) {
		if (!Features.STATE_POOL.on) {
			return original.call(self);
		}
		PooledState pooled = (PooledState) entity;
		if (pooled.velaopt$stateOwner() == self) {
			return (EntityRenderState) pooled.velaopt$state();
		}
		EntityRenderState state = original.call(self);
		pooled.velaopt$setState(state, self);
		return state;
	}
}
