package dev.sqwat.vela;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;
class UiIntegrationTargetsTest {
    private long calls(String owner,String method,String target,String name,String descriptor)throws Exception{
        ClassNode node=new ClassNode();
        try(var input=getClass().getResourceAsStream("/"+owner+".class")){assertNotNull(input,owner);new ClassReader(input).accept(node,0);}
        long count=0;
        for(var m:node.methods)if(m.name.equals(method))for(var ins:m.instructions)if(ins instanceof MethodInsnNode call&&call.owner.equals(target)&&call.name.equals(name)&&call.desc.equals(descriptor))count++;
        return count;
    }
    @Test void pointerTransformsTargetActualDispatchMethods()throws Exception{
        String mouse="net/minecraft/client/MouseHandler",screen="net/minecraft/client/gui/screens/Screen",event="Lnet/minecraft/client/input/MouseButtonEvent;";
        assertEquals(1,calls(mouse,"onButton",screen,"mouseClicked","("+event+"Z)Z"));
        assertEquals(1,calls(mouse,"onButton",screen,"mouseReleased","("+event+")Z"));
        assertEquals(1,calls(mouse,"handleAccumulatedMovement",screen,"mouseMoved","(DD)V"));
        assertEquals(1,calls(mouse,"handleAccumulatedMovement",screen,"mouseDragged","("+event+"DD)Z"));
        assertEquals(1,calls(mouse,"onScroll",screen,"mouseScrolled","(DDDD)Z"));
    }
    @Test void screenAndTabHooksExistInPackagedMinecraft()throws Exception{
        assertEquals(1,calls("net/minecraft/client/gui/Gui","extractRenderState","net/minecraft/client/gui/screens/Screen","extractRenderStateWithTooltipAndSubtitles","(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"));
        assertEquals(4,calls("net/minecraft/client/gui/components/PlayerTabOverlay","extractRenderState","net/minecraft/client/gui/GuiGraphicsExtractor","fill","(IIIII)V"));
    }
    @Test void unavailableModuleCannotBeEnabledByToggleOrConfiguration(){
        var module=new dev.sqwat.vela.module.ClientModule("test","Test","",dev.sqwat.vela.module.ModuleCategory.HUD){};
        module.setEnabled(true);assertTrue(module.enabled());
        module.require(()->false);assertFalse(module.enabled());module.toggle();assertFalse(module.enabled());module.setEnabled(true);assertFalse(module.enabled());
        module.require(()->true);module.toggle();assertTrue(module.enabled());
    }
}
