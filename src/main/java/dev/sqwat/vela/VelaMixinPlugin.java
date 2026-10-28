package dev.sqwat.vela;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/**
 * Skips the built-in upscaler's hooks when the standalone MineSR mod is installed: both would redirect the same
 * render calls, and two redirects of one call stop the game from starting.
 */
public final class VelaMixinPlugin implements IMixinConfigPlugin {
    private static final String UPSCALER="dev.sqwat.vela.mixin.upscale.";
    private boolean standalone;
    @Override public void onLoad(String mixinPackage){standalone=FabricLoader.getInstance().isModLoaded("minesr");}
    @Override public String getRefMapperConfig(){return null;}
    @Override public boolean shouldApplyMixin(String targetClassName,String mixinClassName){return !(standalone&&mixinClassName.startsWith(UPSCALER));}
    @Override public void acceptTargets(Set<String> myTargets,Set<String> otherTargets){}
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String targetClassName,ClassNode targetClass,String mixinClassName,IMixinInfo mixinInfo){}
    @Override public void postApply(String targetClassName,ClassNode targetClass,String mixinClassName,IMixinInfo mixinInfo){}
}
