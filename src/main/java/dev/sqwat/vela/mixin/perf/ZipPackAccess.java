package dev.sqwat.vela.mixin.perf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Opens vanilla's shared ZIP handle for {@link ZipPackIndexMixin}. */
@Mixin(targets="net.minecraft.server.packs.FilePackResources$SharedZipFileAccess")
public interface ZipPackAccess {
    @Invoker("getOrCreateZipFile") java.util.zip.ZipFile vela$zip();
}
