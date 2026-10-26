package dev.sqwat.vela.ui.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class WaveRenderStateTest {
    @Test void depthSurvivesGuiTransformWithoutChangingUvOrColor(){
        float[] mesh={0,0,0,0,1, 10,0,1,0,.5f, 10,20,1,1,.5f, 0,20,0,1,1};
        var state=new WaveRenderState(new Matrix3x2f().translate(3,4).scale(2),mesh,null,null,0xFFAABBCC,null);
        var positions=new ArrayList<float[]>();var uv=new ArrayList<float[]>();var colors=new ArrayList<Integer>();
        var consumer=(VertexConsumer)Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),new Class<?>[]{VertexConsumer.class},(proxy,method,args)->{
            switch(method.getName()){
                case "addVertex" -> positions.add(new float[]{(float)args[0],(float)args[1],(float)args[2]});
                case "setUv" -> uv.add(new float[]{(float)args[0],(float)args[1]});
                case "setColor" -> colors.add((int)args[0]);
                default -> throw new AssertionError(method.getName());
            }
            return proxy;
        });
        state.buildVertices(consumer);
        assertEquals(4,positions.size());assertArrayEquals(new float[]{3,4,1},positions.get(0));
        assertArrayEquals(new float[]{23,44,.5f},positions.get(2));
        assertArrayEquals(new float[]{1,1},uv.get(2));assertTrue(colors.stream().allMatch(c->c==0xFFAABBCC));
        assertEquals(20,state.bounds().width());assertEquals(40,state.bounds().height());
    }
}
