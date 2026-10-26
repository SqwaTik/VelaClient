package dev.sqwat.vela;
import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.module.*;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.NearPlayers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class UtilitiesTest {
    @TempDir Path root;
    @Test void utilitiesUseSharedRegistryAndStayDisabledByDefault(){
        try(var services=new VelaServices(root)){
            assertNull(services.modules().byId("hold_my_items"));
            for(String id:List.of("render_tweaks","auto_tpaccept","aspect_ratio","block_overlay","click_pearl","crystal_optimizer","item_physics")){
                var module=services.modules().byId(id);assertNotNull(module);
                var expected=id.equals("item_physics")?ModuleCategory.VISUALS:ModuleCategory.UTILITIES;
                assertEquals(expected,module.category());assertFalse(module.enabled());
            }
            for(var setting:services.modules().byId("render_tweaks").settings())assertEquals(false,setting.value());
        }
    }
    @Test void remainingMovementToolsAreUtilitiesAndNavigationMatchesSections(){
        try(var services=new VelaServices(root)){
            for(String id:List.of("autosprint")){
                var module=services.modules().byId(id);assertNotNull(module);assertEquals(ModuleCategory.UTILITIES,module.category());assertFalse(module.enabled());
            }
        }
        var sections=dev.sqwat.vela.ui.screen.VelaSection.values();
        assertEquals(sections.length,dev.sqwat.vela.ui.render.DashboardChrome.SECTIONS.length);
        for(int i=0;i<sections.length;i++)assertEquals(sections[i].title(),dev.sqwat.vela.ui.render.DashboardChrome.SECTIONS[i]);
        var layout=dev.sqwat.vela.ui.render.DashboardChrome.layout(854,480);
        for(int i=1;i<sections.length;i++)assertTrue(layout.navigation(i-1).y()+layout.navigation(i-1).height()<layout.navigation(i).y());
    }
    @Test void nearIncludes140MetresButNotBeyond(){
        assertEquals(List.of(23,139,140),NearPlayers.closest(List.of(141,140,139,23),5,i->true,i->i*i));
    }
    @Test void mixinsAreRegistered()throws Exception{
        String json=java.nio.file.Files.readString(Path.of("src/main/resources/velaclient.mixins.json"));
        assertFalse(json.contains("HoldMyItemsMixin"));
        for(String id:List.of("AspectRatioMixin","TeleportCombatMixin","BlockOverlayMixin","NameTagTextCacheMixin","FontCacheInvalidationMixin","MinecraftActions","RenderFireMixin","RenderParticlesMixin","DistanceFogMixin","ItemPhysicsMixin","ItemEntityRenderStateMixin"))assertTrue(json.contains("\""+id+"\""));
    }
    @Test void aspectPresetsAndColorValidation(){
        assertEquals(16f/9,AspectRatio.value("16:9"));assertEquals(4f/3,AspectRatio.value("4:3"));
        assertEquals(1f,AspectRatio.value("1:1"));assertEquals(21f/9,AspectRatio.value("21:9"));
        var color=new ColorSetting("rgb","RGB",0x123456);color.set(0xABCDEF);assertEquals(0xABCDEF,color.value());
        assertThrows(IllegalArgumentException.class,()->color.set(-1));
    }
}
