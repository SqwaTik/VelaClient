package dev.sqwat.vela.mixin.world;

import dev.sqwat.vela.module.visual.Particles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe accepted client-thread events, never outbound attack requests. */
@Mixin(ClientPacketListener.class)
abstract class ParticleEventsMixin {
    @Inject(method="handleHurtAnimation",at=@At("TAIL"))
    private void vela$hurt(ClientboundHurtAnimationPacket packet,CallbackInfo ci){
        var level=Minecraft.getInstance().level;
        if(level!=null)Particles.emit(level.getEntity(packet.id()),Particles.Event.HIT);
    }
    @Inject(method="handleEntityEvent",at=@At("TAIL"))
    private void vela$totem(ClientboundEntityEventPacket packet,CallbackInfo ci){
        var level=Minecraft.getInstance().level;
        if(level!=null&&packet.getEventId()==35){
            var entity=packet.getEntity(level);
            Particles.emit(entity,Particles.Event.TOTEM);
        }
    }
}
