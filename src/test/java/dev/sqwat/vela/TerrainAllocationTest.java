package dev.sqwat.vela;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class TerrainAllocationTest {
    @Test void localSuggestionsAndPingHooksMatchResolvedMinecraft()throws Exception{
        var suggestions=read("net/minecraft/client/gui/components/CommandSuggestions");
        assertTrue(suggestions.methods.stream().anyMatch(m->m.name.equals("updateCommandInfo")&&m.desc.equals("()V")));
        for(String name:java.util.List.of("input","screen","commandUsage","keepSuggestions","currentParseIsCommand","currentParseIsMessage","currentParse","pendingSuggestions"))assertTrue(suggestions.fields.stream().anyMatch(f->f.name.equals(name)),name);
        assertTrue(read("net/minecraft/client/multiplayer/ClientPacketListener").methods.stream().anyMatch(m->m.name.equals("handlePongResponse")&&m.desc.equals("(Lnet/minecraft/network/protocol/ping/ClientboundPongResponsePacket;)V")));
    }
    @Test void pictureFilterTargetsMatchMinecraft() throws Exception {
        var frame=read("net/minecraft/client/renderer/entity/ItemFrameRenderer");
        var extract=frame.methods.stream().filter(m->m.name.equals("extractRenderState")&&m.desc.startsWith("(Lnet/minecraft/world/entity/decoration/ItemFrame;")).findFirst().orElseThrow();
        assertEquals(1,calls(extract,"net/minecraft/world/entity/decoration/ItemFrame","getItem"));
        var painting=read("net/minecraft/client/renderer/entity/PaintingRenderer");
        var submit=painting.methods.stream().filter(m->m.name.equals("submit")&&m.desc.startsWith("(Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;")).findFirst().orElseThrow();
        assertEquals(2,calls(submit,"net/minecraft/client/renderer/texture/TextureAtlas","getSprite"));
    }
    @Test void signWidthHookMatchesActualMinecraftCall() throws Exception {
        var node=read("net/minecraft/client/renderer/blockentity/AbstractSignRenderer");
        var submit=node.methods.stream().filter(m->m.name.equals("submitSignText")).findFirst().orElseThrow();
        assertEquals(1,java.util.Arrays.stream(submit.instructions.toArray()).filter(i->i instanceof MethodInsnNode m
                &&m.owner.equals("net/minecraft/client/gui/Font")&&m.name.equals("width")
                &&m.desc.equals("(Lnet/minecraft/util/FormattedCharSequence;)I")).count());
    }
    @Test void blockVisibilityHooksDoNotRequestPerEntityCallbacks() throws Exception {
        for(String name:java.util.List.of("dev/sqwat/vela/mixin/entity/StorageVisibilityMixin")){
            var node=read(name);
            assertTrue(node.methods.stream().noneMatch(m->m.desc.contains("CallbackInfo")));
            for(var method:node.methods)assertEquals(0,calls(method,
                    "net/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher","getRenderer"));
        }
    }
    @Test void terrainHookTargetsAndReadOnlyUniformContractMatchMinecraft() throws Exception {
        var renderer=read("net/minecraft/client/renderer/LevelRenderer");
        var prepare=renderer.methods.stream().filter(m->m.name.equals("prepareChunkRenders")).findFirst().orElseThrow();
        assertEquals(2,calls(prepare,"net/minecraft/client/renderer/chunk/ChunkSectionLayer","values"));
        assertEquals(1,calls(prepare,"org/joml/Matrix4f","<init>"));
        assertTrue(java.util.Arrays.stream(prepare.instructions.toArray()).anyMatch(i->i instanceof MethodInsnNode m
                &&m.owner.equals("org/joml/Matrix4f")&&m.name.equals("<init>")&&m.desc.equals("(Lorg/joml/Matrix4fc;)V")));
        var uniform=read("net/minecraft/client/renderer/DynamicUniforms$ChunkSectionInfo");
        assertTrue(uniform.fields.stream().anyMatch(f->f.name.equals("modelView")&&f.desc.equals("Lorg/joml/Matrix4fc;")));
        read("dev/sqwat/vela/mixin/perf/TerrainAllocationMixin");
        var gui=read("net/minecraft/client/gui/render/GuiRenderer");
        var guiPrepare=gui.methods.stream().filter(m->m.name.equals("prepare")).findFirst().orElseThrow();
        assertEquals(1,calls(guiPrepare,"net/minecraft/client/renderer/state/gui/GuiRenderState","sortElements"));
        var group=read("net/minecraft/client/renderer/feature/RenderTypeFeatureRenderer$Group");
        var addDraw=group.methods.stream().filter(m->m.name.equals("getOrAddDraw")).findFirst().orElseThrow();
        assertEquals(1,calls(addDraw,"java/util/List","indexOf"));
    }
    private static long calls(MethodNode method,String owner,String name){
        return java.util.Arrays.stream(method.instructions.toArray()).filter(i->i instanceof MethodInsnNode m&&m.owner.equals(owner)&&m.name.equals(name)).count();
    }
    private static ClassNode read(String name)throws Exception{
        try(var in=TerrainAllocationTest.class.getResourceAsStream("/"+name+".class")){
            assertNotNull(in);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;
        }
    }
}
