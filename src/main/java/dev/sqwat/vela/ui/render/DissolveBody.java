package dev.sqwat.vela.ui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.Arrays;

/** Frozen native model surfaces, including equipment. Does not own the borrowed textures. */
public final class DissolveBody {
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/dissolve"))
        .withCull(false)
        .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/dissolve"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/dissolve")).build());
    private final float[] vertices,projected,depth;
    private final int[] order;
    private final Vec3 origin;
    private final net.minecraft.client.renderer.rendertype.PreparedRenderType.Texture texture;
    private final int tint;
    private java.util.List<DissolveBody> layers=java.util.List.of();
    /** The render state of the last living frame: players, mobs and anything else with a living-entity renderer. */
    public record PoseSnapshot(net.minecraft.client.renderer.entity.state.EntityRenderState state){}
    public static void initialize(){}
    private DissolveBody(float[] vertices,Vec3 origin,net.minecraft.client.renderer.rendertype.PreparedRenderType.Texture texture,int tint){
        this.vertices=vertices;this.origin=origin;this.texture=texture;this.tint=tint;
        projected=new float[vertices.length];depth=new float[vertices.length/20];order=new int[depth.length];
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static net.minecraft.client.renderer.entity.EntityRenderer renderer(net.minecraft.world.entity.LivingEntity entity){
        var dispatcher=Minecraft.getInstance().getEntityRenderDispatcher();
        return entity instanceof AbstractClientPlayer player?dispatcher.getPlayerRenderer(player):(net.minecraft.client.renderer.entity.EntityRenderer)dispatcher.getRenderer(entity);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static PoseSnapshot remember(net.minecraft.world.entity.LivingEntity entity){
        var mc=Minecraft.getInstance();var renderer=renderer(entity);
        var state=(net.minecraft.client.renderer.entity.state.EntityRenderState)renderer.createRenderState();
        renderer.extractRenderState(entity,state,mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        return new PoseSnapshot(state);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static DissolveBody capture(net.minecraft.world.entity.LivingEntity entity,PoseSnapshot snapshot){
        var mc=Minecraft.getInstance();var renderer=renderer(entity);
        var state=snapshot.state();var origin=new Vec3(state.x,state.y,state.z);
        var parts=new java.util.ArrayList<DissolveBody>();
        var collector=(net.minecraft.client.renderer.SubmitNodeCollector)java.lang.reflect.Proxy.newProxyInstance(DissolveBody.class.getClassLoader(),new Class[]{net.minecraft.client.renderer.SubmitNodeCollector.class},(proxy,method,args)->{
            if(method.isDefault())return java.lang.reflect.InvocationHandler.invokeDefault(proxy,method,args);
            if(method.getName().equals("order"))return proxy;
            if(!method.getName().equals("submitModel")||parts.size()>=24)return null;
            var type=(net.minecraft.client.renderer.rendertype.RenderType)args[3];
            // Foil is an animated overlay, not a part of the frozen body surface.
            if(type.toString().toLowerCase(java.util.Locale.ROOT).contains("glint"))return null;
            var tex=type.prepare().textures().stream().filter(t->t.name().equals("Sampler0")).findFirst().orElse(null);
            if(tex==null)return null;
            var model=(net.minecraft.client.model.Model)args[0];var saved=save(model);
            try{
                model.setupAnim(args[1]);var mesh=new MeshCollector();VertexConsumer out=mesh;
                if(args[7] instanceof net.minecraft.client.renderer.texture.TextureAtlasSprite sprite)out=sprite.wrap(out);
                model.renderToBuffer((PoseStack)args[2],out,(int)args[4],(int)args[5],(int)args[6]);
                float[] vertices=mesh.finish();if(vertices.length>0)parts.add(new DissolveBody(vertices,origin,tex,(int)args[6]));
            }finally{restore(saved);}
            return null;
        });
        var pose=new PoseStack();var offset=renderer.getRenderOffset(state);pose.translate(offset.x,offset.y,offset.z);
        var camera=new net.minecraft.client.renderer.state.level.CameraRenderState();camera.pos=mc.gameRenderer.mainCamera().position();camera.orientation=new org.joml.Quaternionf(mc.gameRenderer.mainCamera().rotation());
        var saved=renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living?save(living.getModel()):java.util.List.<SavedPart>of();
        try{renderer.submit(state,pose,collector,camera);}finally{restore(saved);}
        if(parts.isEmpty())throw new IllegalStateException("No native model surfaces captured");
        var body=new DissolveBody(new float[0],origin,null,-1);body.layers=java.util.List.copyOf(parts);return body;
    }
    private record SavedPart(net.minecraft.client.model.geom.ModelPart part,net.minecraft.client.model.geom.PartPose pose,boolean visible,boolean skip){}
    private static java.util.List<SavedPart> save(net.minecraft.client.model.Model<?> model){
        return model.allParts().stream().map(p->new SavedPart(p,p.storePose(),p.visible,p.skipDraw)).toList();
    }
    private static void restore(java.util.List<SavedPart> saved){for(var p:saved){p.part.loadPose(p.pose);p.part.visible=p.visible;p.part.skipDraw=p.skip;}}
    public Vec3 particleOrigin(int index){
        var part=layers.isEmpty()?this:layers.get(Math.floorMod(index,layers.size()));
        if(part.vertices.length<5)return origin;
        int vertex=Math.floorMod(index*37,part.vertices.length/5)*5;
        return origin.add(part.vertices[vertex],part.vertices[vertex+1],part.vertices[vertex+2]);
    }
    public void draw(GuiGraphicsExtractor g,double progress,int rgb){
        if(!layers.isEmpty()){for(var layer:layers)layer.draw(g,progress,rgb);return;}
        if(progress<0||progress>=1||vertices.length==0)return;
        if(texture.textureView().isClosed()||texture.textureView().texture().isClosed())return;
        var mc=Minecraft.getInstance();var camera=mc.gameRenderer.mainCamera();var eye=camera.position();
        var matrix=camera.getViewRotationProjectionMatrix(new Matrix4f());var point=new Vector4f();
        int count=0;
        for(int face=0;face<depth.length;face++){
            boolean visible=true;float distance=0;
            for(int j=0;j<4;j++){
                int i=face*20+j*5;
                point.set((float)(origin.x-eye.x)+vertices[i],(float)(origin.y-eye.y)+vertices[i+1],(float)(origin.z-eye.z)+vertices[i+2],1).mul(matrix);
                if(point.w<=.05f||!Float.isFinite(point.w)){visible=false;break;}
                projected[i]=(point.x/point.w+1)*g.guiWidth()*.5f;
                projected[i+1]=(1-point.y/point.w)*g.guiHeight()*.5f;
                projected[i+2]=vertices[i+3];projected[i+3]=vertices[i+4];projected[i+4]=1/point.w;
                distance+=point.w;
            }
            if(!visible)continue;
            int start=face*20;
            float winding=(projected[start+5]-projected[start])*(projected[start+11]-projected[start+1])-(projected[start+6]-projected[start+1])*(projected[start+10]-projected[start]);
            if(winding>=0)continue;
            depth[face]=distance;int k=count++;
            // At most 72 native skin quads: back-to-front order without boxed allocations.
            while(k>0&&depth[order[k-1]]<distance){order[k]=order[k-1];k--;}
            order[k]=face;
        }
        if(count==0)return;
        float[] mesh=new float[count*20];for(int i=0;i<count;i++)System.arraycopy(projected,order[i]*20,mesh,i*20,20);
        int parameters=((int)Math.round(progress*255)<<24)|(tint&0xFFFFFF);
        g.guiRenderState.addGuiElement(new WaveRenderState(new Matrix3x2f(),mesh,TextureSetup.singleTexture(texture.textureView(),texture.sampler()),PIPELINE,parameters,g.scissorStack.peek()));
    }
    private static final class MeshCollector implements VertexConsumer {
        private float[] data=new float[1440];private int size;
        public VertexConsumer addVertex(float x,float y,float z){
            if(size+5>data.length)data=Arrays.copyOf(data,data.length*2);
            data[size++]=x;data[size++]=y;data[size++]=z;size+=2;return this;
        }
        public VertexConsumer setUv(float u,float v){data[size-2]=u;data[size-1]=v;return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setColor(int color){return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
        public VertexConsumer setLineWidth(float width){return this;}
        float[] finish(){return Arrays.copyOf(data,size);}
    }
}
