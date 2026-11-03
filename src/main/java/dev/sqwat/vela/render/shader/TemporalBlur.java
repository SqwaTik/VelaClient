package dev.sqwat.vela.render.shader;

import dev.sqwat.vela.render.camera.CameraBlurMotion;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

/** Current world-only scene snapshots, captured before HUD rendering; no CPU readback. */
public final class TemporalBlur {
    private static final Identifier[] IDS={Identifier.fromNamespaceAndPath("velaclient","motion_history_0"),Identifier.fromNamespaceAndPath("velaclient","motion_history_1")};
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/motion_blur"))
        .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/glass"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/motion_blur")).build());
    private static final History[] history=new History[2];
    private static long motionAt;
    private static float previousYaw, previousPitch;
    private static Object cameraEntity;
    private static net.minecraft.world.phys.Vec3 previousPosition;
    private static boolean previousDetached;
    private static int latest=-1, pending=-1;
    private static Object world;
    private static boolean failed;
    private TemporalBlur(){}
    public static void initialize(){}
    public static void draw(GuiGraphicsExtractor g){
        var c=Minecraft.getInstance();float alpha=dev.sqwat.vela.module.visual.MotionBlur.strength();
        if(failed)return;
        if(alpha<=0||c.level==null||c.gui.screen()!=null||c.gui.overlay()!=null){close();return;}
        var source=Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTexture();
        if(source==null||source.isClosed())return;
        try{
            if(world!=c.level||history[0]==null||history[0].getTexture().getWidth(0)!=source.getWidth(0)||history[0].getTexture().getHeight(0)!=source.getHeight(0)){
                close();world=c.level;
                for(int i=0;i<2;i++){history[i]=new History(source);c.getTextureManager().register(IDS[i],history[i]);}
            }
            pending=latest==0?1:0;
            var camera=c.gameRenderer.mainCamera();
            long now=System.nanoTime();
            double seconds=(now-motionAt)/1e9;
            float horizontal=0,vertical=0;
            var position=camera.position();
            if(motionAt!=0&&cameraEntity==camera.entity()&&previousDetached==camera.isDetached()
                    &&previousPosition!=null&&previousPosition.distanceToSqr(position)<64){
                float fov=camera.getFov();
                float aspect=(float)source.getWidth(0)/source.getHeight(0);
                float horizontalFov=(float)Math.toDegrees(2*Math.atan(Math.tan(Math.toRadians(fov)*.5)*aspect));
                horizontal=CameraBlurMotion.rotation(previousYaw,camera.yRot(),seconds,horizontalFov,alpha);
                vertical=CameraBlurMotion.rotation(previousPitch,camera.xRot(),seconds,fov,alpha);
            }
            motionAt=now;previousYaw=camera.yRot();previousPitch=camera.xRot();cameraEntity=camera.entity();
            previousPosition=position;previousDetached=camera.isDetached();
            if(Math.abs(horizontal)+Math.abs(vertical)<.0001){pending=-1;return;}
            int parameters=0xFF000000|(CameraBlurMotion.encode(horizontal)<<16)|(CameraBlurMotion.encode(vertical)<<8)|127;
            g.pose().pushMatrix();
            try{g.pose().scale(g.guiWidth(),g.guiHeight());g.blit(PIPELINE,IDS[pending],0,0,0,0,1,1,1,1,1,1,parameters);}
            finally{g.pose().popMatrix();}
        }catch(RuntimeException error){disable(error);}
    }
    public static void capture(){
        if(pending<0||failed)return;
        var c=Minecraft.getInstance();var source=Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTexture();
        int slot=pending;pending=-1;
        if(c.level!=world||history[slot]==null||source==null||source.isClosed())return;
        try{
            if(source.getWidth(0)!=history[slot].getTexture().getWidth(0)||source.getHeight(0)!=history[slot].getTexture().getHeight(0))return;
            var encoder=RenderSystem.getDevice().createCommandEncoder();
            encoder.copyTextureToTexture(source,history[slot].getTexture(),0,0,0,0,0,source.getWidth(0),source.getHeight(0));
            latest=slot;
        }catch(RuntimeException error){disable(error);}
    }
    private static void disable(RuntimeException error){close();failed=true;org.slf4j.LoggerFactory.getLogger("Vela").warn("Motion Blur GPU initialization failed",error);}
    public static void close(){for(int i=0;i<2;i++){if(history[i]!=null)history[i].close();history[i]=null;}world=null;latest=pending=-1;motionAt=0;cameraEntity=null;previousPosition=null;}
    private static final class History extends AbstractTexture {
        History(GpuTexture source){texture=RenderSystem.getDevice().createTexture("Vela motion history",GpuTexture.USAGE_COPY_DST|GpuTexture.USAGE_TEXTURE_BINDING|GpuTexture.USAGE_RENDER_ATTACHMENT,source.getFormat(),source.getWidth(0),source.getHeight(0),1,1);textureView=RenderSystem.getDevice().createTextureView(texture);sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);}
    }
}
