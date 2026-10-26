package dev.sqwat.vela;

import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingVisibilityTest {
    @Test void dependentSettingsFollowTheirParentValue(){
        var parent=new BooleanSetting("parent","Parent","",false);
        var child=new NumberSetting("child","Child","",20,5,40,1).visibleWhen(parent::value);
        assertFalse(child.visible());parent.set(true);assertTrue(child.visible());parent.set(false);assertFalse(child.visible());
    }
}
