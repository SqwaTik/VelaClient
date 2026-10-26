package dev.sqwat.vela.mixin;
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
abstract class LoadingOverlayMixin extends net.minecraft.client.gui.screens.Overlay implements dev.sqwat.vela.render.InteractiveReload {
    @Shadow private float currentProgress;
    @Unique private final dev.sqwat.vela.ui.animation.LoadingProgress vela$progress=new dev.sqwat.vela.ui.animation.LoadingProgress();
    @Unique private long vela$lastFrame;
    @Unique private boolean vela$departing;
    @Unique private final String vela$farewell=new String[]{"До встречи!","Заходите ещё","Удачной игры!"}[java.util.concurrent.ThreadLocalRandom.current().nextInt(3)];
    @Unique private final dev.sqwat.vela.render.ReloadWorldBinding vela$worldBinding=new dev.sqwat.vela.render.ReloadWorldBinding();
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
        float out=fadeOutStart<0?-1:(now-fadeOutStart)/90f;
        float in=fadeInStart<0?1:Motion.clamp01((now-fadeInStart)/500f);
        g.nextStratum();
        float alpha=out>=1?1-Motion.clamp01(out-1):1;
        long frame=System.nanoTime();float seconds=vela$lastFrame==0?0:(frame-vela$lastFrame)/1_000_000_000f;vela$lastFrame=frame;
        currentProgress=vela$progress.update(reload.getActualProgress(),out>=0,seconds);
        if(out>=2){minecraft.gui.setOverlay(null);return;}
        if(vela$compact()){
            if(minecraft.gui.screen()!=null)minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(g,mx,my,delta);
            g.nextStratum();
            var services=dev.sqwat.vela.VelaClient.services();
            if(services!=null){var layout=services.hudLayout();layout.viewport(minecraft.getWindow().getGuiScale());var pos=layout.position("notifications",g.guiWidth(),g.guiHeight());ReloadNotice.progress=reload.isDone()?1:Math.min(.99f,reload.getActualProgress());layout.drawChip(g,minecraft,"notifications",pos.x(),pos.y());}
            return;
        }
        g.fillGradient(0,0,g.guiWidth(),g.guiHeight(),0xFF090B11,0xFF05070A);
        if(minecraft.getTextureManager().getTexture(id("loading_background")) instanceof StartupLogoTexture)
            VelaDraw.blit(g,id("loading_background"),0,0,g.guiWidth(),g.guiHeight(),0,0,1024,640,1024,640,0xFFFFFFFF);
        var view=UiViewport.fit(g.guiWidth(),g.guiHeight(),minecraft.getWindow().getGuiScale(),1);
        g.pose().pushMatrix();g.pose().scale(view.scale());
        try {
            int x=view.width()/2-110,y=view.height()/2-20;
            if(vela$departing){
                VelaDraw.logo(g,view.width()/2-22,y-65,44);
                VelaDraw.centeredText(g,"Vela 26.2",view.width()/2f,y,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,20);
                VelaDraw.centeredText(g,vela$farewell,view.width()/2f,y+32,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,9);
                return;
            }
            float cx=view.width()/2f,cy=y-46;
            for(int i=0;i<64;i++){
                double angle=i*Math.PI*2/64+(now%2400)/2400.0*Math.PI*2;
                int shade=i<12?color(0xB8918BFF,alpha*(1-i/14f)):color(0x19918BFF,alpha);
                int px=Math.round(cx+(float)Math.cos(angle)*31),py=Math.round(cy+(float)Math.sin(angle)*31);
                g.fill(px,py,px+1,py+1,shade);
            }
            float logoSize=31;
            if(minecraft.getTextureManager().getTexture(id("logo_128")) instanceof StartupLogoTexture)
                VelaDraw.blit(g,id("logo_128"),cx-logoSize/2,cy-logoSize/2,logoSize,logoSize,0,0,128,128,128,128,color(0xFFFFFFFF,alpha));
            label(g,out>=0?"ready":"title",cx-128,y+1,0xFFF3F5FA,alpha);
            label(g,currentProgress>.85?"initializing":"resources",cx-128,y+25,0xFF929CAF,alpha);
            int left=Math.round(cx-100);g.fill(left,y+86,left+200,y+88,color(0xFF252B38,alpha));
            int progress=Math.round(200*currentProgress);
            if(progress>0)g.fill(left,y+86,left+progress,y+88,color(0xFF918BFF,alpha));
            String percent=Integer.toString(Math.round(currentProgress*100))+"%";
            if(minecraft.getTextureManager().getTexture(id("startup_digits")) instanceof StartupLogoTexture)
                for(int i=0;i<percent.length();i++){
                    int digit=percent.charAt(i)=='%'?10:percent.charAt(i)-'0';
                    VelaDraw.blit(g,id("startup_digits"),left+200-(percent.length()-i)*7-9,y+57,24,32,digit*72,0,72,96,792,96,color(0xFF8D88DC,alpha));
                }
        }finally{g.pose().popMatrix();}
    }
    @Unique private static void label(GuiGraphicsExtractor g,String name,float x,float y,int tint,float alpha){if(Minecraft.getInstance().getTextureManager().getTexture(id("startup_"+name)) instanceof StartupLogoTexture)VelaDraw.blit(g,id("startup_"+name),x,y,256,32,0,0,768,96,768,96,color(tint,alpha));}
    @Unique private static Identifier id(String name){return Identifier.fromNamespaceAndPath("velaclient","textures/gui/"+name+".png");}
    @Unique private static int color(int color,float alpha){return Math.round((color>>>24)*alpha)<<24|color&0xFFFFFF;}
}
