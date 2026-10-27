package dev.sqwat.vela.mixin.hud;
import dev.sqwat.vela.mixin.perf.ReloadPreparationAccessor;
import dev.sqwat.vela.ui.animation.Motion;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.TextureManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces extraction; vanilla tick still owns completion, failures and resource reload. */
@Mixin(LoadingOverlay.class)
abstract class LoadingOverlayMixin extends net.minecraft.client.gui.screens.Overlay implements dev.sqwat.vela.render.resource.InteractiveReload {
    @Shadow private float currentProgress;
    @Unique private final dev.sqwat.vela.ui.animation.LoadingProgress vela$progress=new dev.sqwat.vela.ui.animation.LoadingProgress();
    @Unique private long vela$lastFrame;
    /** Animation time: advances at most 1/30 s per frame, so a frame stalled by the reload never makes motion jump. */
    @Unique private float vela$clock;
    /** Clock time when the fade began (-1: not yet) and when the bar first showed 100%. */
    @Unique private float vela$fadeAt=-1,vela$fullAt=-1;
    @Unique private int vela$smoothFrames;
    @Unique private long vela$doneAt=-1;
    @Unique private static final float HOLD=.35f,FADE=.6f;
    @Unique private boolean vela$departing;
    @Unique private final String vela$farewell=new String[]{"До встречи!","Заходите ещё","Удачной игры!"}[java.util.concurrent.ThreadLocalRandom.current().nextInt(3)];
    @Unique private final dev.sqwat.vela.render.resource.ReloadWorldBinding vela$worldBinding=new dev.sqwat.vela.render.resource.ReloadWorldBinding();
    @Inject(method="<init>",at=@At("TAIL"))
    private void vela$departure(CallbackInfo ci){vela$departing=fadeIn&&minecraft.level==null&&dev.sqwat.vela.ui.SessionPresentation.consumeDeparture();vela$compact();}
    @Unique private boolean vela$compact(){return vela$worldBinding.visible(minecraft.level,fadeIn&&minecraft.isGameLoadFinished()&&!vela$departing,minecraft.player!=null&&!(minecraft.gui.screen() instanceof net.minecraft.client.gui.screens.LevelLoadingScreen));}
    @Override public boolean vela$showsWorld(){return vela$compact();}
    @Override public boolean vela$allowsInput(){return vela$compact()&&reload instanceof ReloadPreparationAccessor preparation&&(!preparation.vela$preparation().isDone()||reload.isDone());}
    @Override public boolean isPausing(){return !vela$allowsInput();}
    @Inject(method="isReadyToFadeOut",at=@At("HEAD"),cancellable=true)
    private void vela$noArtificialWait(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){ci.setReturnValue(true);}
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;
    @Shadow @Final private boolean fadeIn;
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Inject(method="registerTextures",at=@At("TAIL"))
    private static void vela$startupTextures(TextureManager textures,CallbackInfo ci){
        for(String name:new String[]{"logo_128","startup_title","startup_resources","startup_initializing","startup_ready","startup_goodbye","startup_leaving","startup_digits","loading_background"}){
            try(var stream=LoadingOverlayMixin.class.getResourceAsStream("/assets/velaclient/textures/gui/"+name+".png")){
                if(stream!=null)textures.register(id(name),new StartupLogoTexture(NativeImage.read(stream)));
            }catch(java.io.IOException error){org.slf4j.LoggerFactory.getLogger("Vela").warn("Startup texture unavailable: {}",name,error);}
        }
    }
    @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
    private void vela$loading(GuiGraphicsExtractor g,int mx,int my,float delta,CallbackInfo ci){
        ci.cancel();long now=Util.getMillis();if(fadeIn&&fadeInStart==-1L)fadeInStart=now;
        long frame=System.nanoTime();float seconds=vela$lastFrame==0?0:(frame-vela$lastFrame)/1_000_000_000f;vela$lastFrame=frame;
        vela$clock+=Math.min(seconds,1/30f);
        boolean done=fadeOutStart>=0;
        currentProgress=vela$progress.update(reload.getActualProgress(),done,Math.min(seconds,1/30f));
        // The reload ends with texture/sound uploads on this thread, which stall frames. Fade only after the bar has
        // rested at 100% and frames are smooth again, so the fade itself is never cut by a stall; 3 s caps the wait.
        if(done&&vela$doneAt<0)vela$doneAt=now;
        if(vela$progress.finished()&&vela$fullAt<0)vela$fullAt=vela$clock;
        vela$smoothFrames=done&&seconds<.05f?vela$smoothFrames+1:0;
        if(vela$fadeAt<0&&done&&(vela$fullAt>=0&&vela$clock-vela$fullAt>=HOLD&&vela$smoothFrames>=6||now-vela$doneAt>3000))vela$fadeAt=vela$clock;
        float fade=vela$fadeAt<0?0:Motion.clamp01((vela$clock-vela$fadeAt)/FADE);
        float out=done?Math.max(0,fade):-1; // >=0: "ready" label
        if(fade>=1){minecraft.gui.setOverlay(null);return;}
        // The menu is drawn underneath from the moment loading is done: its first frame costs land behind the
        // opaque overlay instead of stuttering right after it disappears, and the fade reveals it.
        if(done&&minecraft.gui.screen()!=null&&!vela$compact()){minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(g,mx,my,delta);}
        g.nextStratum();
        float alpha=1-fade*fade*(3-2*fade);
        if(vela$compact()){
            if(minecraft.gui.screen()!=null)minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(g,mx,my,delta);
            g.nextStratum();
            var services=dev.sqwat.vela.VelaClient.services();
            if(services!=null){var layout=services.hudLayout();layout.viewport(minecraft.getWindow().getGuiScale());var pos=layout.position("notifications",g.guiWidth(),g.guiHeight());ReloadNotice.progress=reload.isDone()?1:Math.min(.99f,reload.getActualProgress());layout.drawChip(g,minecraft,"notifications",pos.x(),pos.y());}
            return;
        }
        g.fillGradient(0,0,g.guiWidth(),g.guiHeight(),color(0xFF090B11,alpha),color(0xFF05070A,alpha));
        if(minecraft.getTextureManager().getTexture(id("loading_background")) instanceof StartupLogoTexture)
            VelaDraw.blit(g,id("loading_background"),0,0,g.guiWidth(),g.guiHeight(),0,0,1024,640,1024,640,color(0xFFFFFFFF,alpha));
        var view=UiViewport.fit(g.guiWidth(),g.guiHeight(),minecraft.getWindow().getGuiScale(),1);
        g.pose().pushMatrix();g.pose().scale(view.scale());
        // The content drifts up and grows slightly as it fades.
        float ease=fade*fade*(3-2*fade);
        g.pose().translate(view.width()/2f,view.height()/2f-8*ease);g.pose().scale(1+.04f*ease);g.pose().translate(-view.width()/2f,-view.height()/2f);
        try {
            int x=view.width()/2-110,y=view.height()/2-20;
            if(vela$departing){
                VelaDraw.logo(g,view.width()/2-22,y-65,44);
                VelaDraw.centeredText(g,"Vela 26.2",view.width()/2f,y,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,20);
                VelaDraw.centeredText(g,vela$farewell,view.width()/2f,y+32,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,9);
                return;
            }
            // Simple and robust: the logo breathing over its own soft glow, a thin bar with a moving sheen, the percent.
            // Only startup textures and plain fills: fonts and UI atlases may not be loaded yet.
            float cx=view.width()/2f,cy=view.height()/2f-22;
            boolean logo=minecraft.getTextureManager().getTexture(id("logo_128")) instanceof StartupLogoTexture;
            float breathe=1+.035f*(float)Math.sin(vela$clock*Math.PI*2/2.2);
            if(logo){
                for(int i=3;i>=1;i--){float size=40*breathe*(1+i*.35f);VelaDraw.blit(g,id("logo_128"),cx-size/2,cy-size/2,size,size,0,0,128,128,128,128,color(0x1A918BFF,alpha/i));}
                float size=40*breathe;
                VelaDraw.blit(g,id("logo_128"),cx-size/2,cy-size/2,size,size,0,0,128,128,128,128,color(0xFFFFFFFF,alpha));
            }
            int barW=180,left=Math.round(cx-barW/2f),barY=Math.round(cy+44);
            g.fill(left,barY,left+barW,barY+3,color(0x26FFFFFF,alpha));
            int progress=Math.round(barW*currentProgress);
            if(progress>0){
                g.fill(left,barY,left+progress,barY+3,color(0xFF918BFF,alpha));
                // Sheen: a short bright band running along the filled part.
                float phase=(vela$clock%1.4f)/1.4f;int band=24,bx=left+Math.round((progress+band)*phase)-band;
                int from=Math.max(left,bx),to=Math.min(left+progress,bx+band);
                if(to>from)g.fill(from,barY,to,barY+3,color(0x66FFFFFF,alpha));
            }
            String percent=Integer.toString(Math.round(currentProgress*100))+"%";
            if(minecraft.getTextureManager().getTexture(id("startup_digits")) instanceof StartupLogoTexture){
                // Digit cells are 72x96 with the glyph centred; drawn at 24x32, advanced by 9 so they read as one number.
                float advance=9f,startX=cx-percent.length()*advance/2f;
                for(int i=0;i<percent.length();i++){
                    int digit=percent.charAt(i)=='%'?10:percent.charAt(i)-'0';
                    // The % cell has extra side bearing: pull it next to the digits.
                    float dx=digit==10?-3.5f:0;
                    VelaDraw.blit(g,id("startup_digits"),startX+i*advance-7.5f+dx,barY+6,24,32,digit*72,0,72,96,792,96,color(0xFF9AA3BC,alpha));
                }
            }
        }finally{g.pose().popMatrix();}
    }
    @Unique private static void label(GuiGraphicsExtractor g,String name,float x,float y,int tint,float alpha){if(Minecraft.getInstance().getTextureManager().getTexture(id("startup_"+name)) instanceof StartupLogoTexture)VelaDraw.blit(g,id("startup_"+name),x,y,256,32,0,0,768,96,768,96,color(tint,alpha));}
    @Unique private static Identifier id(String name){return Identifier.fromNamespaceAndPath("velaclient","textures/gui/"+name+".png");}
    @Unique private static int color(int color,float alpha){return Math.round((color>>>24)*alpha)<<24|color&0xFFFFFF;}
}
