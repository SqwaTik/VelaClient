package dev.sqwat.vela.mixin.perf;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
/**
 * Always use vanilla's profiled reload: it logs total time and per-listener preparing/applying times, which is what
 * resource-pack load optimisation needs. Cost is a timestamp per listener stage.
 */
@Mixin(ReloadableResourceManager.class)
abstract class ProfiledReloadMixin {
    @ModifyArg(method="createReload",at=@At(value="INVOKE",target="Lnet/minecraft/server/packs/resources/SimpleReloadInstance;create(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Z)Lnet/minecraft/server/packs/resources/ReloadInstance;"),index=5)
    private boolean vela$profile(boolean profiled){return true;}
}
