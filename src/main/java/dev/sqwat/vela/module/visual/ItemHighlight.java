package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.render.VelaTheme;
import java.util.LinkedHashMap;
import java.util.Map;

/** Constant-time item classification shared by inventory, hotbar and item previews. */
public final class ItemHighlight extends ClientModule {
    private final Map<String,BooleanSetting> groups=new LinkedHashMap<>();
    private final NumberSetting opacity=setting(new NumberSetting("opacity","Непрозрачность","Яркость фона предмета в процентах",25,5,70,5));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Единый цвет темы вместо цветов категорий",false));
    private final BooleanSetting ownColor=setting(new BooleanSetting("own_color","Свой цвет","Один любой цвет для всех категорий",false));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0x7AA2FF));
    {ownColor.visibleWhen(()->!theme.value());color.visibleWhen(()->!theme.value()&&ownColor.value());}
    private final ItemColorsSetting custom=setting(new ItemColorsSetting("custom_items","Свои предметы","Любые предметы со своим цветом"));
    {custom.visibleWhen(()->false);setting(new ActionSetting("edit_items","Свои предметы и цвета",()->net.minecraft.client.Minecraft.getInstance().gui.setScreen(
        new dev.sqwat.vela.ui.screen.ItemHighlightScreen(net.minecraft.client.Minecraft.getInstance().gui.screen(),this))));}
    public ItemColorsSetting custom(){return custom;}
    public ItemHighlight(){
        super("item_highlight","Item Highlight","Цветные подложки важных предметов в интерфейсе",ModuleCategory.UTILITIES);
        group("totems","Тотемы");group("pearls","Жемчуг и хорус");group("apples","Золотые яблоки");
        group("potions","Зелья");group("experience","Пузырьки опыта");group("crystals","Кристаллы и ТНТ");
    }
    private void group(String id,String name){groups.put(id,setting(new BooleanSetting(id,name,"Подсвечивать предметы категории",true)));}
    public static String category(String item){
        return switch(item){
            case "totem_of_undying"->"totems";
            case "ender_pearl","chorus_fruit"->"pearls";
            case "golden_apple","enchanted_golden_apple"->"apples";
            case "potion","splash_potion","lingering_potion"->"potions";
            case "experience_bottle"->"experience";
            case "end_crystal","tnt"->"crystals";
            default->null;
        };
    }
    /** {@code item}: the full id ("minecraft:diamond"). Items added in the editor win over the categories. */
    public int background(String item){
        if(!enabled())return 0;
        Integer own=custom.colors().get(item);
        if(own!=null)return ((int)Math.round(opacity.value()*2.55)<<24)|own;
        if(!item.startsWith("minecraft:"))return 0;
        item=item.substring("minecraft:".length());
        String group=category(item);if(group==null||!groups.get(group).value())return 0;
        int rgb=theme.value()?VelaTheme.PRIMARY:ownColor.value()?color.value():switch(group){
            case "totems","apples"->0xF3BE52;case "pearls"->0x62CEC0;case "potions"->0xB78AF3;
            case "experience"->0x8AD978;default->0xF77992;
        };
        return ((int)Math.round(opacity.value()*2.55)<<24)|(rgb&0xFFFFFF);
    }
}
