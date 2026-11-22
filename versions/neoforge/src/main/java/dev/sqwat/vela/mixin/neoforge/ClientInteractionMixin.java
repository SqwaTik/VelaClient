package dev.sqwat.vela.mixin.neoforge;

import dev.sqwat.vela.neoforge.shim.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import dev.sqwat.vela.neoforge.shim.fabric.api.event.player.AttackEntityCallback;
import dev.sqwat.vela.neoforge.shim.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NeoForge: Fabric's client interaction callbacks, where Fabric API fires them (before the packet is sent). */
@Mixin(MultiPlayerGameMode.class)
abstract class ClientInteractionMixin {
    @Shadow @Final private Minecraft minecraft;
    @Unique private BlockState vela$broken;

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void vela$attack(Player player, Entity target, CallbackInfo ci) {
        for (var l : AttackEntityCallback.EVENT.listeners())
            if (l.interact(player, player.level(), InteractionHand.MAIN_HAND, target, null) != InteractionResult.PASS) { ci.cancel(); return; }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void vela$useBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        for (var l : UseBlockCallback.EVENT.listeners()) {
            var result = l.interact(player, player.level(), hand, hit);
            if (result != InteractionResult.PASS) { cir.setReturnValue(result); return; }
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void vela$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        vela$broken = minecraft.level == null ? null : minecraft.level.getBlockState(pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void vela$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var state = vela$broken;
        vela$broken = null;
        if (cir.getReturnValueZ() && state != null && minecraft.level != null && minecraft.player != null)
            for (var l : ClientPlayerBlockBreakEvents.AFTER.listeners()) l.afterBlockBreak(minecraft.level, minecraft.player, pos, state);
    }
}
