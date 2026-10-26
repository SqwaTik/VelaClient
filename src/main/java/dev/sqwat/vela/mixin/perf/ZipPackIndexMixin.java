package dev.sqwat.vela.mixin.perf;
import dev.sqwat.vela.render.resource.ZipPackIndex;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Set;
/**
 * Zipped packs (server and user resource packs) answer listResources and getNamespaces from a sorted index instead
 * of scanning every ZIP entry per call. Same results as vanilla; only the lookup changes.
 */
@Mixin(FilePackResources.class)
abstract class ZipPackIndexMixin {
    @Shadow @Final private String prefix;
    @Shadow @Final private static Logger LOGGER;
    /** zipFileAccess's class is package-private, so it is read through a handle made inside FilePackResources. */
    @org.spongepowered.asm.mixin.Unique private static java.lang.invoke.VarHandle vela$access;
    @org.spongepowered.asm.mixin.Unique private java.util.zip.ZipFile vela$zip(){
        try{
            if(vela$access==null)vela$access=java.lang.invoke.MethodHandles.lookup().findVarHandle(FilePackResources.class,"zipFileAccess",
                Class.forName("net.minecraft.server.packs.FilePackResources$SharedZipFileAccess"));
            return ((ZipPackAccess)vela$access.get((FilePackResources)(Object)this)).vela$zip();
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    @org.spongepowered.asm.mixin.Unique private String vela$addPrefix(String path){return prefix.isEmpty()?path:prefix+path;}

    @Inject(method="listResources",at=@At("HEAD"),cancellable=true)
    private void vela$list(PackType type,String namespace,String path,PackResources.ResourceOutput output,CallbackInfo ci){
        var zip=vela$zip();
        ci.cancel();
        if(zip==null)return;
        String root=vela$addPrefix(type.getDirectory()+"/"+namespace+"/");
        ZipPackIndex.of(zip).forEachWithPrefix(root+path+"/",(name,entry)->{
            String relative=name.substring(root.length());
            Identifier id=Identifier.tryBuild(namespace,relative);
            if(id!=null)output.accept(id,IoSupplier.create(zip,entry));
            else LOGGER.warn("Invalid path in datapack: {}:{}, ignoring",namespace,relative);
        });
    }
    @Inject(method="getNamespaces",at=@At("HEAD"),cancellable=true)
    private void vela$namespaces(PackType type,CallbackInfoReturnable<Set<String>> cir){
        var zip=vela$zip();
        if(zip==null){cir.setReturnValue(Set.of());return;}
        var result=new java.util.HashSet<String>();
        for(String namespace:ZipPackIndex.of(zip).namespaces(vela$addPrefix(type.getDirectory()+"/")))
            if(Identifier.isValidNamespace(namespace))result.add(namespace);
            else LOGGER.warn("Non {} character in namespace {} in pack {}, ignoring","[a-z0-9_.-]",namespace,prefix);
        cir.setReturnValue(result);
    }
}
