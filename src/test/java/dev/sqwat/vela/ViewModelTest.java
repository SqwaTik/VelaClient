package dev.sqwat.vela;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sqwat.vela.module.ViewModel;
import dev.sqwat.vela.module.setting.*;
import net.minecraft.world.InteractionHand;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ViewModelTest {
    @Test void previewAnchorCannotCrossCameraOrViewport(){
        var model=new ViewModel();model.setTransform(0,1);model.setTransform(1,-1);model.setTransform(2,1);
        model.keepVisible(InteractionHand.MAIN_HAND,true,1,1.7f);
        var v=model.transforms();double depth=.72-v.get(2).value();
        assertTrue(depth>=.31);
        assertTrue(Math.abs((.56+v.get(0).value())/depth)<.88);
        assertTrue(Math.abs((-.30+v.get(1).value())*1.7/depth)<.88);
        assertEquals(0,model.transforms(InteractionHand.OFF_HAND).get(0).value());
    }
    @Test void selectionNeverTransfersOrResetsHandTransforms(){
        var model=new ViewModel();model.preview(true);
        model.selectHand("Основная");model.setTransform(0,.4);
        model.selectHand("Вторая");model.setTransform(0,-.3);
        for(String selection:java.util.List.of("Обе","Основная","Вторая","Обе")){
            model.selectHand(selection);
            var main=new PoseStack();var off=new PoseStack();
            model.apply(main,InteractionHand.MAIN_HAND);model.apply(off,InteractionHand.OFF_HAND);
            assertEquals(.4f,main.last().pose().m30());assertEquals(-.3f,off.last().pose().m30());
        }
        model.setTransform(1,.2);
        model.selectHand("Вторая");assertEquals(0,model.transforms().get(1).value(),1e-6);
        model.selectHand("Основная");assertEquals(.2,model.transforms().get(1).value(),1e-6);
    }
    @Test void bothModeEditsOnlyCapturedHandAndResetClearsBoth(){
        var model=new ViewModel();model.selectHand("Основная");model.setTransform(0,.4);
        model.selectHand("Вторая");model.setTransform(0,-.3);
        model.selectHand("Обе");model.setTransform(0,.5);
        model.selectHand("Вторая");assertEquals(-.3,model.transforms().getFirst().value(),1e-6);
        model.selectHand("Обе");model.editHand(InteractionHand.OFF_HAND);model.setTransform(0,-.6);
        assertEquals(.5,model.transforms(InteractionHand.MAIN_HAND).getFirst().value(),1e-6);
        assertEquals(-.6,model.transforms(InteractionHand.OFF_HAND).getFirst().value(),1e-6);
        model.selectHand("Обе");model.resetTransforms();assertEquals(0,model.transforms().getFirst().value(),1e-6);
        model.selectHand("Вторая");assertEquals(0,model.transforms().getFirst().value(),1e-6);
    }
    @Test void legacyOffhandSettingsStayOnOffhand(){
        var model=new ViewModel();model.transforms().getFirst().set(.4);
        model.hand().set("Вторая");model.migrateLegacy();model.preview(true);
        var main=new PoseStack();var off=new PoseStack();model.apply(main,InteractionHand.MAIN_HAND);model.apply(off,InteractionHand.OFF_HAND);
        assertEquals(new Matrix4f(),main.last().pose());assertEquals(.4f,off.last().pose().m30());
    }
    @Test void scalingKeepsHandOriginFixedAndShrinksGeometry(){
        var module=new ViewModel();module.setEnabled(true);module.transforms().get(6).set(.5);
        var pose=new PoseStack();module.apply(pose,InteractionHand.MAIN_HAND);
        var origin=new org.joml.Vector3f(.56f,-.52f,-.72f);
        var fixed=new org.joml.Vector3f(origin).mulPosition(pose.last().pose());
        assertEquals(0,origin.distance(fixed),1e-6);
        var edge=new org.joml.Vector3f(origin).add(.2f,0,0).mulPosition(pose.last().pose());
        assertEquals(.1,edge.distance(fixed),1e-6);
    }
    @Test void fractionalWheelEventsAccumulate(){
        var module=new ViewModel();
        for(int i=0;i<20;i++)module.wheel(.1,false,false);
        assertEquals(1.05,module.transforms().get(6).value(),.001);
        for(int i=0;i<20;i++)module.wheel(.1,true,false);
        assertEquals(6,module.transforms().get(5).value(),.001);
    }
    @Test void previewDoesNotEnableModuleAndEndsCleanly(){
        var module=new ViewModel();module.transforms().getFirst().set(.4);
        module.preview(true);var pose=new PoseStack();module.apply(pose,InteractionHand.MAIN_HAND);
        assertEquals(.4f,pose.last().pose().m30());assertFalse(module.enabled());
        module.preview(false);pose=new PoseStack();module.apply(pose,InteractionHand.MAIN_HAND);
        assertEquals(new Matrix4f(),pose.last().pose());
    }
    @Test void mouseWheelControlsAreBoundedAndIndependent(){
        var module=new ViewModel();module.wheel(10,false,false);
        assertEquals(1.25,module.transforms().get(6).value(),.001);
        module.wheel(2,true,false);assertEquals(6,module.transforms().get(5).value(),.001);
        module.wheel(1000,false,true);assertEquals(1,module.transforms().get(2).value(),.001);
    }
    @Test void disabledAndDefaultAreIdentity(){
        var module=new ViewModel();var pose=new PoseStack();
        module.apply(pose,InteractionHand.MAIN_HAND);assertEquals(new Matrix4f(),pose.last().pose());
        module.setEnabled(true);module.apply(pose,InteractionHand.MAIN_HAND);assertEquals(new Matrix4f(),pose.last().pose());
    }
    @Test void selectionDoesNotChangeOtherHand(){
        var module=new ViewModel();module.setEnabled(true);
        for(var setting:module.settings()){
            if(setting.id().equals("hand"))((ChoiceSetting)setting).set("Основная");
            if(setting.id().equals("offset_x"))((NumberSetting)setting).set(.5);
        }
        var pose=new PoseStack();module.apply(pose,InteractionHand.OFF_HAND);
        assertEquals(new Matrix4f(),pose.last().pose());
        module.apply(pose,InteractionHand.MAIN_HAND);assertEquals(.5f,pose.last().pose().m30());
    }
}
