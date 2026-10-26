package dev.sqwat.vela.mixin.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemCooldowns.class)
public abstract class CooldownHudMixin {
    @Inject(method="addCooldown(Lnet/minecraft/resources/Identifier;I)V",at=@At("TAIL"))
    private void vela$start(Identifier group,int ticks,CallbackInfo ci){var p=Minecraft.getInstance().player;if(p!=null&&p.getCooldowns()==(Object)this)dev.sqwat.vela.ui.hud.CooldownTracker.start(group,ticks);}
    @Inject(method="removeCooldown",at=@At("TAIL"))
    private void vela$end(Identifier group,CallbackInfo ci){var p=Minecraft.getInstance().player;if(p!=null&&p.getCooldowns()==(Object)this)dev.sqwat.vela.ui.hud.CooldownTracker.end(group);}
    @Inject(method="tick",at=@At("TAIL"))
    private void vela$tick(CallbackInfo ci){var p=Minecraft.getInstance().player;if(p!=null&&p.getCooldowns()==(Object)this)dev.sqwat.vela.ui.hud.CooldownTracker.tick();}
}
