package dev.sqwat.vela;
import dev.sqwat.vela.core.ConfigService;
import dev.sqwat.vela.module.ModuleRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class ProfileActionsTest {
 @TempDir Path root;
 @Test void multipleExpansionsSurvive()throws Exception{
  try(var s=new ConfigService(root,new ModuleRegistry())){s.current().expandedModules.addAll(java.util.List.of("armor","coordinates","inventory"));s.flush();}
  try(var s=new ConfigService(root,new ModuleRegistry())){assertEquals(3,s.current().expandedModules.size());}
 }
 @Test void lastProfileCanBeArchived()throws Exception{
  try(var s=new ConfigService(root,new ModuleRegistry())){s.archiveProfile("default");assertFalse(s.profiles().contains("default"));assertNotEquals("default",s.current().activeProfile);assertTrue(java.nio.file.Files.isDirectory(root.resolve("profiles/archive")));}
 }
 @Test void createCapturesCurrentSettings()throws Exception{
  try(var s=new ConfigService(root,new ModuleRegistry())){s.current().accentColor="#123456";s.createProfile("new");assertEquals("#123456",s.current().accentColor);}
 }
}
