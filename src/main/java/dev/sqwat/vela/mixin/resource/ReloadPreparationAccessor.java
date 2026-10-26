package dev.sqwat.vela.mixin.resource;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.concurrent.CompletableFuture;
@Mixin(SimpleReloadInstance.class)
public interface ReloadPreparationAccessor {
    @Accessor("allPreparations") CompletableFuture<?> vela$preparation();
}
