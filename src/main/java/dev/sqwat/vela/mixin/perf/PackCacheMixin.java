package dev.sqwat.vela.mixin.perf;
import net.minecraft.server.packs.DownloadQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Mixin(DownloadQueue.class)
abstract class PackCacheMixin {
    @Shadow @Final private Path cacheDir;
    @Inject(method="runDownload",at=@At("HEAD"))
    private void vela$reuse(DownloadQueue.BatchConfig config,Map<UUID,DownloadQueue.DownloadRequest> requests,CallbackInfoReturnable<DownloadQueue.BatchResult> cir) {
        requests.forEach((id,request)->dev.sqwat.vela.render.resource.PackCache.seed(cacheDir.resolve(id.toString()),request.hash()==null?null:request.hash().toString(),config.maxSize()));
    }
}
