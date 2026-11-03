package de.johni0702.minecraft.bobby;

import de.johni0702.minecraft.bobby.mixin.OptionInstanceAccessor;
import de.johni0702.minecraft.bobby.mixin.OptionInstanceIntRangeAccessor;
import de.johni0702.minecraft.bobby.util.FlawlessFrames;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/**
 * Vela: trimmed initializer. No config file, commands or config screen; old caches are not cleaned up
 * (deleteUnusedRegionsAfterDays is -1).
 */
public class Bobby implements ClientModInitializer {
    public static final String MOD_ID = "bobby";

    { instance = this; }
    private static Bobby instance;
    public static Bobby getInstance() {
        return instance;
    }

    private static final Minecraft client = Minecraft.getInstance();

    @Override
    public void onInitializeClient() {
        FlawlessFrames.onClientInitialization();
        client.execute(() -> raiseMaxRenderDistance(BobbyConfig.DEFAULT.getMaxRenderDistance()));
    }

    public BobbyConfig getConfig() {
        return BobbyConfig.DEFAULT;
    }

    public boolean isEnabled() {
        // For singleplayer, disable ourselves unless the view-distance overwrite is active.
        return client.getSingleplayerServer() == null || getConfig().getViewDistanceOverwrite() != 0;
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private static void raiseMaxRenderDistance(int maxRenderDistance) {
        OptionInstance<Integer> viewDistance = client.options.renderDistance();
        if (viewDistance.values() instanceof OptionInstance.IntRange callbacks) {
            OptionInstanceIntRangeAccessor callbacksAcc = (OptionInstanceIntRangeAccessor)(Object) callbacks;
            callbacksAcc.setMaxInclusive(Math.max(callbacks.maxInclusive(), maxRenderDistance));
            OptionInstanceAccessor<Integer> optionAccessor = (OptionInstanceAccessor<Integer>)(Object) viewDistance;
            optionAccessor.setCodec(callbacks.codec());
        }
    }
}
