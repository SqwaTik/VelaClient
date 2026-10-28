package dev.sqwat.vela.upscale;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

/**
 * MineSR inside Vela: the temporal (and neural) upscaler that renders the world at a lower resolution and rebuilds full
 * resolution from jittered frames. The Upscaler module drives it through {@link #apply}; {@code /minesr} keeps the
 * status, retry and training-data capture commands. When the standalone MineSR mod is installed it does the work and
 * this copy stays out of the way, so the renderer is never patched twice.
 */
public final class UpscalerRuntime {
	private static UpscalerConfig config;
	private static Upscaler upscaler;
	private static FrameGen frameGen;
	private static Object lastLevel;

	private UpscalerRuntime() {
	}

	/** The upscaler, or null when the standalone MineSR mod is installed (or before startup). */
	public static Upscaler upscaler() {
		return upscaler;
	}

	/** Frame generation, or null when the standalone MineSR mod is installed (or before startup). */
	public static FrameGen frameGen() {
		return frameGen;
	}

	/** Frame generation pacing: called at a few points in each frame to show the waiting real frame on time. */
	public static void presentPendingFrameIfDue() {
		presentPendingFrameIfDue(false);
	}

	/** The pacing point: in the middle of the world's GPU work. */
	public static void presentPendingFrameMidWorld() {
		if (frameGen != null) {
			frameGen.presentPendingNow(((dev.sqwat.vela.mixin.upscale.MinecraftAccessor) net.minecraft.client.Minecraft.getInstance()).minesr$windowSurface());
		}
	}

	public static void presentPendingFrameIfDue(boolean lastChance) {
		if (frameGen != null) {
			frameGen.maybePresentPending(((dev.sqwat.vela.mixin.upscale.MinecraftAccessor) net.minecraft.client.Minecraft.getInstance()).minesr$windowSurface(), lastChance);
		}
	}

	public static boolean available() {
		return upscaler != null;
	}

	/** True while the standalone mod handles upscaling; the render hooks of this copy then do nothing. */
	public static boolean standalone() {
		return FabricLoader.getInstance().isModLoaded("minesr");
	}

	public static void initialize() {
		if (standalone()) {
			org.slf4j.LoggerFactory.getLogger("Vela/Upscaler").info("Standalone MineSR is installed; the built-in upscaler stays off");
			return;
		}
		config = UpscalerConfig.load();
		upscaler = new Upscaler(config);
		frameGen = new FrameGen(config, upscaler);
		loadNetwork();
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			// New world or dimension: the old history shows a different place.
			if (mc.level != lastLevel) {
				lastLevel = mc.level;
				upscaler.resetHistory();
			}
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
			LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal("minesr").executes(UpscalerRuntime::status);
			root.then(ClientCommands.literal("capture")
					.executes(UpscalerRuntime::captureStatus)
					.then(ClientCommands.literal("start").executes(ctx -> {
						upscaler.capture().start(ctx.getSource().getClient().gameDirectory.toPath().resolve("minesr-captures"));
						upscaler.resetHistory();
						ctx.getSource().sendFeedback(Component.literal("MineSR capture started. Play normally: walk, fly, turn. "
								+ "Saves " + config.captureBurstFrames + "-frame bursts every " + (int) config.captureBurstIntervalSeconds
								+ " s to minesr-captures/. /minesr capture stop when done."));
						return 1;
					}))
					.then(ClientCommands.literal("stop").executes(ctx -> {
						upscaler.capture().stop();
						upscaler.resetHistory();
						return captureStatus(ctx);
					})));
			root.then(ClientCommands.literal("retry").executes(ctx -> {
				upscaler.retry();
				return status(ctx);
			}));
			dispatcher.register(root);
		});
	}

	/** The module's choices, applied every tick; history restarts only when something that changes the image changed. */
	/** The Frame Generation module: an extra frame between every two rendered ones, independent of upscaling. */
	public static void applyFrameGen(boolean on) {
		if (upscaler == null || config.frameGeneration == on) return;
		if (!on) frameGen.release();
		config.frameGeneration = on;
		upscaler.resetHistory();
	}

	/** The Upscaler module's choices. */
	public static void apply(boolean enabled, Quality quality, float sharpness, boolean neural, boolean lodBias) {
		if (upscaler == null) return;
		boolean reset = config.enabled != enabled || !config.quality.equals(quality.id()) || config.neural != neural;
		boolean release = config.enabled && !enabled;
		config.enabled = enabled;
		config.quality = quality.id();
		config.sharpness = Math.clamp(sharpness, 0.0f, 1.0f);
		config.neural = neural;
		config.lodBias = lodBias;
		if (release) upscaler.release();
		else if (reset) upscaler.resetHistory();
	}

	/** config/minesr/upscaler_net.json if you trained your own, otherwise the network shipped with Vela. */
	private static void loadNetwork() {
		java.nio.file.Path custom = FabricLoader.getInstance().getConfigDir().resolve("minesr/upscaler_net.json");
		try {
			String text;
			String source;
			if (java.nio.file.Files.exists(custom)) {
				text = java.nio.file.Files.readString(custom);
				source = custom.toString();
			} else {
				try (var in = UpscalerRuntime.class.getResourceAsStream("/assets/minesr/upscaler_net.json")) {
					if (in == null) {
						upscaler.setNetwork(null, "none bundled");
						return;
					}
					text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
					source = "bundled";
				}
			}
			upscaler.setNetwork(NeuralWeights.pack(com.google.gson.JsonParser.parseString(text).getAsJsonObject()), source);
		} catch (Exception e) {
			org.slf4j.LoggerFactory.getLogger("Vela/Upscaler").warn("Could not load the upscaler network, using hand-tuned rules: {}", e.toString());
			upscaler.setNetwork(null, "failed: " + e.getMessage());
		}
	}

	private static int captureStatus(CommandContext<FabricClientCommandSource> ctx) {
		ctx.getSource().sendFeedback(Component.literal("MineSR capture: " + upscaler.capture().describe()));
		return 1;
	}

	private static int status(CommandContext<FabricClientCommandSource> ctx) {
		ctx.getSource().sendFeedback(Component.literal("MineSR " + upscaler.describe() + "; " + frameGen.describe()));
		return 1;
	}

	/** Diagnostics for the game test: the status line /minesr prints. */
	public static String describe() {
		return upscaler == null ? "unavailable" : upscaler.describe() + "; " + frameGen.describe();
	}

}
