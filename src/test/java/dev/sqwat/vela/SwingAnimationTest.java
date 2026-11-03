package dev.sqwat.vela;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sqwat.vela.module.visual.SwingAnimation;
import dev.sqwat.vela.module.setting.*;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class SwingAnimationTest {
    private static Setting<?> setting(SwingAnimation m,String id){return m.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();}
    @Test void optInAndNoInterferenceWithUseOrEditor(){
        var m=new SwingAnimation();assertFalse(m.applies(false,false,true));m.setEnabled(true);
        assertTrue(m.applies(false,false,false));assertFalse(m.applies(true,false,true));assertFalse(m.applies(false,true,true));
        ((BooleanSetting)setting(m,"only_target")).set(true);assertFalse(m.applies(false,false,false));assertTrue(m.applies(false,false,true));
        m.setEnabled(false);assertFalse(m.applies(false,false,true));
    }
    @Test void everyModeIsFiniteDeterministicAndDistinct(){
        var m=new SwingAnimation();var mode=(ChoiceSetting)setting(m,"mode");
        var mid=new HashSet<Matrix4f>();
        for(String name:SwingAnimation.MODES){
            mode.set(name);
            for(boolean right:new boolean[]{true,false})for(float p:new float[]{0,.01f,.25f,.5f,.9f,1}){
                var a=new PoseStack();var b=new PoseStack();m.apply(a,right,p);m.apply(b,right,p);
                assertTrue(a.last().pose().isFinite(),name);assertEquals(a.last().pose(),b.last().pose(),name);
            }
            var pose=new PoseStack();m.apply(pose,true,.4f);mid.add(new Matrix4f(pose.last().pose()));
        }
        assertEquals(SwingAnimation.MODES.size(),mid.size(),"no two modes move the same way");
        var pose=new PoseStack();m.apply(pose,true,Float.NaN);assertEquals(new Matrix4f(),pose.last().pose());
        mode.set(SwingAnimation.STANDARD);m.setEnabled(true);assertFalse(m.applies(false,false,true));
    }
    @Test void durationUsesIndependentVisualTimelineAndResets(){
        var m=new SwingAnimation();((NumberSetting)setting(m,"duration")).set(2d);
        assertEquals(0,m.progress(3,6,false));
        m.swingStarted(0,6);
        assertEquals(.25f,m.progress(3,6,false),.0001f);
        assertEquals(.5f,m.progress(6,6,false),.0001f);
        assertEquals(0,m.progress(12,6,false));
        m.swingStarted(13,6);m.reset();assertEquals(0,m.progress(14,6,false));
        assertEquals(.5f,m.progress(6,6,true),.0001f);
        assertEquals(0,m.progress(6,6,false));
    }
    @Test void echoesAndEarlyRepeatsDoNotRestartTheSwing(){
        var m=new SwingAnimation();
        m.swingStarted(0,6);
        SwingAnimation.serverEcho=true;try{m.swingStarted(2,6);}finally{SwingAnimation.serverEcho=false;}
        assertEquals(.5f,m.progress(3,6,false),.0001f);
        m.swingStarted(2,6); // still in the first half: like vanilla, no restart
        assertEquals(.5f,m.progress(3,6,false),.0001f);
        m.swingStarted(4,6); // second half: a new swing
        assertEquals(1/6f,m.progress(5,6,false),.0001f);
        assertEquals(0,SwingAnimation.elastic(0));assertEquals(1,SwingAnimation.elastic(1));
    }
}
