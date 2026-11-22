package dev.sqwat.vela.core.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;

/**
 * Keeps Vela's own mixins out of the way of other mods. Another mod that {@code @Overwrite}s or rewrites the same
 * method makes our injector fail at class load, which crashes the game before the title screen (for example Sodium
 * merging {@code LevelRenderer.prepareChunkRenders}). Such a mixin is left out while that mod is installed; the
 * feature behind it simply does not run, and the other mod's version of that code is used.
 *
 * <p>{@code config/velaclient-mixins.properties} can switch off any further mixin by name ({@code perf.Foo=false}),
 * so a player can get past a clash with a mod we have not seen yet without waiting for a build.
 */
public final class VelaMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOG=LoggerFactory.getLogger("Vela/Compat");
    private static final String PACKAGE="dev.sqwat.vela.mixin.";
    private static final List<String> SODIUM_LIKE=List.of("perf.TerrainAllocationMixin","perf.TerrainAllocationMapMixin","perf.DistantCompileMixin",
        "perf.DistantLeavesMixin","perf.DistantRecompileMixin","perf.DistantSectionCompilerMixin","perf.ModelCubeCompileMixin");
    /** Mod id → our mixins that clash with it. Found by starting the client next to each mod (gradle -PwithMods). */
    static final Map<String,List<String>> CONFLICTS=Map.of(
        // Sodium and its forks replace terrain preparation, section compilation and the model cube path.
        "sodium",SODIUM_LIKE,
        "embeddium",SODIUM_LIKE,
        "celeritas",SODIUM_LIKE,
        // ImmediatelyFast batches text itself and has its own map atlas.
        "immediatelyfast",List.of("perf.GlyphQuadMixin","perf.MapRendererAtlasMixin","perf.MapInstanceUploadMixin")
    );
    private static final Set<String> skipped=Collections.synchronizedSet(new TreeSet<>());
    private static Set<String> userOff=Set.of();

    /** Mixins left out for this launch. */
    public static Set<String> skipped(){synchronized(skipped){return Set.copyOf(skipped);}}
    public static boolean clashes(String mixin,Predicate<String> loaded){
        for(var e:CONFLICTS.entrySet())if(e.getValue().contains(mixin)&&loaded.test(e.getKey()))return true;
        return false;
    }

    @Override public void onLoad(String mixinPackage){
        var file=FabricLoader.getInstance().getConfigDir().resolve("velaclient-mixins.properties");
        if(!Files.isRegularFile(file))return;
        var props=new Properties();
        try(var in=Files.newBufferedReader(file)){props.load(in);}catch(Exception error){LOG.warn("Cannot read {}",file,error);return;}
        var off=new HashSet<String>();
        for(var name:props.stringPropertyNames())if("false".equalsIgnoreCase(props.getProperty(name).strip()))off.add(name.strip());
        userOff=off;
    }
    @Override public boolean shouldApplyMixin(String targetClassName,String mixinClassName){
        String mixin=mixinClassName.startsWith(PACKAGE)?mixinClassName.substring(PACKAGE.length()):mixinClassName;
        boolean apply=!userOff.contains(mixin)&&!clashes(mixin,FabricLoader.getInstance()::isModLoaded);
        if(!apply&&skipped.add(mixin))LOG.info("Mixin {} is off: it clashes with an installed mod or is switched off in velaclient-mixins.properties",mixin);
        return apply;
    }
    @Override public String getRefMapperConfig(){return null;}
    @Override public void acceptTargets(Set<String> myTargets,Set<String> otherTargets){}
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String targetClassName,ClassNode targetClass,String mixinClassName,IMixinInfo mixinInfo){}
    @Override public void postApply(String targetClassName,ClassNode targetClass,String mixinClassName,IMixinInfo mixinInfo){}
}
