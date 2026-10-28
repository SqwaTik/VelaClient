package dev.sqwat.vela;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sqwat.vela.module.visual.SwingAnimations;
import dev.sqwat.vela.module.setting.*;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwingAnimationsTest {
    private static Setting<?> setting(SwingAnimations m,String id){return m.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();}
    @Test void optInAndNoInterferenceWithUseOrEditor(){
        var m=new SwingAnimations();assertFalse(m.applies(true,false,false));m.setEnabled(true);
        assertTrue(m.applies(false,false,false));assertFalse(m.applies(true,true,false));assertFalse(m.applies(true,false,true));
        ((BooleanSetting)setting(m,"only_target")).set(true);assertFalse(m.applies(true,false,false,false));assertTrue(m.applies(true,false,false,true));
        ((BooleanSetting)setting(m,"swords_only")).set(true);assertFalse(m.applies(false,false,false,true));
        m.setEnabled(false);assertFalse(m.applies(true,false,false));
    }
    @Test void allReferenceModesHaveFiniteDeterministicPoses(){
        var m=new SwingAnimations();var mode=(ChoiceSetting)setting(m,"mode");
        assertEquals(10,SwingAnimations.MODES.size());
        for(String name:SwingAnimations.MODES){
            mode.set(name);
            for(boolean right:new boolean[]{true,false})for(float p:new float[]{0,.01f,.25f,.5f,.9f,1}){
                var a=new PoseStack();var b=new PoseStack();m.apply(a,right,p);m.apply(b,right,p);
                assertTrue(a.last().pose().isFinite(),name);assertEquals(a.last().pose(),b.last().pose(),name);
            }
        }
        var pose=new PoseStack();m.apply(pose,true,Float.NaN);assertEquals(new Matrix4f(),pose.last().pose());
        mode.set("Стандарт");m.setEnabled(true);assertFalse(m.applies(true,false,false));
    }
    /** Every mode starts and ends at rest, like vanilla: the item never stays turned between swings. */
    @Test void everyModeRestsBeforeAndAfterTheSwing(){
        var m=new SwingAnimations();var mode=(ChoiceSetting)setting(m,"mode");
        for(String name:SwingAnimations.MODES){
            mode.set(name);
            for(float p:new float[]{0,1}){
                var pose=new PoseStack();m.apply(pose,true,p);
                assertTrue(pose.last().pose().equals(new Matrix4f(),1e-4f),name+" at "+p);
            }
        }
    }
    @Test void durationUsesIndependentVisualTimelineAndResets(){
        var m=new SwingAnimations();((NumberSetting)setting(m,"duration")).set(2d);
        assertEquals(.25f,m.progress(.5f,3,6,false),.0001f);
        assertEquals(.5f,m.progress(0,6,6,false),.0001f);
        assertEquals(0,m.progress(0,12,6,false));
        m.reset();assertEquals(0,m.progress(0,13,6,false));
        assertEquals(.5f,m.progress(0,6,6,true),.0001f);
        assertEquals(0,m.progress(0,6,6,false));
    }
    @Test void backToBackSwingsRestartAndElasticEndpointsAreExact(){
        var m=new SwingAnimations();m.progress(.9f,5.4,6,false);
        assertEquals(.1f,m.progress(.1f,6.6,6,false),.0001f);
        assertEquals(0,SwingAnimations.elastic(0));assertEquals(1,SwingAnimations.elastic(1));
    }
}
