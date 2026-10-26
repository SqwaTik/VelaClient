package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Swaps the main hand while enabled (bind it as Toggle for one-key switching, or Hold for a temporary swap).
 * Uses the normal option and sends it to the server like the Options screen does; disabling restores the
 * hand that was set before.
 */
public final class HandSwap extends ClientModule {
    private HumanoidArm original;
    public HandSwap(){super("hand_swap","Hand Swap","Смена ведущей руки одним нажатием",ModuleCategory.UTILITIES);}
    @Override protected void onEnable(){
        var options=Minecraft.getInstance().options;
        original=options.mainHand().get();
        apply(original.getOpposite());
    }
    @Override protected void onDisable(){
        if(original==null)return;
        apply(original);original=null;
    }
    private static void apply(HumanoidArm arm){
        var mc=Minecraft.getInstance();
        mc.options.mainHand().set(arm);
        if(mc.getConnection()!=null)mc.options.broadcastOptions();
    }
}
