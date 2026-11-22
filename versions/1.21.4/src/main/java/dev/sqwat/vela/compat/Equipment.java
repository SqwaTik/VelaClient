package dev.sqwat.vela.compat;

import dev.sqwat.vela.mixin.compat.EquipmentAccess;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** {@code lastEquipmentItems.put(slot, stack)} of later versions, through 1.21.4's last hand, armour and body items. */
public final class Equipment {
    private Equipment(){}
    public static void remember(EquipmentAccess entity,EquipmentSlot slot,ItemStack stack){
        switch(slot.getType()){
            case HAND->entity.vela$setLastHandItem(slot,stack);
            case HUMANOID_ARMOR->entity.vela$setLastArmorItem(slot,stack);
            default->entity.vela$setLastBodyItem(stack);
        }
    }
}
