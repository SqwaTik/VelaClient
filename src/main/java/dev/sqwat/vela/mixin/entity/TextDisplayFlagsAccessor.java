package dev.sqwat.vela.mixin.entity;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Reads the synced style flags; the see-through flag marks holograms meant to show through walls. */
@Mixin(Display.TextDisplay.class)
public interface TextDisplayFlagsAccessor {
    @Invoker("getFlags") byte vela$flags();
}
