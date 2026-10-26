package dev.sqwat.vela;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import static org.junit.jupiter.api.Assertions.*;

/** Mixin targets fail only at game start; these check them against the real Minecraft 26.2 classes. */
class RecentMixinTargetsTest {
    private static ClassNode load(String name)throws Exception{
        try(var in=RecentMixinTargetsTest.class.getResourceAsStream("/"+name.replace('.','/')+".class")){
            assertNotNull(in,name);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;
        }
    }
    private static MethodNode method(ClassNode node,String name,String desc){
        return node.methods.stream().filter(m->m.name.equals(name)&&(desc==null||m.desc.equals(desc))).findFirst().orElse(null);
    }
    private static boolean calls(MethodNode method,String owner,String name){
        for(var insn:method.instructions)if(insn instanceof MethodInsnNode call&&call.owner.equals(owner)&&call.name.equals(name))return true;
        return false;
    }
    @Test void attackSoundHookTargetsEveryPositionalSound()throws Exception{
        var level=load("net.minecraft.client.multiplayer.ClientLevel");
        assertNotNull(method(level,"playSound","(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V"));
        assertNotNull(method(load("net.minecraft.client.multiplayer.ClientPacketListener"),"handleSoundEntityEvent",null));
    }
    @Test void wastedBlocksImplicitDeathScreenWithoutReentry()throws Exception{
        var setScreen=method(load("net.minecraft.client.gui.Gui"),"setScreen","(Lnet/minecraft/client/gui/screens/Screen;)V");
        assertNotNull(setScreen);
        assertTrue(calls(setScreen,"net/minecraft/client/player/LocalPlayer","isDeadOrDying"));
        var death=load("net.minecraft.client.gui.screens.DeathScreen");
        for(String m:new String[]{"extractBackground","visitText","extractRenderState"})assertNotNull(method(death,m,null),m);
    }
    @Test void brewPredictionUsesClientBrewingRules()throws Exception{
        var brewing=load("net.minecraft.world.item.alchemy.PotionBrewing");
        assertNotNull(method(brewing,"mix","(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"));
        assertNotNull(method(brewing,"hasMix","(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"));
    }
    private static boolean field(ClassNode node,String name){return node.fields.stream().anyMatch(f->f.name.equals(name));}
    @Test void chatAndContainerSearchTargets()throws Exception{
        var chat=load("net.minecraft.client.gui.components.ChatComponent");
        assertTrue(field(chat,"allMessages"));
        assertNotNull(method(chat,"refreshTrimmedMessages","()V"));
        assertNotNull(method(chat,"addMessage",null));
        for(String m:new String[]{"addMessageToDisplayQueue","addMessageToQueue"}){
            var queue=method(chat,m,null);assertNotNull(queue,m);
            assertTrue(java.util.stream.StreamSupport.stream(queue.instructions.spliterator(),false)
                .anyMatch(i->i instanceof org.objectweb.asm.tree.IntInsnNode n&&n.operand==100),m+" history cap");
        }
        var disconnect=method(load("net.minecraft.client.gui.Hud"),"onDisconnected","()V");
        assertNotNull(disconnect);assertTrue(calls(disconnect,"net/minecraft/client/gui/components/ChatComponent","clearMessages"));
        assertNotNull(method(load("net.minecraft.client.gui.components.PlayerTabOverlay"),"extractPingIcon",
            "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIILnet/minecraft/client/multiplayer/PlayerInfo;)V"));
        var screen=load("net.minecraft.client.gui.screens.inventory.AbstractContainerScreen");
        for(String f:new String[]{"menu","leftPos","topPos","imageWidth"})assertTrue(field(screen,f),f);
        var contents=method(screen,"extractContents","(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V");
        assertNotNull(contents);
        assertTrue(calls(contents,"net/minecraft/client/gui/screens/inventory/AbstractContainerScreen","extractSlotHighlightFront"));
    }
}
