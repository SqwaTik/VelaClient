// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.entity;

import dev.sqwat.vela.zako.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.sqwat.vela.zako.entity.CachedStack;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.immediate.model.EntityRenderer", remap = false)
public class SodiumEntityRendererMixin {
	// stackPush() looks the stack up through a ThreadLocal on every cuboid, and the render thread's map misses a lot.
	// WrapOperation, not Redirect: MixinExtras 0.5.4 (Fabric Loader 0.19.3) crashes wrapping a static @Redirect here
	@WrapOperation(method = "renderCuboid", at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryStack;stackPush()Lorg/lwjgl/system/MemoryStack;"))
	private static MemoryStack zakoopt$cachedStack(Operation<MemoryStack> original) {
		return Features.MICRO_OPTS.on ? CachedStack.push() : original.call();
	}
}
