package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.KeySetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Switches the main hand with its own key. Turning the module on changes nothing by itself; while it is on, the key
 * swaps left and right (the normal option, sent to the server like the Options screen does). Turning it off brings
 * back the hand that was set before.
 */
public final class HandSwap extends ClientModule {
    private final KeySetting key=setting(new KeySetting("swap_key","Клавиша смены руки",org.lwjgl.glfw.GLFW.GLFW_KEY_H));
    private HumanoidArm original;
    private boolean held;
    public HandSwap(){super("hand_swap","Hand Swap","Смена ведущей руки по своей клавише",ModuleCategory.UTILITIES);}
    // Config loading can enable modules before Minecraft has created its options; the hand is then remembered on the first tick.
    @Override protected void onEnable(){var options=Minecraft.getInstance().options;original=options==null?null:options.mainHand().get();held=true;}
    @Override protected void onDisable(){
        var options=Minecraft.getInstance().options;
        if(original!=null&&options!=null&&options.mainHand().get()!=original)apply(original);
        original=null;
    }
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.options==null)return;
        if(original==null)original=mc.options.mainHand().get();
        boolean down=mc.gui.screen()==null&&dev.sqwat.vela.ui.input.BindInput.down(mc.getWindow().handle(),key.value());
        if(down&&!held)apply(mc.options.mainHand().get().getOpposite());
        held=down;
    }
    private static void apply(HumanoidArm arm){
        var mc=Minecraft.getInstance();
        mc.options.mainHand().set(arm);
        if(mc.getConnection()!=null)mc.options.broadcastOptions();
    }
}
