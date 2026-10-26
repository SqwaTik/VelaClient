package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.TextSetting;
import dev.sqwat.vela.render.entity.NameMask;
import net.minecraft.client.Minecraft;

public final class StreamerMode extends ClientModule {
    private final TextSetting alias=setting(new TextSetting("alias","Псевдоним","Ник, отображаемый вместо твоего имени","Player"));
    private NameMask mask;
    private String lastName="",lastAlias="";
    public StreamerMode(){super("streamer_mode","Streamer Mode","Скрывает свой ник в отображаемом тексте",ModuleCategory.UTILITIES);}
    public static NameMask mask(){
        var services=dev.sqwat.vela.VelaClient.services();
        if(services==null||!(services.modules().byId("streamer_mode") instanceof StreamerMode mode)||!mode.enabled())return null;
        var mc=Minecraft.getInstance();if(mc==null)return null;
        String name=mc.player==null?mc.getUser().getName():mc.player.getGameProfile().name();
        if(mode.mask==null||!name.equals(mode.lastName)||!mode.alias.value().equals(mode.lastAlias)){
            mode.lastName=name;mode.lastAlias=mode.alias.value();mode.mask=new NameMask(name,mode.lastAlias);
            dev.sqwat.vela.render.entity.NameTagTextCache.clear();
        }
        return mode.mask;
    }
    public static String display(String text){var mask=mask();return mask==null?text:mask.plain(text);}
    public static void refresh(){
        dev.sqwat.vela.render.entity.NameTagTextCache.clear();
        var mc=Minecraft.getInstance();if(mc!=null&&mc.gui!=null)mc.gui.hud.getChat().rescaleChat();
    }
    @Override protected void onEnable(){refresh();}
    @Override protected void onDisable(){mask=null;refresh();}
}
