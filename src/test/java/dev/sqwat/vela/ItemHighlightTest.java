package dev.sqwat.vela;
import dev.sqwat.vela.module.visual.ItemHighlight;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ItemHighlightTest {
    @Test void nativeItemHookExistsExactlyOnce()throws Exception{
        try(var in=getClass().getResourceAsStream("/net/minecraft/client/gui/GuiGraphicsExtractor.class")){
            assertNotNull(in);var node=new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(in).accept(node,org.objectweb.asm.ClassReader.SKIP_CODE);
            assertEquals(1,node.methods.stream().filter(m->m.name.equals("item")&&m.desc.equals("(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V")).count());
        }
    }
    @Test void importantItemsHaveCategoriesAndOrdinaryItemsDoNot(){
        assertEquals("totems",ItemHighlight.category("totem_of_undying"));
        assertEquals("pearls",ItemHighlight.category("ender_pearl"));
        assertEquals("potions",ItemHighlight.category("splash_potion"));
        assertEquals("apples",ItemHighlight.category("enchanted_golden_apple"));
        assertNull(ItemHighlight.category("stone"));
    }
    @Test void disabledAndDeselectedItemsAreInert(){
        var m=new ItemHighlight();assertEquals(0,m.background("minecraft:totem_of_undying"));
        m.setEnabled(true);assertNotEquals(0,m.background("minecraft:totem_of_undying"));
        ((dev.sqwat.vela.module.setting.BooleanSetting)m.settings().stream().filter(s->s.id().equals("totems")).findFirst().orElseThrow()).set(false);
        assertEquals(0,m.background("minecraft:totem_of_undying"));assertEquals(0,m.background("minecraft:stone"));
        m.setEnabled(false);assertEquals(0,m.background("ender_pearl"));
    }
}
