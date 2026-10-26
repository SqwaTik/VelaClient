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
/** Feeds Death Recap with what the server already told the client: who hit the player, totem pops, the death message. TAIL runs on the main thread. */
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
    @Inject(method="handlePlayerCombatKill",at=@At("TAIL"))
    private void vela$killed(ClientboundPlayerCombatKillPacket packet,CallbackInfo ci){
        var recap=DeathRecap.current();var player=Minecraft.getInstance().player;
        if(recap!=null&&player!=null&&packet.playerId()==player.getId())recap.killed(packet.message());
    }
}
