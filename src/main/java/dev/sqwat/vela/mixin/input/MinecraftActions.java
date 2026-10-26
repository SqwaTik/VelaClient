package dev.sqwat.vela.mixin.input;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Minecraft.class)
public interface MinecraftActions {
    @Invoker("startAttack") boolean vela$attack();
    @Invoker("startUseItem") void vela$use();
}
