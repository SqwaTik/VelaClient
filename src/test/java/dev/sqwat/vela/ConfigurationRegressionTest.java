package dev.sqwat.vela;

import dev.sqwat.vela.module.visual.Shaders;
import dev.sqwat.vela.config.ConfigService;
import dev.sqwat.vela.module.*;
import dev.sqwat.vela.module.setting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationRegressionTest {
    @TempDir Path directory;
    @Test void loadingProfileUpdatesShaderThemeWithoutOpeningUi() throws Exception {
        int previous=dev.sqwat.vela.ui.render.VelaTheme.PRIMARY;
        try(var configs=new ConfigService(directory,new ModuleRegistry())) {
            configs.current().accentColor="#2060FF";
            configs.saveToProfile("blue");
            configs.current().accentColor="#FF2040";
            configs.saveToProfile("red");
            configs.selectProfile("blue");
            assertEquals(0xFF2060FF,new Shaders().handColor());
            configs.selectProfile("red");
            assertEquals(0xFFFF2040,new Shaders().handColor());
        } finally {dev.sqwat.vela.ui.render.VelaTheme.setPrimary(previous);}
    }
    @Test void unboundHoldModeDoesNotDiscardEnabledHudOnSave(){
        var registry=new ModuleRegistry();var module=registry.register(new Module());
        try(var configs=new ConfigService(directory,registry)){
            module.bind(-1,"None",ClientModule.BindMode.HOLD);module.setEnabled(true);
            assertEquals(ClientModule.BindMode.TOGGLE,module.bindMode());
        }
        var reloaded=new ModuleRegistry();var restored=reloaded.register(new Module());
        try(var configs=new ConfigService(directory,reloaded)){assertTrue(restored.enabled());}
    }
    static final class Module extends ClientModule {
        int starts, stops;
        @Override protected void onEnable(){starts++;}
        @Override protected void onDisable(){stops++;}
        final NumberSetting number = setting(new NumberSetting("value","Value","",0.3,0.1,1.1,0.2));
        Module(){super("test","Test","",ModuleCategory.HUD);}
    }
    @Test void identicalProfilePreservesRuntimeButChangedSettingsRestartIt() throws Exception {
        var registry=new ModuleRegistry();var module=registry.register(new Module());
        try(var configs=new ConfigService(directory,registry)){
            module.setEnabled(true);module.number.set(.9);
            configs.saveToProfile("same");
            configs.selectProfile("same");
            assertEquals(1,module.starts);assertEquals(0,module.stops);
            module.number.set(.5);
            configs.selectProfile("default");
            assertEquals(.9,module.number.value(),.0001);
            assertEquals(2,module.starts);assertEquals(1,module.stops);
        }
    }
    @Test void startsWithoutExistingConfigurationAndRestoresDefaultsBetweenProfiles(){
        var registry=new ModuleRegistry();var module=registry.register(new Module());
        try(var configs=new ConfigService(directory,registry)){
            assertNotNull(configs.current());
            module.number.set(0.9);module.setEnabled(true);
            configs.selectProfile("second");
            assertFalse(module.enabled());assertEquals(0.3,module.number.value(),0.0001);
            configs.selectProfile("default");
            assertTrue(module.enabled());assertEquals(0.9,module.number.value(),0.0001);
        }
    }
    @Test void sliderSnapsRelativeToMinimumAndClamps(){
        var number=new NumberSetting("test","Test","",0.1,0.1,1.1,0.2);
        number.setFraction(0.5);assertEquals(0.7,number.value(),0.0001);
        number.setFraction(-100);assertEquals(0.1,number.value(),0.0001);
        number.setFraction(100);assertEquals(1.1,number.value(),0.0001);
        assertThrows(IllegalArgumentException.class,()->number.set(Double.NaN));
    }
    @Test void renamingActiveProfileKeepsPendingChangesAndSurvivesRestart()throws Exception{
        var registry=new ModuleRegistry();var module=registry.register(new Module());
        try(var configs=new ConfigService(directory,registry)){
            module.number.set(.9);configs.requestSave();
            assertEquals("renamed",configs.renameProfile("default","renamed"));
            assertEquals("renamed",configs.current().activeProfile);
            assertFalse(java.nio.file.Files.exists(directory.resolve("profiles/default.json")));
        }
        try(var configs=new ConfigService(directory,registry)){
            assertEquals("renamed",configs.current().activeProfile);assertEquals(.9,module.number.value(),.0001);
            assertFalse(configs.profiles().contains("default"));
        }
    }
    @Test void renameRefusesExistingTargetAndPathTraversal()throws Exception{
        try(var configs=new ConfigService(directory,new ModuleRegistry())){
            configs.createProfile("second");
            assertThrows(java.io.IOException.class,()->configs.renameProfile("second","default"));
            assertThrows(java.io.IOException.class,()->configs.renameProfile("second","../escape"));
            assertEquals("second",configs.current().activeProfile);
            assertEquals("third",configs.renameProfile("default","third"));
            assertEquals("second",configs.current().activeProfile);
        }
    }
    @Test void holdBindingIsPersistedButPressedStateIsNot(){
        var registry=new ModuleRegistry();var module=registry.register(new Module());
        try(var configs=new ConfigService(directory,registry)){
            module.bind(70,"F",ClientModule.BindMode.HOLD);module.setEnabled(true);configs.requestSave();
        }
        try(var configs=new ConfigService(directory,registry)){
            assertEquals(ClientModule.BindMode.HOLD,module.bindMode());assertEquals(70,module.boundKey());assertFalse(module.enabled());
        }
    }
    @Test void immediateFlushCannotBeOverwrittenByOlderDebouncedSave() throws Exception {
        var registry=new ModuleRegistry();var module=registry.register(new Module());
        try(var configs=new ConfigService(directory,registry)){
            module.number.set(0.5);configs.requestSave();module.number.set(1.1);configs.flush();
        }
        try(var configs=new ConfigService(directory,registry)){assertEquals(1.1,module.number.value(),0.0001);}
    }
}
