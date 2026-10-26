package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Locale;

/** Adds the hunger and saturation a food restores to its tooltip, right under the name. */
public final class FoodInfo extends ClientModule {
    public FoodInfo(){super("food_info","Food Info","Сытость и насыщение в подсказке еды",ModuleCategory.UTILITIES);}
    public static void initialize(){
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,context,flag,lines)->{
            if(dev.sqwat.vela.module.Modules.active(FoodInfo.class)!=null)decorate(stack,lines);
        });
    }
    static void decorate(ItemStack stack,List<Component> lines){
        var food=stack.get(DataComponents.FOOD);
        if(food==null||lines.isEmpty())return;
        lines.add(1,line(food.nutrition(),food.saturation()));
    }
    static MutableComponent line(int nutrition,float saturation){
        return Component.literal("Сытость ").withColor(0x8996B0).append(Component.literal("+"+nutrition).withColor(0xF0A65E))
            .append(Component.literal("  ·  ").withColor(0x5C6680))
            .append(Component.literal("Насыщение ").withColor(0x8996B0))
            .append(Component.literal("+"+format(saturation)).withColor(0xF4D47C));
    }
    static String format(float value){return String.format(Locale.ROOT,"%.1f",value).replaceAll("\\.0$","");}
}
