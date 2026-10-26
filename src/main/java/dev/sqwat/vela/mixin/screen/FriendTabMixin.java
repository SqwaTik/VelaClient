package dev.sqwat.vela.mixin.screen;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
@Mixin(PlayerTabOverlay.class)
public abstract class FriendTabMixin {
    @Shadow protected abstract List<PlayerInfo> getPlayerInfos();
    @Unique private List<PlayerInfo> vela$players=List.of();
    @Unique private int vela$row;
    @Inject(method="extractRenderState",at=@At("HEAD"))
    private void vela$begin(CallbackInfo ci){vela$players=getPlayerInfos();vela$row=0;}
    @Redirect(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V",ordinal=2))
    private void vela$friendRow(GuiGraphicsExtractor g,int x,int y,int right,int bottom,int color){
        var services=dev.sqwat.vela.VelaClient.services();
        if(services!=null&&vela$row<vela$players.size()&&services.friends().contains(vela$players.get(vela$row).getProfile().name()))color=0x506FD92B;
        vela$row++;g.fill(x,y,right,bottom,color);
    }
}
