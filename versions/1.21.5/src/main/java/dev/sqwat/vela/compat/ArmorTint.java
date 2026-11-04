package dev.sqwat.vela.compat;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** 1.21.5's EquipmentLayerRenderer gets no render state, so the armor layer leaves the one it is drawing here. */
public final class ArmorTint {
    private static HumanoidRenderState current;
    private ArmorTint(){}
    public static void begin(HumanoidRenderState state){current=state;}
    public static void end(){current=null;}
    public static HumanoidRenderState current(){return current;}
}
