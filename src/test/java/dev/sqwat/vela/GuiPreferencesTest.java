package dev.sqwat.vela;
import dev.sqwat.vela.core.ConfigService;
import dev.sqwat.vela.friend.FriendService;
import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.ui.render.UiViewport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class GuiPreferencesTest {
 @TempDir Path root;
 @Test void viewAndFavoritesSurviveReopen()throws Exception{
  try(var c=new ConfigService(root,new ModuleRegistry())){c.current().lastSection="HUD";c.current().expandedModule="armor";c.current().favorites.add("armor");c.current().searchKey=70;c.current().friendVisuals=false;c.current().friendChinaHat=false;c.requestSave();c.flush();}
  try(var c=new ConfigService(root,new ModuleRegistry())){assertEquals("HUD",c.current().lastSection);assertTrue(c.current().expandedModules.contains("armor"));assertNull(c.current().expandedModule);assertTrue(c.current().favorites.contains("armor"));assertEquals(70,c.current().searchKey);assertFalse(c.current().friendVisuals);assertFalse(c.current().friendChinaHat);}
 }
 @Test void favoriteFriendsSortFirstAndPersist(){var f=new FriendService(root);f.add("Alpha");f.add("Zulu");var id=f.all().get(1).id();f.toggleFavorite(id);assertEquals("Zulu",f.all().getFirst().name());assertTrue(new FriendService(root).all().getFirst().favorite());f.toggleFavorite(id);assertEquals("Alpha",f.all().getFirst().name());}
 @Test void everySelectedProfileKeepsItsOwnChanges()throws Exception{try(var c=new ConfigService(root,new ModuleRegistry())){c.createProfile("first");c.current().friendVisuals=false;c.requestSave();c.selectProfile("second");c.current().friendVisuals=true;c.requestSave();c.selectProfile("first");assertFalse(c.current().friendVisuals);c.selectProfile("second");assertTrue(c.current().friendVisuals);}}
 @Test void friendVisualsDefaultOnAndIgnoreObsoleteNotes(){var cfg=new com.google.gson.Gson().fromJson("{\"friendNotes\":false}",dev.sqwat.vela.core.ClientConfig.class);assertTrue(cfg.friendVisuals);assertTrue(cfg.friendChinaHat);assertTrue(new dev.sqwat.vela.module.visual.ChinaHat().settings().stream().noneMatch(s->s.id().equals("friends")));}
 @Test void physicalPointerRemainsStableAcrossLiveScale(){var a=UiViewport.fit(960,540,2,1);var b=UiViewport.fit(960,540,2,1.2f);double pixel=420;double localNow=b.local(pixel);assertEquals(a.local(pixel),localNow*b.scale()/a.scale(),.001);assertTrue(b.scale()>a.scale());}
 @Test void iconsArePackaged(){for(String name:new String[]{"settings","microsoft","chevron"})for(int size:new int[]{16,20,24,32,48,64})assertNotNull(getClass().getResource("/assets/velaclient/textures/gui/icon_"+name+"_"+size+".png"));}
}
