package dev.sqwat.vela;

import dev.sqwat.vela.render.perf.VertexPositions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class VertexPositionsTest {
    @Test void matchesJomlForIdentityTranslationRotationScaleAndGeneralMatrices() {
        var actual = new Vector3f();
        var sink = (VertexConsumer) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{VertexConsumer.class},
            (proxy, method, args) -> { actual.set((float)args[0], (float)args[1], (float)args[2]); return proxy; });
        var random = new Random(917);
        for (int i=0;i<1000;i++) {
            float x=random.nextFloat()*40-20,y=random.nextFloat()*40-20,z=random.nextFloat()*40-20;
            Matrix4f matrix=switch(i%4) {
                case 0 -> new Matrix4f();
                case 1 -> new Matrix4f().translation(x,y,z);
                case 2 -> new Matrix4f().translate(x,y,z).rotateXYZ(.3f,.8f,1.1f).scale(.7f,2f,3f);
                default -> new Matrix4f().perspective(1f,1.6f,.1f,100f).rotateY(.4f);
            };
            var expected=matrix.transformPosition(x,y,z,new Vector3f());
            assertSame(sink,VertexPositions.add(sink,matrix,x,y,z));
            assertEquals(expected.x,actual.x); assertEquals(expected.y,actual.y); assertEquals(expected.z,actual.z);
        }
    }
}
