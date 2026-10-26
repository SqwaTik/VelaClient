// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.entity;

import dev.sqwat.vela.zako.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.sqwat.vela.zako.entity.CachedName;

@Mixin(EntityRenderer.class)
public class EntityRendererNameMixin {
	// a player's name is rebuilt (team prefix + name + suffix) every frame; the same instance keeps the text caches hitting by identity
	@WrapOperation(method = "getNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getDisplayName()Lnet/minecraft/network/chat/Component;"))
	private Component zakoopt$cachedName(Entity entity, Operation<Component> original) {
		if (!Features.MICRO_OPTS.fewerChecks) {
			return original.call(entity);
		}
		CachedName cache = (CachedName) entity;
		long tick = entity.level().getGameTime();
		if (cache.zakoopt$nameTick() != tick) {
			cache.zakoopt$setName(original.call(entity), tick);
		}
		return cache.zakoopt$name();
	}
}
