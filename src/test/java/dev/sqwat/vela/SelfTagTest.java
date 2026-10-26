package dev.sqwat.vela;
import dev.sqwat.vela.module.visual.SelfTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SelfTagTest {
    @Test void tagOnlyShowsOwnLivingVisiblePlayerInThirdPerson(){
        var tag=new SelfTag();assertFalse(tag.visible(true,false,false,true));tag.setEnabled(true);
        assertTrue(tag.visible(true,false,false,true));assertFalse(tag.visible(false,false,false,true));
        assertFalse(tag.visible(true,true,false,true));assertFalse(tag.visible(true,false,true,true));assertFalse(tag.visible(true,false,false,false));
    }
    @Test void nativeTargetExists()throws Exception{
        try(var in=getClass().getResourceAsStream("/net/minecraft/client/renderer/entity/player/AvatarRenderer.class")){
            assertNotNull(in);var node=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(in).accept(node,org.objectweb.asm.ClassReader.SKIP_CODE);
            assertTrue(node.methods.stream().anyMatch(m->m.name.equals("shouldShowName")&&m.desc.equals("(Lnet/minecraft/world/entity/Avatar;D)Z")));
        }
    }
}
