package dev.sqwat.vela.mixin.perf;

import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryEventSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No Microsoft telemetry sessions: no event building, logging or upload threads for world loads and performance samples. */
@Mixin(ClientTelemetryManager.class)
abstract class TelemetryOffMixin {
    @Inject(method = "createEventSender", at = @At("HEAD"), cancellable = true)
    private void vela$disabled(CallbackInfoReturnable<TelemetryEventSender> ci) {
        ci.setReturnValue(TelemetryEventSender.DISABLED);
    }
}
