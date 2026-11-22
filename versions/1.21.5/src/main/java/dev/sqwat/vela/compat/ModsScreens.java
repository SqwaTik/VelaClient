package dev.sqwat.vela.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/** 1.21.5 bundles no Mod Menu: the Mods button opens the installed one, or stays on the parent screen without it. */
public final class ModsScreens {
    private ModsScreens(){}

    public static Screen create(Screen parent){
        if(!FabricLoader.getInstance().isModLoaded("modmenu"))return parent;
        try{
            return (Screen)Class.forName("com.terraformersmc.modmenu.api.ModMenuApi").getMethod("createModsScreen",Screen.class).invoke(null,parent);
        }catch(ReflectiveOperationException|LinkageError error){
            return parent;
        }
    }
}
