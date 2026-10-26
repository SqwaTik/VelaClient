package dev.sqwat.vela;
import dev.sqwat.vela.module.CrystalOptimizer;
import dev.sqwat.vela.module.setting.KeySetting;
import dev.sqwat.vela.render.FriendNameTags;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PearlAndFriendTest {
    @Test void bindingSupportsNoneKeyboardAndMouse(){var key=new KeySetting("key","Клавиша");assertEquals(-1,key.value());key.set(65);key.set(1002);assertThrows(IllegalArgumentException.class,()->key.set(999));}
    @Test void crystalChargeBypassIsScopedToCrystalAndAnchorUseCountFillsThenUsesOnceMore(){
        assertTrue(CrystalOptimizer.ignoreLocalAttackCharge(true,true,true));
        assertFalse(CrystalOptimizer.ignoreLocalAttackCharge(false,true,true));
        assertFalse(CrystalOptimizer.ignoreLocalAttackCharge(true,false,true));
        assertFalse(CrystalOptimizer.ignoreLocalAttackCharge(true,true,false));
        assertEquals(5,CrystalOptimizer.remainingAnchorUses(0));assertEquals(4,CrystalOptimizer.remainingAnchorUses(1));
        assertEquals(1,CrystalOptimizer.remainingAnchorUses(4));assertEquals(1,CrystalOptimizer.remainingAnchorUses(5));
        assertTrue(CrystalOptimizer.anchorChargeAdvanced(3,4));assertFalse(CrystalOptimizer.anchorChargeAdvanced(4,4));
    }
    @Test void onlyMarkedNameGetsGreenBackgroundAndTransparentPassStaysTransparent(){
        Object friend=new Object(),other=new Object();FriendNameTags.mark(friend,true);
        assertEquals(0x506FD92B,FriendNameTags.background(friend,0x40000000));assertEquals(0,FriendNameTags.background(friend,0));
        assertEquals(0x40000000,FriendNameTags.background(other,0x40000000));FriendNameTags.mark(friend,false);assertEquals(0x40000000,FriendNameTags.background(friend,0x40000000));
    }
}
