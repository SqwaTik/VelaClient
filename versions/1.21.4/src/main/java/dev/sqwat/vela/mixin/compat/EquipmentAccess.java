package dev.sqwat.vela.mixin.compat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.Map;

/** 1.21.4's collectEquipmentChanges compares with the entity's last hand, armour and body items, so the fake player updates those itself. */
@Mixin(LivingEntity.class)
public interface EquipmentAccess {
    @Invoker("collectEquipmentChanges") Map<EquipmentSlot,ItemStack> vela$collectEquipmentChanges();
    @Invoker("setLastHandItem") void vela$setLastHandItem(EquipmentSlot slot,ItemStack stack);
    @Invoker("setLastArmorItem") void vela$setLastArmorItem(EquipmentSlot slot,ItemStack stack);
    @Accessor("lastBodyItemStack") void vela$setLastBodyItem(ItemStack stack);

}
