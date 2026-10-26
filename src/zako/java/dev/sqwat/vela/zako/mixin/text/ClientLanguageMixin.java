// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
package dev.sqwat.vela.zako.mixin.text;

import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.sqwat.vela.zako.ZakoOptConfig;
import dev.sqwat.vela.zako.IntegrationRuntime;
import dev.sqwat.vela.zako.text.VisualOrderCache;
import dev.sqwat.vela.zako.text.StableText;

@Mixin(ClientLanguage.class)
public class ClientLanguageMixin {
    @Unique private final VisualOrderCache zakoopt$visualOrder=new VisualOrderCache();
    @Unique private long zakoopt$generation=-1;
    @Inject(method="getVisualOrder",at=@At("HEAD"),cancellable=true)
    private void zakoopt$cached(FormattedText text,CallbackInfoReturnable<FormattedCharSequence> cir){
        if(zakoopt$generation!=IntegrationRuntime.generation){zakoopt$visualOrder.clear();zakoopt$generation=IntegrationRuntime.generation;}
        if(!(text instanceof Component)||!ZakoOptConfig.bidiCache())return;
        var cached=zakoopt$visualOrder.get(text);if(cached!=null)cir.setReturnValue(cached);
    }
    @Inject(method="getVisualOrder",at=@At("RETURN"),cancellable=true)
    private void zakoopt$store(FormattedText text,CallbackInfoReturnable<FormattedCharSequence> cir){
        if(text instanceof Component&&ZakoOptConfig.bidiCache()){
            var stable=StableText.mark(cir.getReturnValue());zakoopt$visualOrder.put(text,stable);cir.setReturnValue(stable);
        }
    }
}
