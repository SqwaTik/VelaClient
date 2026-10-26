package dev.sqwat.vela.mixin;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ItemInHandRenderer.class)
public interface HeldItemAccessor {
    @Accessor("mainHandItem") ItemStack vela$visibleMainHand();
}
