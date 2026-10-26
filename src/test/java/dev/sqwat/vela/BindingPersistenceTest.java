package dev.sqwat.vela;
import dev.sqwat.vela.config.ConfigService;
import dev.sqwat.vela.module.*;
import dev.sqwat.vela.ui.input.BindInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class BindingPersistenceTest {
    @TempDir Path root;
    private static ClientModule entry(){return new ClientModule("sample","Sample","",ModuleCategory.HUD){};}
    @Test void keyboardAndMouseBindingsSurviveClose(){for(int key:new int[]{65,BindInput.mouse(3)}){var registry=new ModuleRegistry();var module=registry.register(entry());try(var config=new ConfigService(root,registry)){module.bind(key,"Bind",ClientModule.BindMode.TOGGLE);config.requestSave();}var restoredRegistry=new ModuleRegistry();var restored=restoredRegistry.register(entry());try(var config=new ConfigService(root,restoredRegistry)){assertEquals(key,restored.boundKey());}}}
}
