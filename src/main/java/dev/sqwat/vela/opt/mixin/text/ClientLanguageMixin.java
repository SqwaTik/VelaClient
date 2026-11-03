package dev.sqwat.vela.opt.mixin.text;

import dev.sqwat.vela.opt.feature.Features;

import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.IdentityHashMap;
import java.util.Map;
import dev.sqwat.vela.opt.text.LruMap;
import dev.sqwat.vela.opt.text.StableText;

@Mixin(ClientLanguage.class)
public class ClientLanguageMixin {
	@Unique
	private final Map<Component, FormattedCharSequence> velaopt$visualOrder = new LruMap<>(1024);
	// Component.hashCode walks the whole tree; most lookups are the same instance as last frame
	@Unique
	private final Map<Component, FormattedCharSequence> velaopt$byIdentity = new IdentityHashMap<>();

	@Inject(method = "getVisualOrder", at = @At("HEAD"), cancellable = true)
	private void velaopt$cached(FormattedText text, CallbackInfoReturnable<FormattedCharSequence> cir) {
		if (!(text instanceof Component component) || !Features.BIDI_CACHE.on) {
			return;
		}
		boolean identity = Features.MICRO_OPTS.stableResults;
		FormattedCharSequence cached;
		synchronized (velaopt$visualOrder) {
			cached = identity ? velaopt$byIdentity.get(component) : null;
			if (cached == null) {
				cached = velaopt$visualOrder.get(component);
				if (cached != null && identity) {
					velaopt$remember(component, cached);
				}
			}
		}
		if (cached != null) {
			cir.setReturnValue(cached);
		}
	}

	@Inject(method = "getVisualOrder", at = @At("RETURN"), cancellable = true)
	private void velaopt$store(FormattedText text, CallbackInfoReturnable<FormattedCharSequence> cir) {
		if (text instanceof Component component && Features.BIDI_CACHE.on) {
			FormattedCharSequence stable = StableText.mark(cir.getReturnValue());
			synchronized (velaopt$visualOrder) {
				velaopt$visualOrder.put(component, stable);
				if (Features.MICRO_OPTS.stableResults) {
					velaopt$remember(component, stable);
				}
			}
			cir.setReturnValue(stable);
		}
	}

	@Unique
	private void velaopt$remember(Component component, FormattedCharSequence value) {
		// ponytail: wholesale clear instead of LRU, rebuilt from the equals map within a frame
		if (velaopt$byIdentity.size() >= 2048) {
			velaopt$byIdentity.clear();
		}
		velaopt$byIdentity.put(component, value);
	}
}
