// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.hud;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import dev.sqwat.vela.zako.ZakoOptConfig;

import java.util.Optional;
import java.util.OptionalDouble;

// HUD drawn into its own texture at the monitor refresh rate; frames in between only blend that texture in.
// Crosshair and camera overlays (vignette, pumpkin...) blend with the world, so they stay live every frame.
@UtilityClass
public class HudCache {
	private final Vector4f CLEAR = new Vector4f(0, 0, 0, 0);
	// Shares the normal GUI upload and render pass with Vela's live HUD.
	private final RenderPipeline GUI_COMPOSITE = net.minecraft.client.renderer.RenderPipelines.register(
			RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath("velaclient", "pipeline/hud_cache"))
			.withVertexShader(Identifier.fromNamespaceAndPath("velaclient", "core/glass"))
			.withFragmentShader(Identifier.fromNamespaceAndPath("velaclient", "core/hud_cache"))
			.withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA), GpuFormat.RGBA8_UNORM, 7)).build());
	public void initialize() {} // Register before shader resource reload.

	private TextureTarget target;
	// the game's own main target, taken before the HUD: mods (Xaero's minimap) swap their own one in while they draw
	private RenderTarget main;
	private boolean active;
	private boolean refresh;
	private boolean drawing;
	private long lastRefresh;
	public boolean suppressLiveParts;
	public boolean additionalPass;
	public void clear() {
		active = refresh = drawing = suppressLiveParts = additionalPass = false;
		main = null; lastRefresh = 0;
		HudDelta.idle();
		if (target != null) { target.destroyBuffers(); target = null; }
	}

	// once per frame, before any GUI extraction
	public void beginFrame(boolean renderLevel) {
		Minecraft mc = Minecraft.getInstance();
		boolean was = active;
		long now = System.nanoTime();
		active = ZakoOptConfig.hudCache() && !HudWorth.isMainSwapped() && renderLevel && mc.level != null && !mc.gui.hud.isHidden() && mc.gui.screen() == null && mc.gui.overlay() == null && HudWorth.update(now, MonitorRate.intervalNs(now));
		if (!active) {
			HudDelta.idle();
			return;
		}
		main = mc.gameRenderer.mainRenderTarget();
		int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
		refresh = !was || target == null || target.width != w || target.height != h || now - lastRefresh >= MonitorRate.intervalNs(now);
		HudDelta.frame(refresh, mc.getDeltaTracker().getGameTimeDeltaTicks());
		if (refresh) {
			lastRefresh = now;
			if (target == null) {
				target = new TextureTarget("zakoopt hud", w, h, true, GpuFormat.RGBA8_UNORM);
			} else if (target.width != w || target.height != h) {
				target.resize(w, h);
			}
		}
	}

	public boolean active() {
		return active;
	}

	public boolean skipExtraction() {
		return active && !refresh;
	}

	public RenderTarget drawTarget(RenderTarget requested) {
		if(!drawing)return null;
		if(requested==main)return target;
		if(requested!=null&&requested!=target&&requested.width==main.width&&requested.height==main.height)HudWorth.mainSwapped(requested);
		return null;
	}

	public RenderTarget redirect(RenderTarget requested) {
		return drawing && requested != target && requested == main ? target : null;
	}

	// depth is cleared to 0: 26.x renders with reversed depth
	public void drawIntoTexture(Runnable draw) {
		RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), CLEAR, target.getDepthTexture(), 0.0);
		drawing = true;
		try {
			draw.run();
		} finally {
			drawing = false;
		}
	}

	public void extractComposite(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.renderer.state.gui.GuiRenderState state) {
		graphics.nextStratum();
		state.addGuiElement(new net.minecraft.client.renderer.state.gui.BlitRenderState(GUI_COMPOSITE,
			net.minecraft.client.gui.render.TextureSetup.singleTexture(target.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)),
			new org.joml.Matrix3x2f(graphics.pose()),0,0,graphics.guiWidth(),graphics.guiHeight(),0,1,0,1,-1,null));
		graphics.nextStratum();
	}
}
