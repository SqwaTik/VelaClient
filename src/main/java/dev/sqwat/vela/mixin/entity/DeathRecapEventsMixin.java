package dev.sqwat.vela.mixin.entity;
import dev.sqwat.vela.module.visual.DeathRecap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Feeds Death Recap with what the server already told the client: who hit the player, totem pops, the death message. Every hook runs on the main thread, after the packet's thread hop. */
@Mixin(ClientPacketListener.class)
abstract class DeathRecapEventsMixin {
    @Inject(method="handleDamageEvent",at=@At("TAIL"))
    private void vela$damage(ClientboundDamageEventPacket packet,CallbackInfo ci){
        var recap=DeathRecap.current();var player=Minecraft.getInstance().player;
        if(recap==null||player==null||packet.entityId()!=player.getId())return;
        recap.damaged(packet.sourceCauseId(),packet.sourceType().unwrapKey().map(k->k.identifier().getPath()).orElse(""));
    }
    @Inject(method="handleEntityEvent",at=@At("TAIL"))
    private void vela$totem(ClientboundEntityEventPacket packet,CallbackInfo ci){
        var recap=DeathRecap.current();var mc=Minecraft.getInstance();
        if(recap!=null&&mc.level!=null&&packet.getEventId()==35&&packet.getEntity(mc.level)==mc.player)recap.totem();
    }
    // After the thread hop and before the death screen is built (see DeathRecap.killed). Matched by name only: its
    // last parameter is a PacketProcessor on 26.x and a BlockableEventLoop on 1.21.4/1.21.5.
    @Inject(method="handlePlayerCombatKill",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread",shift=At.Shift.AFTER))
    private void vela$killed(ClientboundPlayerCombatKillPacket packet,CallbackInfo ci){
        var recap=DeathRecap.current();var player=Minecraft.getInstance().player;
        if(recap!=null&&player!=null&&packet.playerId()==player.getId())recap.killed(packet.message());
    }
}
