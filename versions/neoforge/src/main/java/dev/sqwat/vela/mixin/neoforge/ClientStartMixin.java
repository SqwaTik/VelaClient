package dev.sqwat.vela.mixin.neoforge;

import dev.sqwat.vela.neoforge.NeoMod;
import net.minecraft.client.Minecraft;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge: mods are built before the game client exists, so the Fabric entrypoints run where Fabric runs them, right after
 * Minecraft.getInstance() starts answering (before the options load their keys and the mod loading finishes).
 */
@Mixin(Minecraft.class)
abstract class ClientStartMixin {
    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;instance:Lnet/minecraft/client/Minecraft;", opcode = Opcodes.PUTSTATIC, shift = At.Shift.AFTER))
    private void vela$initializeClient(CallbackInfo ci) {
        NeoMod.initializeClient();
    }
}
