package dev.sqwat.vela.compat.gizmos;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * 1.21.11's gizmos on 1.21.5: lines, quads and points submitted between {@link #begin()} and {@link #draw} (Vela's visual
 * effects, see VisualGizmosMixin) are drawn once, after the translucent pass. "Always on top" ones skip the depth test.
 */
public final class Gizmos {
    private static final class Line implements GizmoProperties {
        final Vec3 a,b;final int color;final float width;boolean top;
        Line(Vec3 a,Vec3 b,int color,float width){this.a=a;this.b=b;this.color=color;this.width=width;}
        @Override public GizmoProperties setAlwaysOnTop(){top=true;return this;}
    }
    private static final class Quad implements GizmoProperties {
        final Vec3 a,b,c,d;final int color;boolean top;
        Quad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color){this.a=a;this.b=b;this.c=c;this.d=d;this.color=color;}
        @Override public GizmoProperties setAlwaysOnTop(){top=true;return this;}
    }
    private static final class Point implements GizmoProperties {
        final Vec3 at;final int color;final float size;boolean top;
        Point(Vec3 at,int color,float size){this.at=at;this.color=color;this.size=size;}
        @Override public GizmoProperties setAlwaysOnTop(){top=true;return this;}
    }
    /** Several primitives of one gizmo (a cuboid's faces and edges) answering as one. */
    private record Group(List<GizmoProperties> parts) implements GizmoProperties {
        @Override public GizmoProperties setAlwaysOnTop(){parts.forEach(GizmoProperties::setAlwaysOnTop);return this;}
    }
    private static final GizmoProperties NONE=new GizmoProperties(){@Override public GizmoProperties setAlwaysOnTop(){return this;}};
    private static final List<Line> LINES=new ArrayList<>();
    private static final List<Quad> QUADS=new ArrayList<>();
    private static final List<Point> POINTS=new ArrayList<>();
    private static boolean collecting;
    private Gizmos(){}

    public static void begin(){LINES.clear();QUADS.clear();POINTS.clear();collecting=true;}
    public static boolean isEmpty(){return LINES.isEmpty()&&QUADS.isEmpty()&&POINTS.isEmpty();}

    public static GizmoProperties line(Vec3 a,Vec3 b,int color){return line(a,b,color,3f);}
    public static GizmoProperties line(Vec3 a,Vec3 b,int color,float width){
        if(!collecting)return NONE;
        var line=new Line(a,b,color,width);LINES.add(line);return line;
    }
    public static GizmoProperties point(Vec3 at,int color,float size){
        if(!collecting)return NONE;
        var point=new Point(at,color,size);POINTS.add(point);return point;
    }
    public static GizmoProperties rect(Vec3 a,Vec3 b,Vec3 c,Vec3 d,GizmoStyle style){
        if(!collecting)return NONE;
        List<GizmoProperties> parts=new ArrayList<>(5);
        if(style.hasFill()){var quad=new Quad(a,b,c,d,style.fill());QUADS.add(quad);parts.add(quad);}
        if(style.hasStroke()){
            parts.add(line(a,b,style.stroke(),style.strokeWidth()));parts.add(line(b,c,style.stroke(),style.strokeWidth()));
            parts.add(line(c,d,style.stroke(),style.strokeWidth()));parts.add(line(d,a,style.stroke(),style.strokeWidth()));
        }
        return new Group(parts);
    }
    public static GizmoProperties rect(Vec3 min,Vec3 max,Direction face,GizmoStyle style){
        return switch(face){
            case DOWN->rect(new Vec3(min.x,min.y,min.z),new Vec3(max.x,min.y,min.z),new Vec3(max.x,min.y,max.z),new Vec3(min.x,min.y,max.z),style);
            case UP->rect(new Vec3(min.x,max.y,min.z),new Vec3(min.x,max.y,max.z),new Vec3(max.x,max.y,max.z),new Vec3(max.x,max.y,min.z),style);
            case NORTH->rect(new Vec3(min.x,min.y,min.z),new Vec3(min.x,max.y,min.z),new Vec3(max.x,max.y,min.z),new Vec3(max.x,min.y,min.z),style);
            case SOUTH->rect(new Vec3(min.x,min.y,max.z),new Vec3(max.x,min.y,max.z),new Vec3(max.x,max.y,max.z),new Vec3(min.x,max.y,max.z),style);
            case WEST->rect(new Vec3(min.x,min.y,min.z),new Vec3(min.x,min.y,max.z),new Vec3(min.x,max.y,max.z),new Vec3(min.x,max.y,min.z),style);
            case EAST->rect(new Vec3(max.x,min.y,min.z),new Vec3(max.x,max.y,min.z),new Vec3(max.x,max.y,max.z),new Vec3(max.x,min.y,max.z),style);
        };
    }
    public static GizmoProperties cuboid(BlockPos pos,GizmoStyle style){return cuboid(new AABB(pos),style);}
    public static GizmoProperties cuboid(BlockPos pos,float inflate,GizmoStyle style){return cuboid(new AABB(pos).inflate(inflate),style);}
    public static GizmoProperties cuboid(AABB box,GizmoStyle style,boolean coloredCorner){return cuboid(box,style);}
    public static GizmoProperties cuboid(AABB box,GizmoStyle style){
        if(!collecting)return NONE;
        Vec3 min=new Vec3(box.minX,box.minY,box.minZ),max=new Vec3(box.maxX,box.maxY,box.maxZ);
        List<GizmoProperties> parts=new ArrayList<>(18);
        GizmoStyle faces=GizmoStyle.fill(style.fill());
        if(style.hasFill())for(Direction face:Direction.values())parts.add(rect(min,max,face,faces));
        if(style.hasStroke()){
            double x0=box.minX,y0=box.minY,z0=box.minZ,x1=box.maxX,y1=box.maxY,z1=box.maxZ;
            double[][] edges={{x0,y0,z0,x1,y0,z0},{x0,y0,z0,x0,y1,z0},{x0,y0,z0,x0,y0,z1},{x1,y0,z0,x1,y1,z0},{x1,y1,z0,x0,y1,z0},{x0,y1,z0,x0,y1,z1},
                {x0,y1,z1,x0,y0,z1},{x0,y0,z1,x1,y0,z1},{x1,y0,z1,x1,y0,z0},{x0,y1,z1,x1,y1,z1},{x1,y0,z1,x1,y1,z1},{x1,y1,z0,x1,y1,z1}};
            for(double[] e:edges)parts.add(line(new Vec3(e[0],e[1],e[2]),new Vec3(e[3],e[4],e[5]),style.stroke(),style.strokeWidth()));
        }
        return new Group(parts);
    }

    private static final RenderPipeline QUADS_TOP=RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("pipeline/vela_gizmo_quads_top").withCull(false).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).build();
    private static final RenderPipeline LINES_TOP=RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("pipeline/vela_gizmo_lines_top").withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).build();
    private static final RenderType QUADS_TYPE=RenderType.create("vela_gizmo_quads",1536,false,true,RenderPipelines.DEBUG_QUADS,
        RenderType.CompositeState.builder().createCompositeState(false));
    private static final RenderType QUADS_TOP_TYPE=RenderType.create("vela_gizmo_quads_top",1536,false,true,QUADS_TOP,
        RenderType.CompositeState.builder().createCompositeState(false));
    private static final Map<Integer,RenderType> LINE_TYPES=new HashMap<>();
    private static RenderType lineType(float width,boolean top){
        int key=Math.max(1,Math.round(width*2))*2+(top?1:0);
        return LINE_TYPES.computeIfAbsent(key,k->RenderType.create("vela_gizmo_lines_"+k,1536,false,true,(k&1)==1?LINES_TOP:RenderPipelines.LINES,
            RenderType.CompositeState.builder().setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of((k>>1)/2.0))).createCompositeState(false)));
    }

    /** Draws what was submitted since {@link #begin()}, camera-relative; the pose is the level's (the camera rotation is in the model-view). */
    public static void draw(PoseStack poseStack,MultiBufferSource.BufferSource buffers,double cx,double cy,double cz){
        collecting=false;
        if(isEmpty())return;
        PoseStack.Pose pose=poseStack.last();
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();
        for(int pass=0;pass<2;pass++){
            boolean top=pass==1;
            RenderType quadType=top?QUADS_TOP_TYPE:QUADS_TYPE;
            VertexConsumer quads=null;
            for(Quad q:QUADS){
                if(q.top!=top)continue;
                if(quads==null)quads=buffers.getBuffer(quadType);
                quad(quads,pose,q.a,q.b,q.c,q.d,q.color,cx,cy,cz);
            }
            var left=camera.getLeftVector();var up=camera.getUpVector();
            for(Point p:POINTS){
                if(p.top!=top)continue;
                if(quads==null)quads=buffers.getBuffer(quadType);
                // a camera-facing square about size pixels wide at the point's distance
                double r=p.size*.0015*Math.max(1,p.at.distanceTo(camera.getPosition()));
                Vec3 l=new Vec3(left.x()*r,left.y()*r,left.z()*r),u=new Vec3(up.x()*r,up.y()*r,up.z()*r);
                quad(quads,pose,p.at.add(l).add(u),p.at.add(l).subtract(u),p.at.subtract(l).subtract(u),p.at.subtract(l).add(u),p.color,cx,cy,cz);
            }
            if(quads!=null)buffers.endBatch(quadType);
            for(Line line:LINES){
                if(line.top!=top)continue;
                VertexConsumer out=buffers.getBuffer(lineType(line.width,top));
                float x0=(float)(line.a.x-cx),y0=(float)(line.a.y-cy),z0=(float)(line.a.z-cz),x1=(float)(line.b.x-cx),y1=(float)(line.b.y-cy),z1=(float)(line.b.z-cz);
                float nx=x1-x0,ny=y1-y0,nz=z1-z0;float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
                if(len<1e-6f)continue;
                nx/=len;ny/=len;nz/=len;
                out.addVertex(pose,x0,y0,z0).setColor(line.color).setNormal(pose,nx,ny,nz);
                out.addVertex(pose,x1,y1,z1).setColor(line.color).setNormal(pose,nx,ny,nz);
            }
            for(RenderType type:LINE_TYPES.values())buffers.endBatch(type);
        }
        LINES.clear();QUADS.clear();POINTS.clear();
    }
    private static void quad(VertexConsumer out,PoseStack.Pose pose,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,double cx,double cy,double cz){
        out.addVertex(pose,(float)(a.x-cx),(float)(a.y-cy),(float)(a.z-cz)).setColor(color);
        out.addVertex(pose,(float)(b.x-cx),(float)(b.y-cy),(float)(b.z-cz)).setColor(color);
        out.addVertex(pose,(float)(c.x-cx),(float)(c.y-cy),(float)(c.z-cz)).setColor(color);
        out.addVertex(pose,(float)(d.x-cx),(float)(d.y-cy),(float)(d.z-cz)).setColor(color);
    }
}
