package dev.sqwat.vela.render.perf;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import org.joml.Matrix3x2f;
import org.joml.Vector4f;

/**
 * Builds the in-game HUD (hotbar, hearts, chat, Vela panels, other mods' HUD layers) only as often as the monitor
 * refreshes and draws it into a texture; the frames in between blend that texture in instead of extracting, sorting and
 * uploading every element again. At hundreds of FPS the HUD is about a third of a frame on the CPU, and the screen
 * shows no more than one HUD per refresh anyway.
 *
 * What blends with the world or follows the camera is drawn live every frame on top of the world and below the cached
 * HUD: the crosshair (inverted colours), camera overlays (vignette, pumpkin, frost, spyglass), motion blur, Vela's
 * world-anchored effects and markers. Off with a screen or overlay open, with the HUD hidden, during a Death Recap replay
 * and whenever the frame rate is under about two frames per refresh (there it would only add a full-screen blend).
 */
public final class HudCache {
    private static final Vector4f CLEAR=new Vector4f(0);
    // Two frames per refresh to switch on, 1.6 to switch off, so a frame rate near the edge does not flip it every frame.
    private static final double ON_ABOVE=2.0,OFF_BELOW=1.6;

    private static TextureTarget target;
    private static boolean active,refresh,extracting,drawing,worth=true;
    private static long nextRefresh,lastFrame,refreshNanos=1_000_000_000L/144,nextRateCheck;
    private static double frameNanos;
    private HudCache(){}

    /** Once per frame, before any GUI extraction. */
    public static void beginFrame(Minecraft mc,boolean renderLevel){
        long now=System.nanoTime();
        measure(mc,now);
        boolean was=active;
        active=renderLevel&&enabled()&&mc.level!=null&&mc.player!=null&&mc.gui.screen()==null&&mc.gui.overlay()==null
            &&!mc.gui.hud.isHidden()&&!dev.sqwat.vela.module.visual.DeathReplay.active()&&worth;
        if(!active){
            refresh=false;
            if(mc.level==null)close();
            return;
        }
        int width=mc.getWindow().getWidth(),height=mc.getWindow().getHeight();
        refresh=!was||target==null||target.width!=width||target.height!=height||now>=nextRefresh;
        if(refresh){
            // Scheduled on the refresh grid rather than "one interval after the last refresh": that would refresh
            // a little below the monitor rate, since a frame rarely ends exactly on the interval.
            nextRefresh=Math.max(nextRefresh+refreshNanos,now+refreshNanos/2);
            if(target==null)target=new TextureTarget("Vela HUD cache",width,height,true,GpuFormat.RGBA8_UNORM);
            else if(target.width!=width||target.height!=height)target.resize(width,height);
        }
    }

    private static void measure(Minecraft mc,long now){
        long dt=now-lastFrame;lastFrame=now;
        if(dt>0&&dt<1_000_000_000L)frameNanos=frameNanos==0?dt:frameNanos*.95+dt*.05;
        if(now>=nextRateCheck){
            // A GLFW query: once a second is enough to follow the window to another monitor.
            int hz=mc.getWindow().getRefreshRate();
            refreshNanos=1_000_000_000L/Math.max(30,hz>0?hz:144);
            nextRateCheck=now+1_000_000_000L;
        }
        double framesPerRefresh=frameNanos==0?ON_ABOVE:refreshNanos/frameNanos;
        if(worth?framesPerRefresh<OFF_BELOW:framesPerRefresh>ON_ABOVE)worth=!worth;
    }

    private static boolean enabled(){var o=dev.sqwat.vela.module.Optimization.current();return o!=null&&o.hudCache();}

    public static boolean active(){return active;}
    /** The HUD stays as cached this frame: its extraction is skipped. */
    public static boolean reuse(){return active&&!refresh;}
    public static boolean refresh(){return active&&refresh;}
    /** True while the HUD is extracted for the texture: the live parts are left out of it. */
    public static boolean cachedPass(){return extracting;}
    public static void extracting(boolean value){extracting=value;}

    /** The GUI renderer's target: the HUD texture while the cached HUD draws, otherwise the game's own. */
    public static RenderTarget drawTarget(RenderTarget main){return drawing&&target!=null?target:main;}

    public static void drawIntoTexture(Runnable draw){
        // Depth clears to 0: 26.x renders with reversed depth.
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(),CLEAR,target.getDepthTexture(),0.0);
        drawing=true;
        try{draw.run();}finally{drawing=false;}
    }

    /** Every frame while active: the live parts, then the cached HUD over them. */
    public static void extractLive(Minecraft mc,GuiGraphicsExtractor g,DeltaTracker delta){
        var services=dev.sqwat.vela.VelaClient.services();
        dev.sqwat.vela.render.shader.TemporalBlur.draw(g);
        g.nextStratum();
        var hud=(dev.sqwat.vela.mixin.perf.HudLiveInvoker)mc.gui.hud;
        hud.vela$extractCameraOverlays(g,delta);
        hud.vela$extractCrosshair(g,delta);
        g.nextStratum();
        if(services!=null)services.hudLayout().extractEffects(g,delta);
        g.nextStratum();
        composite(mc,g);
        g.nextStratum();
        if(services!=null)dev.sqwat.vela.marker.MarkerRuntime.draw(g,services.markers());
    }

    private static void composite(Minecraft mc,GuiGraphicsExtractor g){
        if(target==null)return;
        var window=mc.getWindow();
        int width=g.guiWidth(),height=g.guiHeight();
        // The scaled size rounds up; the texture covers the exact window, so it must not stretch by that remainder.
        var pose=new Matrix3x2f(g.pose()).scale(window.getWidth()/(float)window.getGuiScale()/width,window.getHeight()/(float)window.getGuiScale()/height);
        g.guiRenderState.addGuiElement(new BlitRenderState(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            TextureSetup.singleTexture(target.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)),
            pose,0,0,width,height,0,1,1,0,-1,null));
    }

    public static void close(){
        active=refresh=extracting=drawing=false;
        if(target!=null){target.destroyBuffers();target=null;}
    }
}
