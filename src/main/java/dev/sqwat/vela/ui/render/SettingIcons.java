package dev.sqwat.vela.ui.render;
import dev.sqwat.vela.module.setting.*;

/** Semantic icons shared by every module's settings. */
public final class SettingIcons {
    private SettingIcons(){}
    public static String of(Setting<?> setting){
        String id=setting.id();
        if(setting instanceof KeySetting)return "key";
        if(id.equals("health_hp"))return "settings";
        if(id.contains("health"))return "heart";
        if(id.equals("armor_percent"))return "settings";
        if(id.contains("warning"))return "visuals";
        if(id.contains("time")||id.contains("delay")||id.contains("interval"))return "cooldowns";
        if(id.equals("volume")&&setting.name().equals("Объём"))return "settings";
        if(id.contains("sound")||id.contains("volume"))return "speakers";
        if(id.contains("color"))return "visuals";
        if(id.contains("critical")||id.contains("attack"))return "attack";
        if(id.contains("armor")||id.contains("shield"))return "armor";
        if(id.contains("range")||id.contains("fov")||id.contains("target")||id.equals("priority"))return "target";
        if(id.contains("speed")||id.contains("sprint")||id.contains("movement"))return "movement";
        if(id.contains("player")||id.contains("friend"))return "friends";
        if(id.contains("mobs"))return "singleplayer";
        if(id.contains("look")||id.contains("visible")||setting instanceof ColorSetting)return "visuals";
        if(setting instanceof NumberSetting)return "settings";
        if(setting instanceof ChoiceSetting)return "contact_options";
        return "contact_options";
    }
}
