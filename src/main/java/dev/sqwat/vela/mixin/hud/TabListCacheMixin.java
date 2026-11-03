package dev.sqwat.vela.mixin.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Collection;
import java.util.List;

/**
 * Vanilla sorts the whole online list (thousands of players on a big server) by team and name every frame while Tab
 * is held, and Vela's friend highlight asked for it a second time. The sorted first 80 are kept for a quarter second,
 * and rebuilt at once when someone joins or leaves.
 */
@Mixin(PlayerTabOverlay.class)
abstract class TabListCacheMixin {
    @Shadow @Final private Minecraft minecraft;
    @Unique private List<PlayerInfo> vela$sorted;
    @Unique private Collection<PlayerInfo> vela$source;
    @Unique private int vela$size;
    @Unique private long vela$until;

    @Inject(method="getPlayerInfos",at=@At("HEAD"),cancellable=true)
    private void vela$cached(CallbackInfoReturnable<List<PlayerInfo>> cir){
        var listed=minecraft.player.connection.getListedOnlinePlayers();
        if(vela$sorted!=null&&listed==vela$source&&listed.size()==vela$size&&System.nanoTime()<vela$until)cir.setReturnValue(vela$sorted);
    }
    @Inject(method="getPlayerInfos",at=@At("RETURN"))
    private void vela$remember(CallbackInfoReturnable<List<PlayerInfo>> cir){
        var listed=minecraft.player.connection.getListedOnlinePlayers();
        if(cir.getReturnValue()==vela$sorted)return;
        vela$sorted=cir.getReturnValue();vela$source=listed;vela$size=listed.size();vela$until=System.nanoTime()+250_000_000L;
    }
}
