package dev.sqwat.vela.ui.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import org.joml.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PngTextBatchTest {
    private static VertexConsumer sink(List<float[]> vertices){
        return (VertexConsumer)Proxy.newProxyInstance(PngTextBatchTest.class.getClassLoader(),new Class[]{VertexConsumer.class},(proxy,method,args)->{
            switch(method.getName()){
                case "addVertexWith2DPose" -> {
                    var pose=(Matrix3x2fc)args[0];var p=pose.transformPosition(((Number)args[1]).floatValue(),((Number)args[2]).floatValue(),new Vector2f());
                    vertices.add(new float[]{p.x,p.y,0,0});
                }
                case "setUv" -> {vertices.getLast()[2]=(float)args[0];vertices.getLast()[3]=(float)args[1];}
            }
            return proxy;
        });
    }
    @Test void batchVerticesMatchOldPerGlyphBlits(){
        var random=new java.util.Random(347);
        for(int trial=0;trial<200;trial++){
            var pose=new Matrix3x2f().translate(random.nextFloat()*100,random.nextFloat()*100).rotate(random.nextFloat()).scale(.5f+random.nextFloat()*3);
            float[] data=new float[8*20];var old=new ArrayList<float[]>();
            for(int i=0;i<20;i++){
                float x=i*8.15f,y=-3.3f,width=7.7f,height=12.3f;int p=i*8;
                data[p]=x;data[p+1]=y;data[p+2]=x+width;data[p+3]=y+height;
                data[p+4]=.1f;data[p+5]=.2f;data[p+6]=.3f;data[p+7]=.4f;
                var glyphPose=new Matrix3x2f(pose).translate(x,y).scale(width/11,height/17);
                new BlitRenderState(null,null,glyphPose,0,0,11,17,.1f,.3f,.2f,.4f,-1,null).buildVertices(sink(old));
            }
            var actual=new ArrayList<float[]>();var state=new PngTextRenderState(pose,data,null,-1,null);
            pose.identity(); // State owns a snapshot, not the caller's mutable stack.
            state.buildVertices(sink(actual));assertEquals(old.size(),actual.size());
            for(int i=0;i<old.size();i++)assertArrayEquals(old.get(i),actual.get(i),.0002f);
        }
    }
    @Test void clipAndEmptyRunHaveCorrectBounds(){
        float[] quad={-1.2f,-2.3f,12.4f,15.7f,0,0,1,1};var clip=new ScreenRectangle(0,0,10,10);
        var state=new PngTextRenderState(new Matrix3x2f(),quad,null,-1,clip);
        assertEquals(clip,state.bounds());assertEquals(clip,state.scissorArea());
        assertNull(new PngTextRenderState(new Matrix3x2f(),new float[0],null,-1,null).bounds());
        assertNull(new PngTextRenderState(new Matrix3x2f().translate(100,100),quad,null,-1,clip).bounds());
    }
}
