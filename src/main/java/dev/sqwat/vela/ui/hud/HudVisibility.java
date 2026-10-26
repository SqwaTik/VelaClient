package dev.sqwat.vela.ui.hud;
import net.minecraft.client.Minecraft;
public final class HudVisibility {
    private HudVisibility(){}
    public static boolean allowed(boolean overlay,boolean screen,boolean chat,boolean editor){return !overlay;}
    public static boolean allowed(boolean overlay,boolean screen,boolean chat,boolean editor,boolean backgroundReload){return !overlay||backgroundReload;}
    public static boolean allowed(Minecraft c){
        if(c.player==null||c.level==null)return false;
        var overlay=c.gui.overlay();
        boolean background=overlay instanceof dev.sqwat.vela.render.resource.InteractiveReload reload&&reload.vela$showsWorld();
        return allowed(overlay!=null,false,false,false,background);
    }
}
