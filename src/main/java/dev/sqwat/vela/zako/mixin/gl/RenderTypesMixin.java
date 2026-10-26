// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import dev.sqwat.vela.zako.ZakoOptConfig;

@Mixin(RenderTypes.class)
public class RenderTypesMixin {
	@Unique
	private static final Map<Identifier, RenderType> zakoopt$translucent = new ConcurrentHashMap<>();
	@Unique private static long zakoopt$generation = -1;

	// Util.memoize keys on Pair(id, flag) and hashes it through Objects.hash on every call, per entity per frame

	@WrapOperation(method = "entityTranslucent(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
			at = @At(value = "INVOKE", target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private static Object zakoopt$translucent(BiFunction<Object, Object, Object> memo, Object id, Object flag, Operation<Object> original) {
		return zakoopt$lookup(zakoopt$translucent, memo, id, flag, original);
	}

	@Unique
	private static Object zakoopt$lookup(Map<Identifier, RenderType> cache, BiFunction<Object, Object, Object> memo, Object id, Object flag, Operation<Object> original) {
		if (zakoopt$generation != dev.sqwat.vela.zako.IntegrationRuntime.generation) {
			cache.clear(); zakoopt$generation = dev.sqwat.vela.zako.IntegrationRuntime.generation;
		}
		if (!(Boolean) flag || !ZakoOptConfig.lookupCaches()) {
			return original.call(memo, id, flag);
		}
		RenderType type = cache.get(id);
		if (type == null) {
			type = (RenderType) original.call(memo, id, flag);
			if (cache.size() >= 4096) cache.clear();
			cache.put((Identifier) id, type);
		}
		return type;
	}
}
