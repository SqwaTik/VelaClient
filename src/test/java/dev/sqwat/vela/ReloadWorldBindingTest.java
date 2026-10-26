package dev.sqwat.vela;
import dev.sqwat.vela.render.ReloadWorldBinding;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReloadWorldBindingTest {
    @Test void bindsWorldThatArrivesAfterOverlayCreation(){
        var binding=new ReloadWorldBinding();var world=new Object();
        assertFalse(binding.visible(null,true));
        assertTrue(binding.visible(world,true));
        assertTrue(binding.visible(world,true));
        assertFalse(binding.visible(new Object(),true));
        assertFalse(binding.visible(null,true));
    }
    @Test void startupRemainsFullScreen(){assertFalse(new ReloadWorldBinding().visible(new Object(),false));}
}
