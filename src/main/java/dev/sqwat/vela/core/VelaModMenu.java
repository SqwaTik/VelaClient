package dev.sqwat.vela.core;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.ui.screen.VelaDashboardScreen;

/** Mod Menu's "configure" button on Vela opens the Vela dashboard. */
public final class VelaModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory(){
        return parent->VelaClient.services()==null?null:new VelaDashboardScreen(parent,VelaClient.services());
    }
}
