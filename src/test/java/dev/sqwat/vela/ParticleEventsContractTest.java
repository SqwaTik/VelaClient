package dev.sqwat.vela;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.junit.jupiter.api.Assertions.*;

class ParticleEventsContractTest {
    private ClassNode read(String name)throws Exception{
        try(var in=getClass().getClassLoader().getResourceAsStream(name+".class")){
            assertNotNull(in);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;
        }
    }
    @Test void shulkerHooksMatchVanillaTooltipContract()throws Exception{
        assertTrue(read("net/minecraft/world/item/ItemStack").methods.stream().anyMatch(m->m.name.equals("addToTooltip")&&m.desc.equals("(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V")));
        var screen=read("net/minecraft/client/gui/screens/Screen").methods.stream().filter(m->m.name.equals("extractRenderStateWithTooltipAndSubtitles")).findFirst().orElseThrow();
        assertEquals("(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",screen.desc);
        assertTrue(java.util.Arrays.stream(screen.instructions.toArray()).anyMatch(i->i instanceof MethodInsnNode call&&call.name.equals("extractDeferredElements")&&call.desc.equals("(IIF)V")));
        var container=read("net/minecraft/client/gui/screens/inventory/AbstractContainerScreen");
        assertTrue(container.methods.stream().anyMatch(m->m.name.equals("extractTooltip")&&m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V")));
        assertTrue(container.methods.stream().anyMatch(m->m.name.equals("mouseClicked")&&m.desc.equals("(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z")));
        assertTrue(read("net/minecraft/world/item/ItemStack").methods.stream().anyMatch(m->m.name.equals("getTooltipImage")&&m.desc.equals("()Ljava/util/Optional;")));
        var tooltip=read("net/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent");
        assertTrue(tooltip.methods.stream().anyMatch(m->m.name.equals("create")&&m.desc.equals("(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;")));
        assertTrue(tooltip.methods.stream().anyMatch(m->m.name.equals("extractImage")&&m.desc.equals("(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/GuiGraphicsExtractor;)V")));
    }
    @Test void crosshairReplacementTargetsOnlyFirstSpriteAndPreservesAttackIndicators()throws Exception{
        var method=read("net/minecraft/client/gui/Hud").methods.stream().filter(m->m.name.equals("extractCrosshair")).findFirst().orElseThrow();
        var calls=java.util.Arrays.stream(method.instructions.toArray()).filter(i->i instanceof MethodInsnNode call&&call.name.equals("blitSprite")&&call.desc.equals("(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")).toList();
        assertTrue(calls.size()>=2);
        var first=calls.getFirst();boolean crosshair=false;
        for(var i=first.getPrevious();i!=null;i=i.getPrevious())if(i instanceof org.objectweb.asm.tree.FieldInsnNode f&&f.name.equals("CROSSHAIR_SPRITE")){crosshair=true;break;}
        assertTrue(crosshair);
    }
    @Test void hitColorHooksMatchNativePlayerRendering()throws Exception{
        var renderer=read("net/minecraft/client/renderer/entity/LivingEntityRenderer");
        assertTrue(renderer.methods.stream().anyMatch(m->m.name.equals("getModelTint")&&m.desc.equals("(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)I")));
        assertTrue(renderer.methods.stream().anyMatch(m->m.name.equals("getOverlayCoords")&&m.desc.equals("(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)I")));
        var avatar=read("net/minecraft/client/renderer/entity/player/AvatarRenderer");
        assertTrue(avatar.methods.stream().noneMatch(m->m.name.equals("getModelTint")));
    }
    @Test void atmosphereAndAnimationHooksExistInCurrentMinecraft()throws Exception{
        assertTrue(read("net/minecraft/client/renderer/SkyRenderer").methods.stream().anyMatch(m->m.name.equals("extractRenderState")&&m.desc.equals("(Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/state/level/SkyRenderState;)V")));
        var chat=read("net/minecraft/client/gui/components/ChatComponent");
        assertEquals(1,chat.methods.stream().filter(m->m.name.equals("addMessage")).count());
        assertTrue(chat.fields.stream().anyMatch(f->f.name.equals("chatScrollbarPos")&&f.desc.equals("I")));
        assertTrue(read("net/minecraft/client/gui/screens/inventory/AbstractContainerScreen").methods.stream().anyMatch(m->m.name.equals("init")&&m.desc.equals("()V")));
        var hud=read("net/minecraft/client/gui/Hud");
        assertTrue(hud.methods.stream().anyMatch(m->m.name.equals("extractItemHotbar")));
        assertTrue(hud.methods.stream().anyMatch(m->m.name.equals("extractTabList")));
    }
    @Test void packetHooksMatchActualMinecraftMethods()throws Exception{
        var target=read("net/minecraft/client/multiplayer/ClientPacketListener");
        assertTrue(target.methods.stream().anyMatch(m->m.name.equals("handleHurtAnimation")&&m.desc.equals("(Lnet/minecraft/network/protocol/game/ClientboundHurtAnimationPacket;)V")));
        assertTrue(target.methods.stream().anyMatch(m->m.name.equals("handleEntityEvent")&&m.desc.equals("(Lnet/minecraft/network/protocol/game/ClientboundEntityEventPacket;)V")));
    }
    @Test void newInputWeatherAndAnimationHooksMatchNativeMethods()throws Exception{
        assertTrue(read("net/minecraft/client/KeyboardHandler").methods.stream().anyMatch(m->m.name.equals("keyPress")&&m.desc.equals("(JILnet/minecraft/client/input/KeyEvent;)V")));
        assertTrue(read("net/minecraft/client/MouseHandler").methods.stream().anyMatch(m->m.name.equals("onButton")&&m.desc.equals("(JLnet/minecraft/client/input/MouseButtonInfo;I)V")));
        var level=read("net/minecraft/world/level/Level");
        assertTrue(read("net/minecraft/client/ClientClockManager").methods.stream().anyMatch(m->m.name.equals("getTotalTicks")&&m.desc.equals("(Lnet/minecraft/core/Holder;)J")));
        for(String method:java.util.List.of("getDefaultClockTime","getRainLevel","getThunderLevel"))assertTrue(level.methods.stream().anyMatch(m->m.name.equals(method)));
        var tick=read("dev/sqwat/vela/module/FakePlayer$LocalDummy").methods.stream().filter(m->m.name.equals("tick")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(tick.instructions.toArray()).anyMatch(i->i instanceof MethodInsnNode call&&call.name.equals("calculateEntityAnimation")));
    }
    @Test void fakePlayerAcceptedDamageEmitsFeedbackAndNativeHideDoesNotBlockIt()throws Exception{
        var fake=read("dev/sqwat/vela/module/FakePlayer$LocalDummy");
        var receive=fake.methods.stream().filter(m->m.name.equals("receive")).findFirst().orElseThrow();
        int emissions=0;
        for(var instruction:receive.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals("dev/sqwat/vela/module/Particles")&&call.name.equals("emit"))emissions++;
        assertEquals(2,emissions,"Confirmed hit and local totem must both emit");
        var effect=read("dev/sqwat/vela/module/Particles");
        for(var method:effect.methods)for(var instruction:method.instructions)
            if(instruction instanceof MethodInsnNode call)assertNotEquals("dev/sqwat/vela/module/RenderTweaks",call.owner);
    }
}
