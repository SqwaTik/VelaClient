package dev.sqwat.vela.mixin.entity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityRenderer.class)
abstract class FriendNameTagMixin {
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void vela$friend(Entity entity,EntityRenderState state,float partial,CallbackInfo ci){
        var services=dev.sqwat.vela.VelaClient.services();
        if(entity instanceof Player player&&services!=null)dev.sqwat.vela.render.entity.FriendNameTags.mark(state.nameTag,services.friends().contains(player.getGameProfile().name()));
    }
}
