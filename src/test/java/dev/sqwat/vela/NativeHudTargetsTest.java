package dev.sqwat.vela;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class NativeHudTargetsTest {
    @Test void utilityTargetsMatchRuntime() throws Exception {
        var camera=methods("net/minecraft/client/Camera");
        assertTrue(camera.contains("setupPerspective(FFFFF)V"));
        assertTrue(camera.contains("createProjectionMatrixForCulling()Lorg/joml/Matrix4f;"));
        assertTrue(methods("net/minecraft/client/renderer/LevelRenderer").contains("submitBlockOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V"));
        assertTrue(methods("net/minecraft/client/multiplayer/ClientPacketListener").contains("handleDamageEvent(Lnet/minecraft/network/protocol/game/ClientboundDamageEventPacket;)V"));
    }
    @Test void labelCacheTargetsMatchRuntime() throws Exception {
        assertTrue(methods("net/minecraft/client/renderer/feature/NameTagFeatureRenderer").contains("prepareText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/renderer/feature/NameTagFeatureRenderer$Submit;)Lnet/minecraft/client/gui/Font$PreparedText;"));
        var font = methods("net/minecraft/client/gui/font/FontManager");
        assertTrue(font.contains("apply(Lnet/minecraft/client/gui/font/FontManager$Preparation;Lnet/minecraft/util/profiling/ProfilerFiller;)V"));
        assertTrue(font.contains("updateOptions(Lnet/minecraft/client/Options;)V"));
        assertTrue(font.contains("close()V"));
    }
    @Test void emptyTextSubmissionTargetMatchesRuntime()throws Exception{
        assertTrue(methods("net/minecraft/client/renderer/SubmitNodeCollection").contains("submitText(Lcom/mojang/blaze3d/vertex/PoseStack;FFLnet/minecraft/util/FormattedCharSequence;ZLnet/minecraft/client/gui/Font$DisplayMode;IIII)V"));
    }
    @Test void mapFramePreparationTargetsMatchMinecraft() throws Exception {
        assertTrue(methods("net/minecraft/client/renderer/entity/ItemFrameRenderer").contains("extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V"));
        assertTrue(methods("net/minecraft/client/renderer/item/ItemModelResolver").contains("updateForNonLiving(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/Entity;)V"));
        // Registration is required: an unregistered optimization has no runtime effect.
        try(var stream=getClass().getResourceAsStream("/velaclient.mixins.json")) {
            assertNotNull(stream);
            assertTrue(new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).contains("\"MapFramePreparationMixin\""));
        }
    }
    @Test void hotbarGroupAndFarLimitTargetsExist()throws Exception{
        assertTrue(methods("net/minecraft/client/gui/Hud").contains("extractHotbarAndDecorations(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"));
        assertTrue(methods("de/johni0702/minecraft/bobby/BobbyConfig").contains("getMaxRenderDistance()I"));
    }
    @Test void interactiveReloadTargetsExist()throws Exception{
        for(var target:Map.of("net/minecraft/client/MouseHandler",List.of("onButton","onScroll","handleAccumulatedMovement"),"net/minecraft/client/KeyboardHandler",List.of("charTyped","preeditCallback"),"net/minecraft/client/Minecraft",List.of("tick")).entrySet()){
            var found=new HashSet<String>();
            try(var stream=getClass().getResourceAsStream("/"+target.getKey()+".class")){
                new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions){return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){if(owner.equals("net/minecraft/client/gui/Gui")&&name.equals("overlay")&&desc.equals("()Lnet/minecraft/client/gui/screens/Overlay;"))found.add(method);}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
            }
            assertTrue(found.containsAll(target.getValue()),target.getKey()+": "+found);
        }
        boolean[] barrier={false},command={false};
        try(var stream=getClass().getResourceAsStream("/net/minecraft/server/packs/resources/SimpleReloadInstance.class")){new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9){@Override public FieldVisitor visitField(int access,String name,String desc,String sig,Object value){if(name.equals("allPreparations")&&desc.equals("Ljava/util/concurrent/CompletableFuture;"))barrier[0]=true;return null;}},ClassReader.SKIP_CODE);}
        try(var stream=getClass().getResourceAsStream("/net/minecraft/client/gui/Gui.class")){new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int access,String method,String desc,String sig,String[] ex){if(!method.equals("handleKeybinds"))return null;return new MethodVisitor(Opcodes.ASM9){@Override public void visitFieldInsn(int op,String owner,String name,String desc){if(op==Opcodes.GETFIELD&&name.equals("overlay"))command[0]=true;}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);}
        assertTrue(barrier[0]);assertTrue(command[0]);
    }
    @Test void farCacheTargetsMatchBundledBobby()throws Exception{
        assertTrue(methods("de/johni0702/minecraft/bobby/FakeChunkManager").contains("update(ZLjava/util/function/BooleanSupplier;I)V"));
        var cache=methods("de/johni0702/minecraft/bobby/FakeChunkManager");
        assertTrue(cache.contains("load(IILnet/minecraft/world/level/chunk/LevelChunk;)V"));
        assertTrue(cache.contains("getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"));
        assertTrue(cache.contains("shouldBeLoaded(II)Z"));
        var config=methods("de/johni0702/minecraft/bobby/BobbyConfig");
        assertTrue(config.contains("isDynamicMultiWorld()Z"));assertTrue(config.contains("isNoBlockEntities()Z"));assertTrue(config.contains("getUnloadDelaySecs()I"));
    }
    @Test void mouseTurnInvocationMatchesZoomInjection()throws Exception{
        boolean[] found={false};
        try(var stream=getClass().getResourceAsStream("/net/minecraft/client/MouseHandler.class")){
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9){
                @Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){
                    if(!name.equals("turnPlayer"))return null;
                    return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){if(owner.equals("net/minecraft/client/player/LocalPlayer")&&name.equals("turn")&&desc.equals("(DD)V"))found[0]=true;}};
                }
            },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        }
        assertTrue(found[0]);
    }
    @Test void legacyHeadMixinTargetsExistWithoutStartingMinecraft()throws Exception{
        var targets=methods("net/minecraft/client/renderer/item/SpecialModelWrapper$Unbaked");
        assertTrue(targets.contains("bake(Lnet/minecraft/client/renderer/item/ItemModel$BakingContext;Lorg/joml/Matrix4fc;)Lnet/minecraft/client/renderer/item/ItemModel;"));
        assertTrue(targets.contains("base()Lnet/minecraft/resources/Identifier;"));
        assertTrue(targets.contains("transformation()Ljava/util/Optional;"));
        assertTrue(targets.contains("specialModel()Lnet/minecraft/client/renderer/special/SpecialModelRenderer$Unbaked;"));
    }
    @Test void nativeHudMixinTargetsExistWithoutStartingMinecraft()throws Exception{
        var gui=methods("net/minecraft/client/gui/GuiGraphicsExtractor");
        assertTrue(gui.contains("fill(IIIII)V"));
        assertTrue(gui.contains("text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V"));
        var hud=methods("net/minecraft/client/gui/Hud");
        for(String name:List.of("extractBossOverlay","extractScoreboardSidebar","extractTabList"))assertTrue(hud.contains(name+"(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"));
    }
    @Test void gridTextureIsPackaged(){assertNotNull(getClass().getResource("/assets/velaclient/textures/gui/hud_grid.png"));}
    private Set<String> methods(String name)throws Exception{
        var result=new HashSet<String>();try(var stream=getClass().getResourceAsStream("/"+name+".class")){assertNotNull(stream);new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions){result.add(method+descriptor);return null;}},ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);}return result;
    }
}
